package com.oryno.piggy_ledger.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropShape {
    CIRCLE,
    RECTANGLE
}

enum class AspectRatioPreset(val labelRes: Int, val widthRatio: Float, val heightRatio: Float) {
    FREE(R.string.ratio_original, -1f, -1f),
    RATIO_1_1(R.string.ratio_1_1, 1f, 1f),
    RATIO_4_3(R.string.ratio_4_3, 4f, 3f),
    RATIO_3_4(R.string.ratio_3_4, 3f, 4f),
    RATIO_16_9(R.string.ratio_16_9, 16f, 9f),
    RATIO_9_16(R.string.ratio_9_16, 9f, 16f)
}

enum class EditorNavTab {
    CROP,
    ROTATE,
    ZOOM
}

data class CropBoxRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val width: Float get() = max(10f, right - left)
    val height: Float get() = max(10f, bottom - top)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class EditorStateSnapshot(
    val scale: Float,
    val offset: Offset,
    val rotationDegrees: Float,
    val cropShape: CropShape,
    val aspectRatio: AspectRatioPreset,
    val cropBox: CropBoxRect
)

@Composable
fun ImageCropEditorDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ImageCropEditorScreen(
            imageUri = imageUri,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
fun ImageCropEditorScreen(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val densityFloat = density.density

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    // Core transform state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    var cropShape by remember { mutableStateOf(CropShape.CIRCLE) }
    var aspectRatio by remember { mutableStateOf(AspectRatioPreset.RATIO_1_1) }

    // Interactive Crop Box in Canvas coordinate space
    var cropBox by remember { mutableStateOf(CropBoxRect()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var isCropBoxInitialized by remember { mutableStateOf(false) }

    // Active tool tab
    var activeTab by remember { mutableStateOf(EditorNavTab.CROP) }

    // Undo / Redo history
    val undoStack = remember { mutableStateListOf<EditorStateSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorStateSnapshot>() }
    var preGestureSnapshot by remember { mutableStateOf<EditorStateSnapshot?>(null) }
    var isTransforming by remember { mutableStateOf(false) }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(250),
        label = "rotationAnim"
    )

    fun currentSnapshot(): EditorStateSnapshot {
        return EditorStateSnapshot(
            scale = scale,
            offset = offset,
            rotationDegrees = rotationDegrees,
            cropShape = cropShape,
            aspectRatio = aspectRatio,
            cropBox = cropBox
        )
    }

    fun pushUndo(snapshot: EditorStateSnapshot = currentSnapshot()) {
        undoStack.add(snapshot)
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val lastState = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentSnapshot())
            scale = lastState.scale
            offset = lastState.offset
            rotationDegrees = lastState.rotationDegrees
            cropShape = lastState.cropShape
            aspectRatio = lastState.aspectRatio
            cropBox = lastState.cropBox
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentSnapshot())
            scale = nextState.scale
            offset = nextState.offset
            rotationDegrees = nextState.rotationDegrees
            cropShape = nextState.cropShape
            aspectRatio = nextState.aspectRatio
            cropBox = nextState.cropBox
        }
    }

    // Helper to center and fit crop box for a given aspect ratio
    fun updateCropBoxForPreset(preset: AspectRatioPreset, shape: CropShape, cSize: Size) {
        if (cSize.width <= 0 || cSize.height <= 0) return
        val padding = 32f
        val availW = (cSize.width - padding * 2).coerceAtLeast(100f)
        val availH = (cSize.height - padding * 2).coerceAtLeast(100f)

        val targetRatio = when {
            shape == CropShape.CIRCLE -> 1f
            preset == AspectRatioPreset.FREE -> {
                val bmp = sourceBitmap
                if (bmp != null) bmp.width.toFloat() / bmp.height.toFloat() else 1f
            }
            else -> preset.widthRatio / preset.heightRatio
        }

        val (boxW, boxH) = if (availW / availH > targetRatio) {
            val h = min(availH, 360f * densityFloat)
            val w = h * targetRatio
            Pair(w, h)
        } else {
            val w = min(availW, 360f * densityFloat)
            val h = w / targetRatio
            Pair(w, h)
        }

        val l = (cSize.width - boxW) / 2f
        val t = (cSize.height - boxH) / 2f
        cropBox = CropBoxRect(left = l, top = t, right = l + boxW, bottom = t + boxH)
    }

    // Load Bitmap safely on IO thread
    LaunchedEffect(imageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                sourceBitmap = loadOrientedBitmap(context, imageUri)
            } catch (e: Exception) {
                Log.e("ImageCropEditor", "Failed to load bitmap", e)
            }
        }
        isLoading = false
    }

    BackHandler {
        onDismiss()
    }

    Scaffold(
        containerColor = Color(0xFFF1F5F9), // Light studio canvas Slate-100
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { handleUndo() },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (undoStack.isNotEmpty()) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.crop_editor_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            if (undoStack.isNotEmpty()) {
                                Text(
                                    text = "Edited (${undoStack.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PinkPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { handleRedo() },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (redoStack.isNotEmpty()) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isProcessing && sourceBitmap != null) {
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val croppedUri = cropAndSavePrecise(
                                            context = context,
                                            source = sourceBitmap!!,
                                            scale = scale,
                                            offset = offset,
                                            rotationDegrees = rotationDegrees,
                                            cropShape = cropShape,
                                            cropBox = cropBox,
                                            canvasSize = canvasSize
                                        )
                                        withContext(Dispatchers.Main) {
                                            if (croppedUri != null) {
                                                onConfirm(croppedUri)
                                            } else {
                                                isProcessing = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ImageCropEditor", "Crop failed", e)
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing && !isLoading,
                        modifier = Modifier
                            .size(40.dp)
                            .background(PinkPrimary, CircleShape)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Tool Context Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (activeTab) {
                            EditorNavTab.CROP -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Row 1: Shape Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ShapeSelectionChip(
                                            title = stringResource(R.string.shape_circle),
                                            icon = Icons.Default.FitScreen,
                                            isSelected = cropShape == CropShape.CIRCLE,
                                            onClick = {
                                                if (cropShape != CropShape.CIRCLE) {
                                                    pushUndo()
                                                    cropShape = CropShape.CIRCLE
                                                    aspectRatio = AspectRatioPreset.RATIO_1_1
                                                    updateCropBoxForPreset(AspectRatioPreset.RATIO_1_1, CropShape.CIRCLE, canvasSize)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        ShapeSelectionChip(
                                            title = stringResource(R.string.shape_square),
                                            icon = Icons.Default.CropSquare,
                                            isSelected = cropShape == CropShape.RECTANGLE,
                                            onClick = {
                                                if (cropShape != CropShape.RECTANGLE) {
                                                    pushUndo()
                                                    cropShape = CropShape.RECTANGLE
                                                    updateCropBoxForPreset(aspectRatio, CropShape.RECTANGLE, canvasSize)
                                                }
                                            }
                                        )
                                    }

                                    // Row 2: Aspect Ratio Preset Buttons
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AspectRatioPreset.entries.forEach { preset ->
                                            val isSelected = (cropShape == CropShape.RECTANGLE && aspectRatio == preset) ||
                                                    (cropShape == CropShape.CIRCLE && preset == AspectRatioPreset.RATIO_1_1)
                                            val label = stringResource(preset.labelRes)

                                            Surface(
                                                onClick = {
                                                    pushUndo()
                                                    aspectRatio = preset
                                                    cropShape = CropShape.RECTANGLE
                                                    updateCropBoxForPreset(preset, CropShape.RECTANGLE, canvasSize)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) PinkPrimary.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = when (preset) {
                                                            AspectRatioPreset.FREE -> Icons.Default.FitScreen
                                                            AspectRatioPreset.RATIO_1_1 -> Icons.Default.CropSquare
                                                            AspectRatioPreset.RATIO_4_3, AspectRatioPreset.RATIO_16_9 -> Icons.Default.CropLandscape
                                                            AspectRatioPreset.RATIO_3_4, AspectRatioPreset.RATIO_9_16 -> Icons.Default.CropPortrait
                                                        },
                                                        contentDescription = label,
                                                        tint = if (isSelected) PinkPrimary else Color(0xFF475569),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) PinkPrimary else Color(0xFF334155)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            EditorNavTab.ROTATE -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ToolActionButton(
                                        icon = Icons.Default.RotateLeft,
                                        label = "-90°",
                                        onClick = {
                                            pushUndo()
                                            rotationDegrees = (rotationDegrees - 90f + 360f) % 360f
                                        }
                                    )
                                    ToolActionButton(
                                        icon = Icons.Default.Rotate90DegreesCw,
                                        label = "+90°",
                                        isPrimary = true,
                                        onClick = {
                                            pushUndo()
                                            rotationDegrees = (rotationDegrees + 90f) % 360f
                                        }
                                    )
                                    ToolActionButton(
                                        icon = Icons.Default.RestartAlt,
                                        label = stringResource(R.string.reset_transform_label),
                                        onClick = {
                                            if (rotationDegrees != 0f) {
                                                pushUndo()
                                                rotationDegrees = 0f
                                            }
                                        }
                                    )
                                }
                            }

                            EditorNavTab.ZOOM -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                pushUndo()
                                                scale = (scale - 0.25f).coerceIn(0.8f, 5f)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomOut,
                                                contentDescription = "Zoom Out",
                                                tint = Color(0xFF475569)
                                            )
                                        }

                                        Slider(
                                            value = scale,
                                            onValueChange = {
                                                if (!isTransforming) {
                                                    pushUndo()
                                                    isTransforming = true
                                                }
                                                scale = it
                                            },
                                            onValueChangeFinished = {
                                                isTransforming = false
                                            },
                                            valueRange = 0.8f..4f,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = PinkPrimary,
                                                activeTrackColor = PinkPrimary,
                                                inactiveTrackColor = Color(0xFFE2E8F0)
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                pushUndo()
                                                scale = (scale + 0.25f).coerceIn(0.8f, 5f)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Zoom In",
                                                tint = Color(0xFF475569)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${(scale * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = stringResource(R.string.reset_transform_label),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PinkPrimary,
                                            modifier = Modifier.clickable {
                                                if (scale != 1f || offset != Offset.Zero) {
                                                    pushUndo()
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // Navigation Tabs (Crop, Rotate, Zoom)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorNavTabButton(
                            title = stringResource(R.string.crop_tool_label),
                            icon = Icons.Default.Crop,
                            isActive = activeTab == EditorNavTab.CROP,
                            onClick = { activeTab = EditorNavTab.CROP }
                        )

                        EditorNavTabButton(
                            title = stringResource(R.string.rotate_tool_label),
                            icon = Icons.Default.RotateRight,
                            isActive = activeTab == EditorNavTab.ROTATE,
                            onClick = { activeTab = EditorNavTab.ROTATE }
                        )

                        EditorNavTabButton(
                            title = stringResource(R.string.zoom_tool_label),
                            icon = Icons.Default.ZoomIn,
                            isActive = activeTab == EditorNavTab.ZOOM,
                            onClick = { activeTab = EditorNavTab.ZOOM }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFE2E8F0)), // Light Slate canvas background
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PinkPrimary)
            } else if (sourceBitmap != null) {
                val bitmap = sourceBitmap!!

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .onGloballyPositioned { coordinates ->
                            val newSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                            if (newSize != canvasSize) {
                                canvasSize = newSize
                                if (!isCropBoxInitialized) {
                                    updateCropBoxForPreset(aspectRatio, cropShape, newSize)
                                    isCropBoxInitialized = true
                                }
                            }
                        }
                ) {
                    // 1. The Real User Image (rendered fully visible, crisp, transformed)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (preGestureSnapshot == null) {
                                        preGestureSnapshot = currentSnapshot()
                                        pushUndo(preGestureSnapshot!!)
                                    }
                                    scale = (scale * zoom).coerceIn(0.8f, 5f)
                                    offset = Offset(
                                        x = offset.x + pan.x,
                                        y = offset.y + pan.y
                                    )
                                }
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.all { !it.pressed }) {
                                            preGestureSnapshot = null
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Selected Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                    rotationZ = animatedRotation
                                },
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 2. Interactive Crop Overlay Canvas (Scrim + Grid + Corner Handles + Draggable Box)
                    var dragMode by remember { mutableStateOf<DragHandleMode>(DragHandleMode.NONE) }
                    var initialDragBox by remember { mutableStateOf(CropBoxRect()) }

                    val handleTouchRadius = with(density) { 36.dp.toPx() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(canvasSize, cropBox) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        pushUndo()
                                        initialDragBox = cropBox
                                        dragMode = getDragMode(startOffset, cropBox, handleTouchRadius)
                                    },
                                    onDragEnd = {
                                        dragMode = DragHandleMode.NONE
                                    },
                                    onDragCancel = {
                                        dragMode = DragHandleMode.NONE
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val minSize = 100f
                                        val maxW = canvasSize.width
                                        val maxH = canvasSize.height

                                        when (dragMode) {
                                            DragHandleMode.MOVE_BOX -> {
                                                val bw = cropBox.width
                                                val bh = cropBox.height
                                                val newL = (cropBox.left + dragAmount.x).coerceIn(0f, maxW - bw)
                                                val newT = (cropBox.top + dragAmount.y).coerceIn(0f, maxH - bh)
                                                cropBox = CropBoxRect(
                                                    left = newL,
                                                    top = newT,
                                                    right = newL + bw,
                                                    bottom = newT + bh
                                                )
                                            }
                                            DragHandleMode.TOP_LEFT -> {
                                                val newL = (cropBox.left + dragAmount.x).coerceIn(0f, cropBox.right - minSize)
                                                val newT = (cropBox.top + dragAmount.y).coerceIn(0f, cropBox.bottom - minSize)
                                                cropBox = cropBox.copy(left = newL, top = newT)
                                            }
                                            DragHandleMode.TOP_RIGHT -> {
                                                val newR = (cropBox.right + dragAmount.x).coerceIn(cropBox.left + minSize, maxW)
                                                val newT = (cropBox.top + dragAmount.y).coerceIn(0f, cropBox.bottom - minSize)
                                                cropBox = cropBox.copy(right = newR, top = newT)
                                            }
                                            DragHandleMode.BOTTOM_LEFT -> {
                                                val newL = (cropBox.left + dragAmount.x).coerceIn(0f, cropBox.right - minSize)
                                                val newB = (cropBox.bottom + dragAmount.y).coerceIn(cropBox.top + minSize, maxH)
                                                cropBox = cropBox.copy(left = newL, bottom = newB)
                                            }
                                            DragHandleMode.BOTTOM_RIGHT -> {
                                                val newR = (cropBox.right + dragAmount.x).coerceIn(cropBox.left + minSize, maxW)
                                                val newB = (cropBox.bottom + dragAmount.y).coerceIn(cropBox.top + minSize, maxH)
                                                cropBox = cropBox.copy(right = newR, bottom = newB)
                                            }
                                            DragHandleMode.NONE -> {}
                                        }
                                    }
                                )
                            }
                    ) {
                        val scrimColor = Color.Black.copy(alpha = 0.55f)
                        val cb = cropBox

                        // Draw Scrim Surrounding Crop Box (Top, Bottom, Left, Right)
                        // Top
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset.Zero,
                            size = Size(size.width, cb.top)
                        )
                        // Bottom
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, cb.bottom),
                            size = Size(size.width, size.height - cb.bottom)
                        )
                        // Left
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, cb.top),
                            size = Size(cb.left, cb.height)
                        )
                        // Right
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(cb.right, cb.top),
                            size = Size(size.width - cb.right, cb.height)
                        )

                        // Draw Crop Box Border & Shape Guides
                        if (cropShape == CropShape.CIRCLE) {
                            val radius = min(cb.width, cb.height) / 2f
                            val center = Offset(cb.centerX, cb.centerY)

                            // Circle crop guide
                            drawCircle(
                                color = PinkPrimary,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            // Subtle white outer ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.6f),
                                radius = radius + 1.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        } else {
                            // Rectangle crop border
                            drawRect(
                                color = PinkPrimary,
                                topLeft = Offset(cb.left, cb.top),
                                size = Size(cb.width, cb.height),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // 3x3 Rule-of-Thirds Grid Lines
                            val thirdW = cb.width / 3f
                            val thirdH = cb.height / 3f
                            val gridColor = Color.White.copy(alpha = 0.45f)
                            val gridStroke = 1.dp.toPx()

                            // Vertical grid lines
                            drawLine(gridColor, Offset(cb.left + thirdW, cb.top), Offset(cb.left + thirdW, cb.bottom), strokeWidth = gridStroke)
                            drawLine(gridColor, Offset(cb.left + thirdW * 2, cb.top), Offset(cb.left + thirdW * 2, cb.bottom), strokeWidth = gridStroke)

                            // Horizontal grid lines
                            drawLine(gridColor, Offset(cb.left, cb.top + thirdH), Offset(cb.right, cb.top + thirdH), strokeWidth = gridStroke)
                            drawLine(gridColor, Offset(cb.left, cb.top + thirdH * 2), Offset(cb.right, cb.top + thirdH * 2), strokeWidth = gridStroke)
                        }

                        // Draw L-Shaped Corner Bracket Handles (Top-Left, Top-Right, Bottom-Left, Bottom-Right)
                        val bracketLen = 22.dp.toPx()
                        val bracketStroke = 4.dp.toPx()
                        val handleColor = Color.White

                        // Top-Left
                        drawLine(handleColor, Offset(cb.left - 2f, cb.top), Offset(cb.left + bracketLen, cb.top), strokeWidth = bracketStroke)
                        drawLine(handleColor, Offset(cb.left, cb.top - 2f), Offset(cb.left, cb.top + bracketLen), strokeWidth = bracketStroke)

                        // Top-Right
                        drawLine(handleColor, Offset(cb.right + 2f, cb.top), Offset(cb.right - bracketLen, cb.top), strokeWidth = bracketStroke)
                        drawLine(handleColor, Offset(cb.right, cb.top - 2f), Offset(cb.right, cb.top + bracketLen), strokeWidth = bracketStroke)

                        // Bottom-Left
                        drawLine(handleColor, Offset(cb.left - 2f, cb.bottom), Offset(cb.left + bracketLen, cb.bottom), strokeWidth = bracketStroke)
                        drawLine(handleColor, Offset(cb.left, cb.bottom + 2f), Offset(cb.left, cb.bottom - bracketLen), strokeWidth = bracketStroke)

                        // Bottom-Right
                        drawLine(handleColor, Offset(cb.right + 2f, cb.bottom), Offset(cb.right - bracketLen, cb.bottom), strokeWidth = bracketStroke)
                        drawLine(handleColor, Offset(cb.right, cb.bottom + 2f), Offset(cb.right, cb.bottom - bracketLen), strokeWidth = bracketStroke)
                    }

                    // Helpful Hint Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drag box to move • Drag corners to resize • Pinch to zoom",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Failed to load image",
                    color = Color(0xFF64748B),
                    fontSize = 15.sp
                )
            }
        }
    }
}

