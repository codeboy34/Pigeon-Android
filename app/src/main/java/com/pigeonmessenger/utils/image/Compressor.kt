package com.pigeonmessenger.utils.image

import android.graphics.Bitmap
import android.util.Log
import io.reactivex.Flowable
import java.io.File
import java.io.IOException

class Compressor {
    private var maxWidth = 1080
    private var maxHeight = 1080
    private var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    private var quality = 80

    fun setMaxWidth(maxWidth: Int): Compressor {
        this.maxWidth = maxWidth
        return this
    }

    fun setMaxHeight(maxHeight: Int): Compressor {
        this.maxHeight = maxHeight
        return this
    }

    fun setCompressFormat(compressFormat: Bitmap.CompressFormat): Compressor {
        this.compressFormat = compressFormat
        return this
    }

    fun setQuality(quality: Int): Compressor {
        this.quality = quality
        return this
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun compressToFile(imageFile: File, compressedFilePath: String = imageFile.name): File {
        return ImageUtil.compressImage(imageFile, maxWidth, maxHeight, compressFormat, quality,
            compressedFilePath)
    }

    @Throws(IOException::class)
    fun compressToBitmap(imageFile: File): Bitmap {
        return ImageUtil.decodeSampledBitmapFromFile(imageFile, maxWidth, maxHeight)
    }

    fun compressToFileAsFlowable(imageFile: File): Flowable<File> {
        return compressToFileAsFlowable(imageFile, imageFile.name)
    }

    fun compressToFileAsFlowable(imageFile: File, compressedFilePath: String): Flowable<File> {
        return Flowable.defer {
            try {
                Flowable.just<File>(compressToFile(imageFile, compressedFilePath))
            } catch (e: IOException) {
                Log.d("Compresser","Err",e);
                Flowable.error<File>(e)
            }
        }
    }

    fun compressToBitmapAsFlowable(imageFile: File): Flowable<Bitmap> {
        return Flowable.defer {
            try {
                Flowable.just<Bitmap>(compressToBitmap(imageFile))
            } catch (e: IOException) {
                Flowable.error<Bitmap>(e)
            }
        }
    }
}