package com.federicopuy.objectdetectionapp.objectdetection

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun assetFilePath(context: Context, assetName: String): String {
    val file = File(context.filesDir, assetName)
    if (file.exists() && file.length() > 0) {
        return file.absolutePath
    }
    runCatching {
        val inputStream = context.assets.open(assetName)
        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(4 * 1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        outputStream.flush()
    }.onFailure {
        throw RuntimeException(it)
    }
    return file.absolutePath
}

fun readStringsFromTxtAsset(context: Context, assetName: String): List<String> {
    val inputStream = context.assets.open(assetName)
    val size = inputStream.available()
    val buffer = ByteArray(size)
    inputStream.read(buffer)
    inputStream.close()
    return String(buffer).split("\n")
}

