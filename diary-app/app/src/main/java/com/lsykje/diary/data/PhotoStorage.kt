package com.lsykje.diary.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * 설계 문서 7-2절 규격: 긴 쪽 최대 2000px + JPEG 품질 85%로 리사이즈·압축해서
 * 앱 전용 내부 저장소(filesDir/photos)에 복사본만 저장한다. 원본 사진은 건드리지 않는다.
 */
object PhotoStorage {
    private const val MAX_EDGE = 2000
    private const val JPEG_QUALITY = 85

    fun photosDir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    /** content Uri(갤러리 사진 선택 결과)를 읽어 리사이즈·압축 후 내부 저장소에 저장하고 파일 경로를 돌려준다 */
    fun importAndResize(context: Context, sourceUri: Uri): String {
        val resolver = context.contentResolver

        val original = resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("이미지를 읽을 수 없습니다: $sourceUri")

        val rotated = applyExifRotation(context, sourceUri, original)
        val resized = downscale(rotated, MAX_EDGE)

        val outFile = File(photosDir(context), "${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (resized !== original) resized.recycle()
        if (rotated !== original) rotated.recycle()
        original.recycle()

        return outFile.absolutePath
    }

    fun delete(filePath: String) {
        File(filePath).takeIf { it.exists() }?.delete()
    }

    private fun downscale(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longEdge
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
