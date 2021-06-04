package com.bronikowski.ripo.utils

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.bronikowski.ripo.MainActivity
import org.opencv.core.Mat
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object SaveFileUtils {
    private var file: File? = null;
    private val TAG = MainActivity::class.java.simpleName
    fun saveFile(@RawRes rawId: Int, name: String, saveName: String, context: Context) : File? {
        try {
            val rawRes = context.resources.openRawResource(rawId)
            val cascadeDir = context.getDir(name, AppCompatActivity.MODE_PRIVATE)
            file = File(cascadeDir, name)
            val os: FileOutputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (rawRes.read(buffer).also { bytesRead = it } != -1) {
                os.write(buffer, 0, bytesRead)
            }
            rawRes.close()
            os.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
        }
        return file;
    }
}