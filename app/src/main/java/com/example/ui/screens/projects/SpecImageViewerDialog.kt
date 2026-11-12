package com.example.ui.screens.projects

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch

@Composable
fun SpecImageViewerDialog(
    imageUris: List<String> = emptyList(),
    imageUri: String? = null,
    initialIndex: Int = 0,
    title: String? = null,
    projectName: String? = null,
    customerName: String? = null,
    isOrphaned: Boolean = false,
    onDeleteFile: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Consolidate image uris list
    val allUris = remember(imageUris, imageUri) {
        val list = imageUris.filter { it.isNotBlank() }
        if (list.isNotEmpty()) list
        else if (!imageUri.isNullOrBlank()) listOf(imageUri)
        else emptyList()
    }

    if (allUris.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, allUris.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex, pageCount = { allUris.size })

    // Controls visibility (Default visible, toggleable by tapping on the image)
    var controlsVisible by remember { mutableStateOf(true) }

    // Transform states for zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when page changes
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val currentImageUri = allUris.getOrElse(pagerState.currentPage) { allUris.first() }

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
            // Interactive Pager for swiping between images when not zoomed in
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = (scale <= 1.05f),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val uriForPage = allUris[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(page) {
                            detectTapGestures(
                                onTap = {
                                    controlsVisible = !controlsVisible
                                },
                                onDoubleTap = {
                                    if (scale > 1.05f) {
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
                        painter = rememberAsyncImagePainter(uriForPage),
                        contentDescription = "تصویر طرح تابلو",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (page == pagerState.currentPage) scale else 1f,
                                scaleY = if (page == pagerState.currentPage) scale else 1f,
                                translationX = if (page == pagerState.currentPage && scale > 1f) offset.x else 0f,
                                translationY = if (page == pagerState.currentPage && scale > 1f) offset.y else 0f
                            )
                    )
                }
            }

            // Top Control Bar & Metadata Info (with smooth show/hide animation)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Action Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    tint = Color.White
                                )
                            }

                            // Counter indicator when there are multiple images
                            if (allUris.size > 1) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${allUris.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Top Action Buttons Row
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Save to Gallery
                                IconButton(
                                    onClick = {
                                        val success = FileStorageHelper.saveImageToPublicGallery(context, currentImageUri)
                                        if (success) {
                                            Toast.makeText(context, "تصویر با موفقیت در گالری گوشی ذخیره شد.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "خطا در ذخیره تصویر در گالری.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "ذخیره در گالری",
                                        tint = Color.White
                                    )
                                }

                                // Delete File from Storage (standard transparent black background like other buttons)
                                if (onDeleteFile != null) {
                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
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
                                    onClick = { openInGallery(context, currentImageUri) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "باز کردن در گالری",
                                        tint = Color.White
                                    )
                                }

                                // Share Image
                                IconButton(
                                    onClick = { shareImage(context, currentImageUri) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "اشتراک‌گذاری تصویر",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Row 2: Item Name (Title in next row under controls)
                        val displayTitle = title?.ifBlank { null }
                        if (!displayTitle.isNullOrBlank()) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Row 3: Metadata chips (Project & Customer)
                        val displayProj = projectName?.ifBlank { null }
                        val displayCust = customerName?.ifBlank { null }
                        if (!displayProj.isNullOrBlank() || !displayCust.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!displayProj.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "پروژه: $displayProj",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (!displayCust.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "کارفرما: $displayCust",
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
            }

            // Bottom Thumbnails Row when multiple images exist (with show/hide animation)
            if (allUris.size > 1) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(allUris.size) { index ->
                                val isSelected = (index == pagerState.currentPage)
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(allUris[index]),
                                        contentDescription = "تصویر شماره ${index + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
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
                onDeleteFile(currentImageUri)
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
            imageUris = listOf("https://example.com/sample1.jpg", "https://example.com/sample2.jpg"),
            title = "طرح تابلو چلنیوم",
            projectName = "تابلو فروشگاه پارس",
            customerName = "علی رضایی",
            isOrphaned = false,
            onDismiss = {}
        )
    }
}
