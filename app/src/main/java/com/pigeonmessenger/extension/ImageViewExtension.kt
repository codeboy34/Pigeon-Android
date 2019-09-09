package com.pigeonmessenger.extension

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.pigeonmessenger.R
import com.pigeonmessenger.utils.StringSignature
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.io.File

fun ImageView.loadImage(uri: String?) {
    Glide.with(this).load(uri).into(this)
}

fun ImageView.loadImage(uri: Uri?) {
    Glide.with(this).load(uri).into(this)
}


fun ImageView.loadImage(uri: Uri?, @DrawableRes holder: Int, sign: String) {
    Glide.with(this).load(uri).apply(RequestOptions.placeholderOf(holder).centerCrop().signature(StringSignature(sign))).into(this)
}

fun ImageView.loadImage(uri: String?, @DrawableRes holder: Int, sign: String) {
    Glide.with(this).load(uri).apply(RequestOptions.placeholderOf(holder).signature(StringSignature(sign))).into(this)
}

fun ImageView.loadImage(uri: String?, @DrawableRes holder: Int) {
    Glide.with(this).load(uri).apply(RequestOptions.placeholderOf(holder)).into(this)
}

fun ImageView.loadImage(uri: String?, requestListener: RequestListener<Drawable?>) {
    Glide.with(this).load(uri).listener(requestListener).into(this)
}

fun ImageView.loadImage(uri: String?, width: Int, height: Int) {
    val multi = MultiTransformation(CropTransformation(width, height))
    Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(multi).dontAnimate()).into(this)
}


/* ---------------------------------avatar extensions start here ----------------------------------------------*/

fun ImageView.loadAvatar(file: File, thumbnail: String?, placeHolder: Int) {
    Glide.with(this).load(file).apply(RequestOptions().dontAnimate().circleCrop().apply {
        if (thumbnail != null) {
            this.error(thumbnail.toDrawable())
        } else {
            this.error(placeHolder)
        }
        val sign = if (file.exists()) file.length().toString() else if (thumbnail != null) thumbnail.length.toString() else "0"
        signature(StringSignature(sign))
    }).into(this)
}

fun ImageView.loadAvatarFile(file: File, thumbnail: String?, placeHolder: Int) {
    Glide.with(this).load(file).apply(RequestOptions().dontAnimate().circleCrop().apply {
        if (thumbnail != null) {
            this.error(thumbnail.toDrawable())
        } else {
            this.error(placeHolder)
        }
        val sign = if (file.exists()) file.length().toString() else if (thumbnail != null) thumbnail.length.toString() else "0"
        signature(StringSignature(sign))
    }).into(this)
}

fun ImageView.loadAvatarThumbnail(thumbnail: String, placeHolder: Int) {
    Glide.with(this).load(thumbnail.toDrawable()).apply(RequestOptions().dontAnimate().circleCrop().apply {
        this.error(placeHolder)
        signature(StringSignature(thumbnail.length.toString()))
    }).into(this)
}

fun ImageView.loadAvatarPlaceHolder(placeHolder: Int) {
    Glide.with(this).load(placeHolder).apply(RequestOptions().dontAnimate().circleCrop()).into(this)
}

fun ImageView.loadOwnerAvatar(icon: String?, thumbnail: String?) {
    when {
        icon != null -> {
            Glide.with(this).load(icon.toDrawable()).apply(RequestOptions().dontAnimate().circleCrop().apply {
                error(R.drawable.avatar_contact)
            }).into(this)
        }
        thumbnail != null -> {
            Glide.with(this).load(thumbnail.toDrawable()).apply(RequestOptions().dontAnimate().circleCrop().apply {
                error(R.drawable.avatar_contact)
            }).into(this)
        }
        else -> {
            Glide.with(this).load(R.drawable.avatar_contact).apply(RequestOptions().dontAnimate().circleCrop()).into(this)
        }
    }
}


fun ImageView.loadBaseAvatar(avatar: String, thumbnail: String?) {
    Glide.with(this).load(avatar.toDrawable()).apply(RequestOptions().dontAnimate()
            .apply {
                if (thumbnail != null) {
                    this.placeholder(thumbnail.toDrawable())
                }
            }).into(this)
}

/*--------------------------------avatar extensions start here ----------------------------------*/

fun ImageView.loadImageCenterCrop(uri: String?, @DrawableRes holder: Int? = null) {
    Glide.with(this).load(uri).apply(RequestOptions().dontAnimate().dontTransform().centerCrop().apply {
        if (holder != null) {
            this.placeholder(holder)
        }
    }).into(this)
}

fun ImageView.loadGif(uri: String?, requestListener: RequestListener<GifDrawable?>? = null, centerCrop: Boolean? = null, @DrawableRes holder: Int? = null) {
    var requestOptions = RequestOptions().dontTransform()
    if (centerCrop != null) {
        requestOptions = requestOptions.centerCrop()
    }
    if (holder != null) {
        requestOptions = requestOptions.placeholder(holder)
    }
    if (requestListener != null) {
        Glide.with(this).asGif().load(uri).apply(requestOptions).listener(requestListener).into(this)
    } else {
        Glide.with(this).asGif().load(uri).apply(requestOptions).into(this)
    }
}

