package com.moneydropper.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object SignatureHelper {
    fun saveSignatureBitmap(context: Context, bitmap: Bitmap, fileName: String): String {
        val directory = File(context.filesDir, "signatures")
        if (!directory.exists()) directory.mkdirs()
        
        val file = File(directory, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }
}
