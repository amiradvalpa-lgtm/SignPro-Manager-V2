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
            .replace("[/\\\\:*?\"<>|\\s~]+".toRegex(), "_")
            .take(30)
    }

    data class ParsedAttachmentMetadata(
        val projectId: String?,
        val projectTitle: String?,
        val customerName: String?,
        val signType: String?
    )

    /**
     * Encodes metadata into the filename:
     * ATTACH__pid~[projectId]__proj~[projectTitle]__cust~[customerName]__spec~[signType]__[timestamp]_[uuid].jpg
     */
    fun createInternalCameraFile(
        context: Context,
        projectId: String,
        projectTitle: String,
        customerName: String,
        signType: String = ""
    ): File {
        val dir = getAttachmentsDir(context)
        val safeProj = sanitizeForFilename(projectTitle.ifBlank { "پروژه" })
        val safeCust = sanitizeForFilename(customerName.ifBlank { "کارفرما" })
        val safeSpec = sanitizeForFilename(signType.ifBlank { "مشخصات" })
        val safeId = sanitizeForFilename(projectId)
        val filename = "ATTACH__pid~${safeId}__proj~${safeProj}__cust~${safeCust}__spec~${safeSpec}__${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
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
        customerName: String,
        signType: String = ""
    ): Uri? {
        return try {
            val targetFile = createInternalCameraFile(context, projectId, projectTitle, customerName, signType)
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
     * Parses metadata from file name. Supports both modern ATTACH__ format and legacy ATTACH_ format.
     */
    fun parseMetadataFromFilename(filename: String): ParsedAttachmentMetadata {
        return try {
            if (!filename.startsWith("ATTACH")) return ParsedAttachmentMetadata(null, null, null, null)
            val withoutExt = filename.substringBeforeLast(".")

            // Check if modern format with key~value pairs
            if (withoutExt.contains("__pid~") || withoutExt.contains("__proj~")) {
                var projectId: String? = null
                var projectTitle: String? = null
                var customerName: String? = null
                var signType: String? = null

                val tokens = withoutExt.split("__")
                for (token in tokens) {
                    when {
                        token.startsWith("pid~") -> projectId = token.removePrefix("pid~")
                        token.startsWith("proj~") -> projectTitle = token.removePrefix("proj~").replace("_", " ")
                        token.startsWith("cust~") -> customerName = token.removePrefix("cust~").replace("_", " ")
                        token.startsWith("spec~") -> signType = token.removePrefix("spec~").replace("_", " ")
                    }
                }
                return ParsedAttachmentMetadata(projectId, projectTitle, customerName, signType)
            }

            // Legacy format fallback: ATTACH_p[id]_[proj]_c[cust]_[time]_[uuid]
            val parts = withoutExt.removePrefix("ATTACH_").split("_")
            var projectId: String? = null
            var projectTitle: String? = null
            var customerName: String? = null

            val pIndex = parts.indexOfFirst { it.startsWith("p") }
            val cIndex = parts.indexOfFirst { it.startsWith("c") }

            if (pIndex != -1) {
                projectId = parts[pIndex].removePrefix("p")
            }
            if (pIndex != -1 && cIndex != -1 && cIndex > pIndex + 1) {
                projectTitle = parts.subList(pIndex + 1, cIndex).joinToString(" ")
            }
            if (cIndex != -1 && cIndex + 1 < parts.size) {
                val remaining = parts.subList(cIndex, parts.size - 2)
                customerName = remaining.joinToString(" ").removePrefix("c")
            }

            ParsedAttachmentMetadata(projectId, projectTitle?.replace("_", " "), customerName?.replace("_", " "), null)
        } catch (e: Exception) {
            ParsedAttachmentMetadata(null, null, null, null)
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
     * Takes both active specs and raw specs (including soft-deleted ones) so that
     * orphaned files retain full item, project, and customer identity.
     */
    fun scanAttachments(
        context: Context,
        allSpecs: List<ProjectSpecEntity>,
        allProjects: List<ProjectEntity>,
        allCustomers: List<CustomerEntity>,
        rawSpecs: List<ProjectSpecEntity> = allSpecs,
        rawProjects: List<ProjectEntity> = allProjects,
        rawCustomers: List<CustomerEntity> = allCustomers
    ): Pair<List<SpecAttachmentItem>, List<SpecAttachmentItem>> {
        val dir = getAttachmentsDir(context)
        val files = dir.listFiles()?.filter { it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".png") || it.name.endsWith(".jpeg")) }
            ?: emptyList()

        // Also check legacy cache dir if exists
        val cachePhotos = File(context.cacheDir, "camera_photos").listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".png")) } ?: emptyList()

        val allDiskFiles = (files + cachePhotos).distinctBy { it.absolutePath }

        // Map URIs from active database specs
        val activeSpecByUriKey = mutableMapOf<String, ProjectSpecEntity>()
        for (spec in allSpecs) {
            val uris = spec.attachmentUris.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (u in uris) {
                activeSpecByUriKey[u] = spec
                try {
                    val parsed = Uri.parse(u)
                    parsed.lastPathSegment?.let { seg -> activeSpecByUriKey[seg] = spec }
                    parsed.path?.let { p -> activeSpecByUriKey[p] = spec }
                } catch (_: Exception) {}
            }
        }

        // Map URIs from raw database specs (including soft-deleted)
        val rawSpecByUriKey = mutableMapOf<String, ProjectSpecEntity>()
        for (spec in rawSpecs) {
            val uris = spec.attachmentUris.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (u in uris) {
                rawSpecByUriKey[u] = spec
                try {
                    val parsed = Uri.parse(u)
                    parsed.lastPathSegment?.let { seg -> rawSpecByUriKey[seg] = spec }
                    parsed.path?.let { p -> rawSpecByUriKey[p] = spec }
                } catch (_: Exception) {}
            }
        }

        val projectsById = (rawProjects + allProjects).distinctBy { it.id }.associateBy { it.id }
        val customersById = (rawCustomers + allCustomers).distinctBy { it.id }.associateBy { it.id }

        val attachedList = mutableListOf<SpecAttachmentItem>()
        val unattachedList = mutableListOf<SpecAttachmentItem>()

        for (file in allDiskFiles) {
            val fileUri = Uri.fromFile(file).toString()
            val filename = file.name
            val sizeFormatted = formatFileSize(file.length())

            // 1. Match against active specs
            val activeSpec = activeSpecByUriKey[fileUri] ?: activeSpecByUriKey[filename] ?: activeSpecByUriKey[file.absolutePath]

            if (activeSpec != null) {
                val project = projectsById[activeSpec.projectId]
                val customer = project?.customerId?.let { customersById[it] }
                attachedList.add(
                    SpecAttachmentItem(
                        file = file,
                        uriString = fileUri,
                        projectId = activeSpec.projectId,
                        projectTitle = project?.title ?: activeSpec.subTitle,
                        customerName = customer?.name,
                        signType = activeSpec.signType,
                        isAttached = true,
                        fileSizeFormatted = sizeFormatted
                    )
                )
            } else {
                // 2. Unattached from active specs. Check if it belonged to a soft-deleted spec or project in raw database!
                val historicalSpec = rawSpecByUriKey[fileUri] ?: rawSpecByUriKey[filename] ?: rawSpecByUriKey[file.absolutePath]
                val meta = parseMetadataFromFilename(filename)

                val projId = historicalSpec?.projectId ?: meta.projectId
                val project = projId?.let { projectsById[it] }
                val customer = project?.customerId?.let { customersById[it] }

                val displayTitle = historicalSpec?.signType
                    ?: meta.signType?.ifBlank { null }
                    ?: "طرح تابلوسازی"

                val displayProj = project?.title
                    ?: historicalSpec?.subTitle
                    ?: meta.projectTitle?.ifBlank { null }
                    ?: "پروژه تابلوسازی"

                val displayCust = customer?.name
                    ?: meta.customerName?.ifBlank { null }
                    ?: "کارفرمای پروژه"

                unattachedList.add(
                    SpecAttachmentItem(
                        file = file,
                        uriString = fileUri,
                        projectId = projId,
                        projectTitle = displayProj,
                        customerName = displayCust,
                        signType = displayTitle,
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
