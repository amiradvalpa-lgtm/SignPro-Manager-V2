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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.positionChanged
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
import kotlin.math.abs

data class SpecViewerImageItem(
    val uri: String,
    val title: String? = null,
    val projectName: String? = null,
    val customerName: String? = null,
    val isOrphaned: Boolean = false
)

@Composable
fun SpecImageViewerDialog(
    imageItems: List<SpecViewerImageItem> = emptyList(),
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

    // Consolidate image items list with per-image metadata support
    val allItems = remember(imageItems, imageUris, imageUri, title, projectName, customerName, isOrphaned) {
        val explicit = imageItems.filter { it.uri.isNotBlank() }
        if (explicit.isNotEmpty()) {
            explicit
        } else {
            val list = imageUris.filter { it.isNotBlank() }
            val uris = if (list.isNotEmpty()) list
            else if (!imageUri.isNullOrBlank()) listOf(imageUri)
            else emptyList()
            uris.map { uri ->
                SpecViewerImageItem(
                    uri = uri,
                    title = title,
                    projectName = projectName,
                    customerName = customerName,
                    isOrphaned = isOrphaned
                )
            }
        }
    }

    if (allItems.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, allItems.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex, pageCount = { allItems.size })
    val thumbnailListState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)

    // Controls visibility (Default visible, toggleable by tapping on the image)
    var controlsVisible by remember { mutableStateOf(true) }

    // Transform states for zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom and sync thumbnail scroll when page changes
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
        if (allItems.size > 1) {
            thumbnailListState.animateScrollToItem(pagerState.currentPage)
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val currentItem = allItems.getOrElse(pagerState.currentPage) { allItems.first() }
    val currentImageUri = currentItem.uri

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
                val uriForPage = allItems[page].uri
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var zoom = 1f
                                var pan = Offset.Zero
                                var pastTouchSlop = false
                                val touchSlop = viewConfiguration.touchSlop

                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (!canceled) {
                                        val pointerCount = event.changes.count { it.pressed }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        if (!pastTouchSlop) {
                                            zoom *= zoomChange
                                            pan += panChange

                                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                            val zoomMotion = abs(1f - zoom) * centroidSize
                                            val panMotion = pan.getDistance()

                                            if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                                pastTouchSlop = true
                                            }
                                        }

                                        if (pastTouchSlop) {
                                            // Allow HorizontalPager to handle single-finger swipes when not zoomed in
                                            val shouldHandleTransform = pointerCount >= 2 || scale > 1.05f
                                            if (shouldHandleTransform) {
                                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                                    scale = newScale
                                                    if (newScale > 1.05f) {
                                                        val maxX = (size.width * (newScale - 1f)) / 2f
                                                        val maxY = (size.height * (newScale - 1f)) / 2f
                                                        offset = Offset(
                                                            x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                                            y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                                        )
                                                    } else {
                                                        offset = Offset.Zero
                                                    }
                                                }
                                                event.changes.forEach { change ->
                                                    if (change.positionChanged()) {
                                                        change.consume()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } while (!canceled && event.changes.any { it.pressed })
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
                                        offset = Offset.Zero
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
                            if (allItems.size > 1) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${allItems.size}",
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

                        // Row 2: Item Name (Title in next row under controls - updates with current page)
                        val displayTitle = currentItem.title?.ifBlank { null }
                        if (!displayTitle.isNullOrBlank()) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Row 3: Metadata chips (Project & Customer - updates with current page)
                        val displayProj = currentItem.projectName?.ifBlank { null }
                        val displayCust = currentItem.customerName?.ifBlank { null }
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
            if (allItems.size > 1) {
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
                            state = thumbnailListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(allItems.size) { index ->
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
                                        painter = rememberAsyncImagePainter(allItems[index].uri),
                                        contentDescription = "تصویر شماره ${index + 1}",
                                        contentScale = ContentScale.Crop,
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
            itemDetail = currentItem.title ?: currentItem.projectName,
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
            imageItems = listOf(
                SpecViewerImageItem(
                    uri = "https://example.com/sample1.jpg",
                    title = "طرح تابلو چلنیوم",
                    projectName = "تابلو فروشگاه پارس",
                    customerName = "علی رضایی"
                ),
                SpecViewerImageItem(
                    uri = "https://example.com/sample2.jpg",
                    title = "کامپوزیت سردر",
                    projectName = "نمایندگی پارس",
                    customerName = "محمد کریمی"
                )
            ),
            onDismiss = {}
        )
    }
}
