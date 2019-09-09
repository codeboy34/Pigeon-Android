package com.pigeonmessenger.extension

import android.app.Activity
import android.content.*
import android.database.Cursor
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.annotation.Nullable
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.pigeonmessenger.BuildConfig
import com.pigeonmessenger.R
import com.pigeonmessenger.utils.Attachment
import com.pigeonmessenger.utils.video.MediaController
import com.pigeonmessenger.utils.video.VideoEditedInfo
import com.pigeonmessenger.webrtc.PeerConnectionClient
import com.pigeonmessenger.widget.PbDialog
import com.pigeonmessenger.widget.gallery.MimeType
import com.pigeonmessenger.widget.gallery.engine.impl.GlideEngine
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.toast
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import timber.log.Timber
import java.io.File

const val REQUEST_IMAGE = 0x01
const val REQUEST_GALLERY = 0x02
const val REQUEST_CAMERA = 0x03
const val REQUEST_FILE = 0x04
const val REQUEST_AUDIO = 0x05

private val maxVideoSize by lazy {
    480f
}


fun Activity.openImage(output: Uri) {
    val cameraIntents = ArrayList<Intent>()
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val packageManager = this.packageManager
    val listCam = packageManager.queryIntentActivities(captureIntent, 0)
    for (res in listCam) {
        val packageName = res.activityInfo.packageName
        val intent = Intent(captureIntent)
        intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
        intent.`package` = packageName
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output)
        cameraIntents.add(intent)
    }

    val galleryIntent = Intent()
    galleryIntent.type = "image/*"
    galleryIntent.action = Intent.ACTION_PICK

    val chooserIntent = Intent.createChooser(galleryIntent, "Select Picture")
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
    try {
        this.startActivityForResult(chooserIntent, REQUEST_IMAGE)
    } catch (e: ActivityNotFoundException) {
    }
}



fun Activity.openImage() {
    val cameraIntents = ArrayList<Intent>()
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val packageManager = this.packageManager
    val listCam = packageManager.queryIntentActivities(captureIntent, 0)

    val galleryIntent = Intent()
    galleryIntent.type = "image/*"
    galleryIntent.action = Intent.ACTION_PICK

    val chooserIntent = Intent.createChooser(galleryIntent, "Select Picture")

    try {
        this.startActivityForResult(chooserIntent, REQUEST_IMAGE)
    } catch (e: ActivityNotFoundException) {
    }
}


@Suppress("DEPRECATION")
fun Context.vibrate(pattern: LongArray) {
    if (Build.VERSION.SDK_INT >= 26) {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(pattern, -1)
    }
}
private val uiHandler = Handler(Looper.getMainLooper())

fun Context.mainThread(runnable: () -> Unit) {
    uiHandler.post(runnable)
}

inline fun supportsNougat(code: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        code()
    }
}

fun Context.openPermissionSetting() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
    toast(R.string.error_permission)
}

fun Activity.selectDocument() {
    selectMediaType("*/*", arrayOf("*/*"), REQUEST_FILE)
}


fun Activity.selectMediaType(type: String, extraMimeType: Array<String>?, requestCode: Int) {
    val intent = Intent()
    intent.type = type
    intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeType)
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    try {
        startActivityForResult(intent, requestCode)
        return
    } catch (e: ActivityNotFoundException) {
    }

    intent.action = Intent.ACTION_GET_CONTENT
    try {
        startActivityForResult(intent, requestCode)
    } catch (e: ActivityNotFoundException) {
    }
}


fun FragmentActivity.addFragment(from: Fragment, to: Fragment, tag: String) {
    val fm = supportFragmentManager
    fm?.let {
        val ft = it.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
        if (to.isAdded) {
            ft.show(to)
        } else {
            ft.add(R.id.container, to, tag)
        }
        ft.addToBackStack(null)
        ft.commitAllowingStateLoss()
    }
}

fun Activity.openCamera(output: Uri) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output)
    } else {
        val file = File(output.path)
        val photoUri = FileProvider.getUriForFile(this.applicationContext,
                BuildConfig.APPLICATION_ID + ".provider", file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (intent.resolveActivity(this.packageManager) != null) {
        startActivityForResult(intent, REQUEST_CAMERA)
    } else {
        toast(R.string.error_no_camera)
    }
}


fun Activity.openGallery() {
    com.pigeonmessenger.widget.gallery.Gallery.from(this)
            .choose(MimeType.ofMedia())
            .imageEngine(GlideEngine())
            .forResult(REQUEST_GALLERY)
}

fun Context.displaySize(): Point {
    val displaySize = Point()
    val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = manager.defaultDisplay
    display.getSize(displaySize)
    return displaySize
}
fun Context.displayWitdh(): Int {
    return displaySize().x
}

fun Context.displayHeight(): Int {
    return displaySize().y
}


fun Context.getUriForFile(file: File): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val authority = String.format("%s.provider", this.packageName)
        FileProvider.getUriForFile(this, authority, file)
    } else {
        Uri.fromFile(file)
    }
}

inline fun <T : Any, R> notNullElse(input: T?, normalAction: (T) -> R, default: R): R {
    return if (input == null) {
        default
    } else {
        input.let(normalAction)
    }
}

fun Context.dpToPx(dp: Float): Int {
    return if (dp == 0f) {
        0
    } else {
        Math.ceil((this.resources.displayMetrics.density * dp).toDouble()).toInt()
    }
}


inline fun <T : Any> notNullElse(input: T?, normalAction: (T) -> Unit, elseAction: () -> Unit) {
    return if (input != null) {
        input.let(normalAction)
    } else {
        elseAction()
    }
}

