package com.example.myapplication.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.myapplication.ui.explore.FullScreenImageActivity
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

object ImageOpenHelper {
    // Umbral para considerar Base64 "grande" y escribir a archivo (puedes ajustar)
    private const val BASE64_THRESHOLD = 10000

    fun openFullScreen(context: Context, images: List<String>, startIndex: Int = 0) {
        try {
            val resolved = ArrayList<String>()
            val uriItems = ArrayList<Uri>()

            for ((i, img) in images.withIndex()) {
                if (img.startsWith("http://") || img.startsWith("https://") || img.startsWith("content://") || img.startsWith("file://")) {
                    resolved.add(img)
                    if (img.startsWith("content://") || img.startsWith("file://")) uriItems.add(Uri.parse(img))
                } else if (img.startsWith("data:")) {
                    // data:[<mediatype>][;base64],<data>
                    val comma = img.indexOf(',')
                    val b64 = if (comma >= 0 && comma + 1 < img.length) img.substring(comma + 1) else img
                    if (b64.length > BASE64_THRESHOLD) {
                        val uri = writeBase64ToCacheAndGetUri(context, b64, i)
                        if (uri != null) {
                            resolved.add(uri.toString())
                            uriItems.add(uri)
                        } else resolved.add(img)
                    } else {
                        // small base64, pass as is
                        resolved.add(img)
                    }
                } else {
                    // plain string: maybe raw base64 or short id
                    if (img.length > BASE64_THRESHOLD) {
                        val uri = writeBase64ToCacheAndGetUri(context, img, i)
                        if (uri != null) {
                            resolved.add(uri.toString())
                            uriItems.add(uri)
                        } else resolved.add(img)
                    } else {
                        resolved.add(img)
                    }
                }
            }

            val intent = Intent(context, FullScreenImageActivity::class.java)
            intent.putStringArrayListExtra("imageUris", resolved)
            intent.putExtra("startIndex", startIndex)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Permitir que la actividad receptora lea URIs que provienen del FileProvider
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Si hay URIs de FileProvider, agregar ClipData y conceder permisos
            if (uriItems.isNotEmpty()) {
                val first = uriItems[0]
                var clip = ClipData.newUri(context.contentResolver, "image", first)
                for (i in 1 until uriItems.size) {
                    clip.addItem(ClipData.Item(uriItems[i]))
                }
                intent.clipData = clip
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeBase64ToCacheAndGetUri(context: Context, base64Str: String, index: Int): Uri? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val cacheDir = File(context.cacheDir, "image_open_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val fileName = "img_${System.currentTimeMillis()}_${index}.jpg"
            val f = File(cacheDir, fileName)
            val fos = FileOutputStream(f)
            fos.write(bytes)
            fos.flush()
            fos.close()
            // Obtener uri con FileProvider
            val authority = context.packageName + ".fileprovider"
            FileProvider.getUriForFile(context, authority, f)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
