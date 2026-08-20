package com.oryno.piggy_ledger.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.clerk.api.Clerk
import com.clerk.api.user.setProfileImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun ProfileSettingsView(viewModel: PiggyLedgerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val userName by viewModel.authUserName.collectAsStateWithLifecycle()
    val userEmail by viewModel.authUserEmail.collectAsStateWithLifecycle()
    val userPhoto by viewModel.authUserPhotoUrl.collectAsStateWithLifecycle()
    
    val cropImage = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                viewModel.setAuthUser(email = userEmail, name = userName, photoUrl = uriContent.toString(), clerkUserId = "")
                scope.launch(Dispatchers.IO) {
                    try {
                        val user = Clerk.user
                        if (user != null) {
                            val path = uriContent.path
                            if (path != null) {
                                val file = java.io.File(path)
                                if (file.exists()) {
                                    user.setProfileImage(file)
                                }
                            }
                        }
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            val exception = result.error
            exception?.printStackTrace()
        }
    }

    val openCropper = {
        cropImage.launch(
            CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = true,
                    imageSourceIncludeCamera = true,
                    fixAspectRatio = true,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    maxCropResultWidth = 1000,
                    maxCropResultHeight = 1000
                )
            )
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
                        model = userPhoto,
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