private enum class DragHandleMode {
    NONE,
    MOVE_BOX,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

private fun getDragMode(touch: Offset, box: CropBoxRect, radius: Float): DragHandleMode {
    fun dist(x1: Float, y1: Float, x2: Float, y2: Float) =
        kotlin.math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toFloat()

    if (dist(touch.x, touch.y, box.left, box.top) <= radius) return DragHandleMode.TOP_LEFT
    if (dist(touch.x, touch.y, box.right, box.top) <= radius) return DragHandleMode.TOP_RIGHT
    if (dist(touch.x, touch.y, box.left, box.bottom) <= radius) return DragHandleMode.BOTTOM_LEFT
    if (dist(touch.x, touch.y, box.right, box.bottom) <= radius) return DragHandleMode.BOTTOM_RIGHT

    // Inside box -> drag box
    if (touch.x >= box.left && touch.x <= box.right && touch.y >= box.top && touch.y <= box.bottom) {
        return DragHandleMode.MOVE_BOX
    }

    return DragHandleMode.NONE
}

@Composable
private fun EditorNavTabButton(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(
        targetValue = if (isActive) PinkPrimary else Color(0xFF94A3B8),
        label = "tabTint"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) PinkPrimary.copy(alpha = 0.15f) else Color.Transparent,
        label = "tabBg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = tintColor
        )
    }
}

