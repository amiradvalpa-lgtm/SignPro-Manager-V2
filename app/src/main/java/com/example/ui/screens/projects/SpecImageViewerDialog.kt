package com.example.ui.screens.projects

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.ui.components.common.DeleteConfirmationDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FileStorageHelper

@Composable
fun SpecImageViewerDialog(
    imageUri: String,
    title: String? = null,
    projectName: String? = null,
    customerName: String? = null,
    isOrphaned: Boolean = false,
    onDeleteFile: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Interactive Zoomable and Pannable Image Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "تصویر طرح تابلو با امکان زوم",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = if (scale > 1f) offset.x else 0f,
                            translationY = if (scale > 1f) offset.y else 0f
                        )
                )
            }

            // Top Control Bar & Metadata Info
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = Color.White
                            )
                        }

                        // Title in Center
                        if (!title.isNullOrBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                        }

                        // Top Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Save to Device Gallery Button
                            IconButton(
                                onClick = {
                                    val success = FileStorageHelper.saveImageToPublicGallery(context, imageUri)
                                    if (success) {
                                        Toast.makeText(context, "تصویر با موفقیت در گالری گوشی ذخیره شد.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "خطا در ذخیره تصویر در گالری.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "ذخیره در گالری",
                                    tint = Color.White
                                )
                            }

                            // Delete File from Storage (for unattached files)
                            if (onDeleteFile != null) {
                                IconButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier.background(Color.Red.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف فایل از حافظه",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Open in System Gallery Viewer
                            IconButton(
                                onClick = { openInGallery(context, imageUri) },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "باز کردن در گالری",
                                    tint = Color.White
                                )
                            }

                            // Share Image
                            IconButton(
                                onClick = { shareImage(context, imageUri) },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "اشتراک‌گذاری تصویر",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Metadata Row: Project Title & Customer Name & Orphaned Tag
                    if (!projectName.isNullOrBlank() || !customerName.isNullOrBlank() || isOrphaned) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!projectName.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "پروژه: $projectName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (!customerName.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "کارفرما: $customerName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (isOrphaned) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "آیتم این تصویر حذف شده است",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Hint for Double Tap & Zoom
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = if (scale > 1.05f) "برای ریست دوباره دوبار لمس کنید" else "با دو انگشت زوم کنید یا دوبار ضربه بزنید",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (showDeleteConfirmDialog && onDeleteFile != null) {
        DeleteConfirmationDialog(
            show = true,
            title = "حذف فایل از حافظه برنامه",
            message = "آیا از حذف دائمی این تصویر از حافظه دستگاه اطمینان دارید؟ این فایل قابل بازیابی نخواهد بود.",
            itemDetail = title ?: projectName,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeleteFile()
                onDismiss()
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

private fun shareImage(context: Context, uriStr: String) {
    try {
        val uri = Uri.parse(uriStr)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر طرح تابلو"))
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در اشتراک‌گذاری تصویر: ${e.localizedMessage ?: "ناموفق"}", Toast.LENGTH_SHORT).show()
    }
}

private fun openInGallery(context: Context, uriStr: String) {
    try {
        val uri = Uri.parse(uriStr)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(viewIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "برنامه‌ای برای نمایش این تصویر یافت نشد.", Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "Spec Image Viewer Preview")
@Composable
fun SpecImageViewerDialogPreview() {
    MyApplicationTheme {
        SpecImageViewerDialog(
            imageUri = "https://example.com/sample.jpg",
            title = "طرح تابلو چلنیوم",
            projectName = "تابلو فروشگاه پارس",
            customerName = "علی رضایی",
            isOrphaned = false,
            onDismiss = {}
        )
    }
}
