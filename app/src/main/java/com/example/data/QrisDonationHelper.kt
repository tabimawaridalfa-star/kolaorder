package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Zero-permission helper for saving and sharing the bundled voluntary QRIS donation image.
 * - Android 10+ (API 29+): writes directly to MediaStore Pictures/KalaOrder without storage permissions.
 * - Android 7-9 (API 24-28): writes to a SAF CreateDocument URI chosen by the user.
 * - Sharing: exports via FileProvider from app cache directory with read URI permission.
 */
object QrisDonationHelper {

    const val QRIS_FILE_NAME = "QRIS_KalaOrder_Donasi.jpg"

    private fun readQrisJpegBytes(context: Context): ByteArray {
        val rawAttempt = runCatching {
            context.resources.openRawResource(R.drawable.qris_donasi_kalaorder).use { input ->
                input.readBytes()
            }
        }.getOrNull()

        if (rawAttempt != null && rawAttempt.isNotEmpty()) {
            return rawAttempt
        }

        // Fallback via BitmapFactory if resource stream was compressed/transformed
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.qris_donasi_kalaorder)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        return out.toByteArray()
    }

    fun canSaveDirectlyToMediaStore(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun saveQrisToGalleryMediaStore(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val bytes = readQrisJpegBytes(context)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "QRIS_KalaOrder_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KalaOrder")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: return false

            val completedValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completedValues, null, null)
            true
        }.getOrDefault(false)
    }

    fun writeQrisToUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            val bytes = readQrisJpegBytes(context)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } != null
        }.getOrDefault(false)
    }

    fun shareQrisImage(context: Context): Boolean {
        return runCatching {
            val bytes = readQrisJpegBytes(context)
            val sharedDir = File(context.cacheDir, "shared_qris").apply { mkdirs() }
            val imageFile = File(sharedDir, QRIS_FILE_NAME)
            imageFile.writeBytes(bytes)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile,
            )

            val caption = "Traktir Kopi Pengembang KalaOrder (${CreatorInfo.CREATOR_NAME}) ☕\n" +
                "QRIS: ${CreatorInfo.QRIS_MERCHANT_NAME} • NMID: ${CreatorInfo.QRIS_NMID}\n" +
                "Scan atau unggah dari galeri lewat DANA, GoPay, OVO, ShopeePay, atau m-Banking."

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Bagikan QRIS Traktir Kopi").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