@Composable
private fun ShapeSelectionChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) PinkPrimary else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF334155)
            )
        }
    }
}

@Composable
private fun ToolActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = if (isPrimary) PinkPrimary else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPrimary) PinkPrimary else Color(0xFFE2E8F0)
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPrimary) Color.White else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPrimary) Color.White else Color(0xFF334155)
            )
        }
    }
}

/**
 * Loads the bitmap with correct EXIF orientation handling.
 */
private fun loadOrientedBitmap(context: Context, uri: Uri): Bitmap? {
    val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
    val original = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    if (original == null) return null

    var orientation = ExifInterface.ORIENTATION_NORMAL
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (e: Exception) {
        Log.w("ImageCropEditor", "Failed to read EXIF orientation", e)
    }

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
    }

    return if (matrix.isIdentity) {
        original
    } else {
        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    }
}

/**
 * High-precision crop matching the on-screen crop box and gesture transforms.
 */
private fun cropAndSavePrecise(
    context: Context,
    source: Bitmap,
    scale: Float,
    offset: Offset,
    rotationDegrees: Float,
    cropShape: CropShape,
    cropBox: CropBoxRect,
    canvasSize: Size
): Uri? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0 || cropBox.width <= 0 || cropBox.height <= 0) {
        return null
    }

    // 1. Calculate how Image(contentScale = Fit) maps the bitmap onto the canvas
    val bmpW = source.width.toFloat()
    val bmpH = source.height.toFloat()
    val canvasW = canvasSize.width
    val canvasH = canvasSize.height

    val fitScale = min(canvasW / bmpW, canvasH / bmpH)
    val fittedW = bmpW * fitScale
    val fittedH = bmpH * fitScale

    val baseImgLeft = (canvasW - fittedW) / 2f
    val baseImgTop = (canvasH - fittedH) / 2f

    // 2. High-resolution output canvas
    val maxOutputDimension = 1200f
    val cropRatio = cropBox.width / cropBox.height

    val (outW, outH) = if (cropRatio >= 1f) {
        val w = maxOutputDimension
        val h = (maxOutputDimension / cropRatio).roundToInt().coerceAtLeast(100)
        Pair(w.toInt(), h)
    } else {
        val h = maxOutputDimension
        val w = (maxOutputDimension * cropRatio).roundToInt().coerceAtLeast(100)
        Pair(w.toInt(), h.toInt())
    }

    val croppedBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(croppedBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val matrix = Matrix()

    // Base fit transformation in canvas coordinates
    matrix.postScale(fitScale, fitScale)
    matrix.postTranslate(baseImgLeft, baseImgTop)

    // User gesture scale & pan around canvas center
    val canvasCenterX = canvasW / 2f
    val canvasCenterY = canvasH / 2f
    matrix.postScale(scale, scale, canvasCenterX, canvasCenterY)
    matrix.postTranslate(offset.x, offset.y)
    matrix.postRotate(rotationDegrees, canvasCenterX, canvasCenterY)

    // Translate by crop box origin to align with output top-left
    matrix.postTranslate(-cropBox.left, -cropBox.top)

    // Scale from crop box dimensions to output resolution
    val outputScaleX = outW.toFloat() / cropBox.width
    val outputScaleY = outH.toFloat() / cropBox.height
    matrix.postScale(outputScaleX, outputScaleY)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)

    // Apply circular mask if Circle shape was chosen
    val finalBitmap = if (cropShape == CropShape.CIRCLE) {
        val circleOutput = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(circleOutput)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
        }
        val radius = min(outW, outH) / 2f
        circleCanvas.drawCircle(outW / 2f, outH / 2f, radius, maskPaint)

        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        circleCanvas.drawBitmap(croppedBitmap, 0f, 0f, maskPaint)
        croppedBitmap.recycle()
        circleOutput
    } else {
        croppedBitmap
    }

    // Save to temp cache file
    val outFile = File(context.cacheDir, "cropped_photo_${System.currentTimeMillis()}.jpg")
    FileOutputStream(outFile).use { out ->
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
    }
    finalBitmap.recycle()

    return Uri.fromFile(outFile)
}
