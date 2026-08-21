package com.oryno.piggy_ledger.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.clerk.api.Clerk
import com.clerk.api.user.setProfileImage
import com.clerk.api.user.reload
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsView(viewModel: PiggyLedgerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val userName by viewModel.authUserName.collectAsStateWithLifecycle()
    val userEmail by viewModel.authUserEmail.collectAsStateWithLifecycle()
    val userPhoto by viewModel.authUserPhotoUrl.collectAsStateWithLifecycle()

    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var selectedImageUriForCrop by remember { mutableStateOf<Uri?>(null) }
    var tempCameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUriForCrop = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraCaptureUri != null) {
            selectedImageUriForCrop = tempCameraCaptureUri
        }
    }

    val openCropper = {
        showPhotoSourceSheet = true
    }

    if (showPhotoSourceSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.2f)) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.profile_photo_sheet_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    IconButton(
                        onClick = { showPhotoSourceSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Choose from Gallery
                Surface(
                    onClick = {
                        showPhotoSourceSheet = false
                        try {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } catch (e: Exception) {
                            Log.e("ProfileSettingsView", "Failed to launch visual media picker", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PinkPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.choose_from_gallery),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyDark
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Take Photo with Camera
                Surface(
                    onClick = {
                        showPhotoSourceSheet = false
                        try {
                            val tempFile = File(context.cacheDir, "camera_profile_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            tempCameraCaptureUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            Log.e("ProfileSettingsView", "Failed to launch camera", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.take_photo),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyDark
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (selectedImageUriForCrop != null) {
        ImageCropEditorDialog(
            imageUri = selectedImageUriForCrop!!,
            onDismiss = {
                selectedImageUriForCrop = null
            },
            onConfirm = { croppedUri ->
                selectedImageUriForCrop = null
                // Immediately update local state so the UI updates instantly
                viewModel.setAuthUser(email = userEmail, name = userName, photoUrl = croppedUri.toString(), clerkUserId = "")
                
                scope.launch(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "clerk_avatar_${System.currentTimeMillis()}.jpg")
                    try {
                        // 1. Copy content Uri stream to a real physical File for Clerk SDK upload
                        context.contentResolver.openInputStream(croppedUri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // 2. Upload physical file to Clerk backend
                        val user = Clerk.userFlow.value ?: Clerk.user
                        if (user != null && tempFile.exists() && tempFile.length() > 0) {
                            Log.d("ProfileSettingsView", "Uploading profile image to Clerk (${tempFile.length()} bytes)...")
                            val uploadResult = user.setProfileImage(tempFile)
                            Log.d("ProfileSettingsView", "Clerk setProfileImage result: $uploadResult")
                            
                            // 3. Reload Clerk user state to sync the new remote imageUrl across the session
                            user.reload()
                            
                            val updatedUser = Clerk.userFlow.value ?: Clerk.user
                            val remoteImageUrl = updatedUser?.imageUrl
                            if (!remoteImageUrl.isNullOrBlank()) {
                                withContext(Dispatchers.Main) {
                                    viewModel.setAuthUser(
                                        email = userEmail,
                                        name = userName,
                                        photoUrl = remoteImageUrl,
                                        clerkUserId = updatedUser.id
                                    )
                                }
                            }
                        } else {
                            Log.w("ProfileSettingsView", "Clerk user is null or temp file empty")
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileSettingsView", "Error updating profile image to Clerk", e)
                    } finally {
                        if (tempFile.exists()) {
                            tempFile.delete()
                        }
                    }
                }
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vapor")
    val vaporX by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vaporX"
    )
    val vaporY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vaporY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFCE7F3), // Cool, nice soft pink center
                        Color(0xFFFDF2F8), // Very faint pink
                        Color(0xFFF1F5F9), // Light slate transition
                        Color(0xFFE2E8F0)  // Muted gray-blue edges (no red)
                    ),
                    center = androidx.compose.ui.geometry.Offset(vaporX, vaporY),
                    radius = 2500f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp)
        ) {
            // Profile Image Circle (Base Layer)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                if (userPhoto.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userPhoto)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = stringResource(R.string.profile_change_photo),
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }
            
            // Edit Button (Top Layer, Popped Up)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(54.dp)
                    .shadow(8.dp, CircleShape)
                    .background(Color(0xFF0F172A), CircleShape) // Match the uploaded image's dark button style but with Piggy's dark navy
                    .clickable { openCropper() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.profile_change_photo),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