fun Activity.hideKeyboard() {
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = currentFocus
    if (view != null) {
        inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}
fun Context.getVideoModel(uri: Uri): VideoEditedInfo? {
    try {
        val path = uri.getFilePath() ?: return null
        val m = MediaMetadataRetriever().apply {
            setDataSource(path)
        }
        val fileName = File(path).name
        val rotation = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val image = m.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
        val mediaWith = image.width
        val mediaHeight = image.height
        val duration = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        val thumbnail = image.zoomOut()?.fastBlur(1f, 10)?.bitmap2String()
        val scale = if (mediaWith > mediaHeight) maxVideoSize / mediaWith else maxVideoSize / mediaHeight
        val resultWidth = (Math.round((mediaWith * scale / 2).toDouble()) * 2).toInt()
        val resultHeight = (Math.round((mediaHeight * scale / 2).toDouble()) * 2).toInt()
        return if (scale < 1) {
            val bitrate = MediaController.getBitrate(path, scale)
            VideoEditedInfo(path, duration, rotation, mediaWith, mediaHeight, resultWidth, resultHeight, thumbnail,
                    fileName, bitrate)
        } else {
            VideoEditedInfo(path, duration, rotation, mediaWith, mediaHeight, mediaWith, mediaHeight, thumbnail,
                    fileName, 0, false)
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
    return null
}

fun Context.getAttachment(uri: Uri): Attachment? {
    var cursor: Cursor? = null

    try {
        cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
            val mimeType = contentResolver.getType(uri)

            return Attachment(uri, fileName, mimeType, fileSize)
        }
    } finally {
        if (cursor != null) cursor.close()
    }
    return null
}


fun Context.networkConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network: NetworkInfo
    try {
        network = cm.activeNetworkInfo
    } catch (t: Throwable) {
        return false
    }
    return network != null && network.isConnected
}
fun Context.isMobileNetworkConnected(){
   val networkInfo =  connectivityManager.activeNetworkInfo
           ////if (networkInfo!=null && networkInfo.isConnected && networkInfo.type)
}

inline fun belowOreo(code: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        code()
    }
}

fun Context.displayRatio(): Float {
    val size = displaySize()
    return size.y.toFloat() / size.x
}

fun Context.statusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        return resources.getDimensionPixelSize(resourceId)
    }
    return dpToPx(24f)
}

fun Context.isTablet(): Boolean = resources.getBoolean(R.bool.isTablet)

inline fun <T : Number, R> notEmptyOrElse(input: T?, normalAction: (T) -> R, elseAction: () -> R): R {
    return if (input != null && input != 0) {
        normalAction(input)
    } else {
        elseAction()
    }
}

fun Context.getClipboardManager(): ClipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


private var maxItemWidth: Int? = null

fun Context.maxItemWidth(): Int {
    if (maxItemWidth == null) {
        maxItemWidth = displaySize().x - dpToPx(66f)
    }
    return maxItemWidth!!
}

fun Context.runOnUIThread(runnable: Runnable, delay: Long = 0L) {
    if (delay == 0L) {
        uiHandler.post(runnable)
    } else {
        uiHandler.postDelayed(runnable, delay)
    }
}


inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
    val fragmentTransaction = beginTransaction()
    fragmentTransaction.func()
    fragmentTransaction.commitAllowingStateLoss()
}

fun FragmentActivity.replaceFragment(fragment: Fragment, frameId: Int, tag: String) {
    supportFragmentManager.inTransaction { replace(frameId, fragment, tag) }
}

inline fun supportsOreo(code: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        code()
    }
}


@Nullable
 fun Context.createVideoCapturer(): CameraVideoCapturer? {
    var camera2EnumeratorIsSupported = false
    try {
        camera2EnumeratorIsSupported = Camera2Enumerator.isSupported(this)
    } catch (throwable: Throwable) {
        Log.w(PeerConnectionClient.TAG, "Camera2Enumator.isSupport() threw.", throwable)
    }

    Log.w(PeerConnectionClient.TAG, "Camera2 enumerator supported: $camera2EnumeratorIsSupported")
    val enumerator: CameraEnumerator

    if (camera2EnumeratorIsSupported)
        enumerator = Camera2Enumerator(this)
    else
        enumerator = Camera1Enumerator(true)

    val deviceNames = enumerator.deviceNames

    for (deviceName in deviceNames) {
        if (enumerator.isFrontFacing(deviceName)) {
            Log.w(PeerConnectionClient.TAG, "Creating front facing camera capturer.")
            val videoCapturer = enumerator.createCapturer(deviceName, null)

            if (videoCapturer != null) {
                Log.w(PeerConnectionClient.TAG, "Found front facing capturer: $deviceName")

                return videoCapturer
            }
        }
    }

    for (deviceName in deviceNames) {
        if (!enumerator.isFrontFacing(deviceName)) {
            Log.w(PeerConnectionClient.TAG, "Creating other camera capturer.")
            val videoCapturer = enumerator.createCapturer(deviceName, null)

            if (videoCapturer != null) {
                Log.w(PeerConnectionClient.TAG, "Found other facing capturer: $deviceName")
                return videoCapturer
            }
        }
    }

    Log.w(PeerConnectionClient.TAG, "Video capture not supported!")
    return null
}


fun Context.progressDialog(): PbDialog {
    val pbDialog = PbDialog()
    pbDialog.isCancelable= false
    return  pbDialog
}



inline fun <T : Fragment> T.withArgs(argsBuilder: Bundle.() -> Unit): T =
        this.apply { arguments = Bundle().apply(argsBuilder) }