fun ImageView.loadGifMark(uri: String?, holder: String?, mark: Int) {
    Glide.with(this).asGif().load(uri).apply(RequestOptions().dontTransform()
            //.signature(StringSignature("$uri$mark"))
            .apply {
                if (holder != null) {
                    this.placeholder(holder.toDrawable())
                }
            }).into(this)
}


fun ImageView.loadGifMark(uri: String?, mark: Int) {
    Glide.with(this).asGif().load(uri).apply(RequestOptions().dontTransform()
            //     .signature(StringSignature("$uri$mark"))
    )
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                    return true
                }

                override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    setImageDrawable(resource)
                    return true
                }
            })
            .submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadThumbnailDrawable(thumbnail: String?, @DrawableRes holder: Int) {
    Glide.with(this).load(thumbnail?.toDrawable()).apply(RequestOptions().dontAnimate().centerCrop()
            .apply { this.placeholder(holder) }).into(this)
}

fun ImageView.loadIcon(file: File, thumbnail: String?) {
    Glide.with(this).load(file).apply(RequestOptions()
            .placeholder(thumbnail?.toDrawable())
            .error(thumbnail?.toDrawable())
    ).into(this)
}

fun ImageView.loadPlaceHolder(@DrawableRes placeHolder: Int) {
    Glide.with(this).load(placeHolder).into(this)
}


fun ImageView.loadImageMark(uri: String?, holder: String?, mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions().dontAnimate()
            .apply {
                if (holder != null) {
                    val drawable = holder.toDrawable()
                    if (drawable != null)
                        Log.d("ImageViewExtension", "not null drawable: ");
                    this.placeholder(holder.toDrawable())
                } else Log.d("ImageViewEx", "drawable null: ");
            }).into(this)

}

fun ImageView.loadImageMark(uri: String?, mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions().dontAnimate())
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    Log.d("ImageViewExten", "onLoadFailed ");
                    return true
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    setImageDrawable(resource)
                    return true
                }
            })
            .submit(layoutParams.width, layoutParams.height)
}


fun ImageView.loadLongImageMark(uri: String?, holder: String?, mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(CropTransformation(0, layoutParams.height, CropTransformation.CropType.TOP))
            .dontAnimate()
            .apply {
                if (holder != null) {
                    this.placeholder(holder.toDrawable())
                }
            }).into(this)

}


fun ImageView.loadLongImageMark(uri: String?, mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(CropTransformation(0, layoutParams.height, CropTransformation.CropType.TOP))
            .dontAnimate()).listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            return true
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            setImageDrawable(resource)
            return true
        }
    }).submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadVideoMark(uri: String?, holder: String?, mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions().frame(0)
            // .signature(StringSignature("$uri$mark"))
            .centerCrop().dontAnimate().apply {
                if (holder != null) {
                    this.placeholder(holder.toDrawable())
                }
            }
    ).into(this)
}

fun ImageView.loadVideoMark(uri: String?, mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions().frame(0)
            /// .signature(StringSignature("$uri$mark"))
            .centerCrop().dontAnimate()).listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            return true
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            setImageDrawable(resource)
            return true
        }
    }).submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadVideo(uri: String, @DrawableRes holder: Int) {
    Glide.with(this).load(uri).apply(RequestOptions().frame(0)
            .centerCrop().placeholder(holder).dontAnimate()).into(this)
}

fun ImageView.loadSticker(uri: String?, type: String?) {
    uri?.let {
        when (type) {
            "GIF" -> {
                loadGif(uri)
            }
            else -> loadImage(uri)
        }
    }
}

fun ImageView.loadBase64(uri: ByteArray?, width: Int, height: Int, mark: Int) {
    val multi = MultiTransformation(CropTransformation(width, height))
    Glide.with(this).load(uri)
            .apply(RequestOptions().centerCrop()
                    .transform(multi)
                    //.signature(StringSignature("$uri$mark"))
                    .dontAnimate()).into(this)
}

fun ImageView.loadCircleImage(uri: String?, @DrawableRes holder: Int? = null) {
    if (uri.isNullOrBlank()) {
        if (holder != null) {
            setImageResource(holder)
        }
    } else if (holder == null) {
        Glide.with(this).load(uri).apply(RequestOptions().circleCrop()).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().placeholder(holder).circleCrop()).into(this)
    }
}

fun ImageView.loadRoundImage(uri: String?, radius: Int, @DrawableRes holder: Int? = null) {
    if (uri.isNullOrBlank() && holder != null) {
        setImageResource(holder)
    } else if (holder == null) {
        Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(radius, 0))).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().transform(RoundedCornersTransformation(radius, 0))
                .placeholder(holder))
                .into(this)
    }
}
