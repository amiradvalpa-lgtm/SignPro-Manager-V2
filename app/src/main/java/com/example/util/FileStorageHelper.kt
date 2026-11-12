package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectSpecEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

data class SpecAttachmentItem(
    val file: File,
    val uriString: String,
    val projectId: String?,
    val projectTitle: String?,
    val customerName: String?,
    val signType: String?,
    val isAttached: Boolean,
    val fileSizeFormatted: String
)

object FileStorageHelper {

    private const val ATTACHMENTS_DIR = "spec_attachments"

    /**
     * Directory inside app-specific internal storage for persisting project spec attachments.
     */
    fun getAttachmentsDir(context: Context): File {
        return File(context.filesDir, ATTACHMENTS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Sanitizes a string for inclusion in a filename, supporting Persian/Unicode characters.
     */
    private fun sanitizeForFilename(raw: String): String {
        return raw.trim()
            .replace("[/\\\\:*?\"<>|\\s]+".toRegex(), "_")
            .take(30)
    }

    /**
     * Encodes metadata into the filename:
     * ATTACH_p[projectId]_[projectTitle]_c[customerName]_[timestamp]_[uuid].jpg
     */
    fun createInternalCameraFile(
        context: Context,
        projectId: String,
        projectTitle: String,
        customerName: String
    ): File {
        val dir = getAttachmentsDir(context)
        val safeProj = sanitizeForFilename(projectTitle.ifBlank { "پروژه" })
        val safeCust = sanitizeForFilename(customerName.ifBlank { "کارفرما" })
        val safeId = sanitizeForFilename(projectId)
        val filename = "ATTACH_p${safeId}_${safeProj}_c${safeCust}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        return File(dir, filename)
    }

    /**
     * Copies an image stream from an external/gallery URI into app internal storage.
     * Returns the file URI as Uri or null on failure.
     */
    fun saveImageToInternalStorage(
        context: Context,
        sourceUri: Uri,
        projectId: String,
        projectTitle: String,
        customerName: String
    ): Uri? {
        return try {
            val targetFile = createInternalCameraFile(context, projectId, projectTitle, customerName)
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            FileOutputStream(targetFile).use { outStream ->
                inputStream.copyTo(outStream)
            }
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses metadata from file name if created with ATTACH_ convention.
     * e.g., ATTACH_pp1_تابلو_سامسونگ_cعلی_رضایی_179328282828_a1b2c3.jpg
     */
    fun parseMetadataFromFilename(filename: String): Triple<String?, String?, String?> {
        return try {
            if (!filename.startsWith("ATTACH_")) return Triple(null, null, null)
            val withoutExt = filename.substringBeforeLast(".")
            val parts = withoutExt.removePrefix("ATTACH_").split("_")
            var projectId: String? = null
            var projectTitle: String? = null
            var customerName: String? = null

            // Find parts starting with p and c
            val pIndex = parts.indexOfFirst { it.startsWith("p") }
            val cIndex = parts.indexOfFirst { it.startsWith("c") }

            if (pIndex != -1) {
                projectId = parts[pIndex].removePrefix("p")
            }
            if (pIndex != -1 && cIndex != -1 && cIndex > pIndex + 1) {
                projectTitle = parts.subList(pIndex + 1, cIndex).joinToString(" ")
            }
            if (cIndex != -1 && cIndex + 1 < parts.size) {
                // Next elements before timestamp
                val remaining = parts.subList(cIndex, parts.size - 2)
                customerName = remaining.joinToString(" ").removePrefix("c")
            }

            Triple(projectId, projectTitle?.replace("_", " "), customerName?.replace("_", " "))
        } catch (e: Exception) {
            Triple(null, null, null)
        }
    }

    /**
     * Formats bytes to human-readable string.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * Scans internal attachments directory and cross-references with Room database.
     */
    fun scanAttachments(
        context: Context,
        allSpecs: List<ProjectSpecEntity>,
        allProjects: List<ProjectEntity>,
        allCustomers: List<CustomerEntity>
    ): Pair<List<SpecAttachmentItem>, List<SpecAttachmentItem>> {
        val dir = getAttachmentsDir(context)
        val files = dir.listFiles()?.filter { it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".png") || it.name.endsWith(".jpeg")) }
            ?: emptyList()

        // Also check legacy cache dir if exists
        val cachePhotos = File(context.cacheDir, "camera_photos").listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".png")) } ?: emptyList()

        val allDiskFiles = (files + cachePhotos).distinctBy { it.absolutePath }

        // Map URIs from database specs
        // Each spec can have multiple comma-separated URIs
        val specByUriKey = mutableMapOf<String, ProjectSpecEntity>()
        for (spec in allSpecs) {
            val uris = spec.attachmentUris.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (u in uris) {
                specByUriKey[u] = spec
                // Also index by file path or filename
                try {
                    val parsed = Uri.parse(u)
                    parsed.lastPathSegment?.let { seg ->
                        specByUriKey[seg] = spec
                    }
                    parsed.path?.let { p ->
                        specByUriKey[p] = spec
                    }
                } catch (_: Exception) {}
            }
        }

        val projectsById = allProjects.associateBy { it.id }
        val customersById = allCustomers.associateBy { it.id }

        val attachedList = mutableListOf<SpecAttachmentItem>()
        val unattachedList = mutableListOf<SpecAttachmentItem>()

        for (file in allDiskFiles) {
            val fileUri = Uri.fromFile(file).toString()
            val filename = file.name
            val sizeFormatted = formatFileSize(file.length())

            // Match against active specs
            val matchingSpec = specByUriKey[fileUri] ?: specByUriKey[filename] ?: specByUriKey[file.absolutePath]

            if (matchingSpec != null) {
                val project = projectsById[matchingSpec.projectId]
                val customer = project?.customerId?.let { customersById[it] }
                attachedList.add(
                    SpecAttachmentItem(
                        file = file,
                        uriString = fileUri,
                        projectId = matchingSpec.projectId,
                        projectTitle = project?.title ?: matchingSpec.subTitle,
                        customerName = customer?.name,
                        signType = matchingSpec.signType,
                        isAttached = true,
                        fileSizeFormatted = sizeFormatted
                    )
                )
            } else {
                // Unattached/Orphaned: parse metadata from filename
                val (metaProjId, metaProjTitle, metaCustName) = parseMetadataFromFilename(filename)
                val project = metaProjId?.let { projectsById[it] }
                val customer = project?.customerId?.let { customersById[it] }

                val displayProj = project?.title ?: metaProjTitle ?: "پروژه حذف‌شده"
                val displayCust = customer?.name ?: metaCustName ?: "کارفرمای نامشخص"

                unattachedList.add(
                    SpecAttachmentItem(
                        file = file,
                        uriString = fileUri,
                        projectId = metaProjId,
                        projectTitle = displayProj,
                        customerName = displayCust,
                        signType = null,
                        isAttached = false,
                        fileSizeFormatted = sizeFormatted
                    )
                )
            }
        }

        return Pair(attachedList, unattachedList)
    }

    /**
     * Saves an image from the app's internal storage or URI to the device's public Gallery.
     */
    fun saveImageToPublicGallery(context: Context, imageUriStr: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val uri = Uri.parse(imageUriStr)
            val inputStream = resolver.openInputStream(uri) ?: return false

            val filename = "ALPA_SIGNAGE_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AlpaSignage")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val targetUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

            resolver.openOutputStream(targetUri)?.use { outStream ->
                inputStream.copyTo(outStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Permanently deletes a file from internal storage.
     */
    fun deletePhysicalFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
