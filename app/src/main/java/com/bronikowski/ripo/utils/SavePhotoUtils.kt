package com.bronikowski.ripo.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import org.opencv.android.Utils
import org.opencv.core.CvException
import org.opencv.core.Mat
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream


object SavePhotoUtils {

    private val folderName = "RiPO_Objects"

    fun saveImage(mat: Mat, context: Context) {
        var bmp: Bitmap? = null

        try {
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bmp)

            val directory = File(
                Environment.getExternalStorageDirectory().toString() + separator + folderName
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".jpeg"
            val file = File(directory, fileName)
            saveImageToStream(bmp, FileOutputStream(file))
            if (file.absolutePath != null) {
                val values = contentValues()
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        } catch (e: CvException) {
            e.printStackTrace()
        }
    }

    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}