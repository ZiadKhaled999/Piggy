package com.oryno.piggy_ledger.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.reload
import com.clerk.api.user.setProfileImage
import com.clerk.api.user.update
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream

private enum class ProfileEditableField {
    NAME, PHONE
}

internal fun extractClerkFullName(user: User?): String {
    if (user == null) return ""
    return listOfNotNull(user.firstName, user.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { user.username.orEmpty() }
}

internal fun extractClerkEmail(user: User?): String {
    if (user == null) return ""
    return user.primaryEmailAddress?.emailAddress
        ?: user.emailAddresses?.firstOrNull()?.emailAddress
        ?: ""
}

internal fun extractClerkPhone(user: User?): String {
    if (user == null) return ""
    val metaPhone = try {
        user.unsafeMetadata?.get("phone")?.jsonPrimitive?.contentOrNull
    } catch (_: Exception) {
        null
    }
    return metaPhone?.takeIf { it.isNotBlank() }
        ?: user.primaryPhoneNumber?.phoneNumber
        ?: user.phoneNumbers.firstOrNull()?.phoneNumber
        ?: ""
}

internal fun extractClerkBirthDate(user: User?): String {
    if (user == null) return ""
    return try {
        val raw = user.unsafeMetadata?.get("birthDate")?.jsonPrimitive?.contentOrNull
            ?: user.unsafeMetadata?.get("birthday")?.jsonPrimitive?.contentOrNull
        raw ?: ""
    } catch (_: Exception) {
        ""
    }
}

private fun extractClerkFailureMessage(context: Context, result: ClerkResult.Failure<*>): String {
    val errorResponse = result.error as? ClerkErrorResponse
    val clerkError = errorResponse?.errors?.firstOrNull()
    return clerkError?.longMessage
        ?: clerkError?.message
        ?: result.throwable?.message
        ?: context.getString(R.string.profile_save_failed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsView(viewModel: PiggyLedgerViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val userName by viewModel.authUserName.collectAsStateWithLifecycle()
    val userEmail by viewModel.authUserEmail.collectAsStateWithLifecycle()
    val userPhoto by viewModel.authUserPhotoUrl.collectAsStateWithLifecycle()
    val userPhone by viewModel.authUserPhone.collectAsStateWithLifecycle()
    val userBirth by viewModel.authUserBirthDate.collectAsStateWithLifecycle()
    val clerkUser by Clerk.userFlow.collectAsStateWithLifecycle()

    var nameInput by remember { mutableStateOf(userName) }
    var birthInput by remember { mutableStateOf(userBirth) }
    var phoneInput by remember { mutableStateOf(userPhone) }
    var emailInput by remember { mutableStateOf(userEmail) }
    var isInitializedFromClerk by remember { mutableStateOf(false) }

    var editingField by remember { mutableStateOf<ProfileEditableField?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var isSaveError by remember { mutableStateOf(false) }

    var showBirthdayPicker by remember { mutableStateOf(false) }

    LaunchedEffect(userName, userPhone, userBirth, userEmail) {
        if (!isSaving && editingField == null) {
            if (nameInput.isBlank() && userName.isNotBlank()) nameInput = userName
            if (birthInput.isBlank() && userBirth.isNotBlank()) birthInput = userBirth
            if (phoneInput.isBlank() && userPhone.isNotBlank()) phoneInput = userPhone
            if (emailInput.isBlank() && userEmail.isNotBlank()) emailInput = userEmail
        }
    }

    // Sync latest profile fields from Clerk when reloaded cleanly
    LaunchedEffect(Unit) {
        val clerkReloaded = withContext(Dispatchers.IO) {
            try {
                val activeUser = Clerk.userFlow.value ?: Clerk.user
                if (activeUser != null) {
                    activeUser.reload() is ClerkResult.Success
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.w("ProfileSettingsView", "Clerk reload skipped/failed: ${e.message}")
                false
            }
        }
        val refreshedUser = Clerk.userFlow.value ?: Clerk.user ?: clerkUser
        val latestLocalName = viewModel.authUserName.value
        val latestLocalEmail = viewModel.authUserEmail.value
        val latestLocalPhone = viewModel.authUserPhone.value
        val latestLocalBirth = viewModel.authUserBirthDate.value
        val latestLocalPhoto = viewModel.authUserPhotoUrl.value

        if (refreshedUser != null && !isInitializedFromClerk) {
            val clerkName = extractClerkFullName(refreshedUser)
            val resolvedName = if (clerkReloaded && clerkName.isNotBlank()) {
                clerkName
            } else {
                latestLocalName.ifBlank { clerkName }
            }
            val resolvedEmail = extractClerkEmail(refreshedUser).ifBlank { latestLocalEmail }
            val clerkPhone = extractClerkPhone(refreshedUser)
            val resolvedPhone = if (clerkReloaded && clerkPhone.isNotBlank()) {
                clerkPhone
            } else {
                latestLocalPhone.ifBlank { clerkPhone }
            }
            val clerkBirth = extractClerkBirthDate(refreshedUser)
            val resolvedBirth = if (clerkReloaded && clerkBirth.isNotBlank()) {
                clerkBirth
            } else {
                latestLocalBirth.ifBlank { clerkBirth }
            }
            val resolvedPhoto = refreshedUser.imageUrl.ifBlank { latestLocalPhoto }

            nameInput = resolvedName
            emailInput = resolvedEmail
            phoneInput = resolvedPhone
            birthInput = resolvedBirth
            isInitializedFromClerk = true

            viewModel.updateAuthProfile(
                email = resolvedEmail,
                name = resolvedName,
                photoUrl = resolvedPhoto,
                phone = resolvedPhone,
                birthDate = resolvedBirth
            )
        } else if (!isInitializedFromClerk) {
            if (nameInput.isBlank()) nameInput = latestLocalName
            if (emailInput.isBlank()) emailInput = latestLocalEmail
            if (phoneInput.isBlank()) phoneInput = latestLocalPhone
            if (birthInput.isBlank()) birthInput = latestLocalBirth
            isInitializedFromClerk = true
        }
    }

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
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 28.dp)
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_btn),
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                viewModel.updateAuthProfile(
                    email = emailInput.trim().ifBlank { userEmail },
                    name = nameInput.trim().ifBlank { userName },
                    photoUrl = croppedUri.toString(),
                    phone = phoneInput.trim(),
                    birthDate = birthInput.trim()
                )

                scope.launch(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "clerk_avatar_${System.currentTimeMillis()}.jpg")
                    try {
                        context.contentResolver.openInputStream(croppedUri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val user = Clerk.userFlow.value ?: Clerk.user
                        if (user != null && tempFile.exists() && tempFile.length() > 0) {
                            Log.d("ProfileSettingsView", "Uploading profile image to Clerk (${tempFile.length()} bytes)...")
                            val uploadResult = user.setProfileImage(tempFile)
                            Log.d("ProfileSettingsView", "Clerk setProfileImage result: $uploadResult")

                            user.reload()

                            val updatedUser = Clerk.userFlow.value ?: Clerk.user
                            val remoteImageUrl = updatedUser?.imageUrl
                            if (!remoteImageUrl.isNullOrBlank()) {
                                withContext(Dispatchers.Main) {
                                    viewModel.updateAuthProfile(
                                        email = emailInput.trim().ifBlank { userEmail },
                                        name = nameInput.trim().ifBlank { userName },
                                        photoUrl = remoteImageUrl,
                                        phone = phoneInput.trim(),
                                        birthDate = birthInput.trim()
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

    if (showBirthdayPicker) {
        BirthdayWheelPickerSheet(
            initialDate = birthInput,
            onDismiss = { showBirthdayPicker = false },
            onConfirm = { selectedDate ->
                birthInput = selectedDate
                saveMessage = null
                showBirthdayPicker = false
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
                        Color(0xFFFCE7F3),
                        Color(0xFFFDF2F8),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0)
                    ),
                    center = Offset(vaporX, vaporY),
                    radius = 2500f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 64.dp, start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(136.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(10.dp, CircleShape)
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(R.string.profile_change_photo),
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(76.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(48.dp)
                            .shadow(8.dp, CircleShape)
                            .background(NavyDark, CircleShape)
                            .clickable { openCropper() }
                            .testTag("profile_photo_edit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.profile_change_photo),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Name (Editable ONLY via the circular NavyDark pen icon)
                    ProfileFieldCard(
                        label = stringResource(R.string.auth_fullname_label),
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            saveMessage = null
                        },
                        placeholder = stringResource(R.string.profile_name_placeholder),
                        icon = Icons.Default.Person,
                        iconTint = PinkPrimary,
                        iconBg = PinkPrimary.copy(alpha = 0.12f),
                        isEditable = true,
                        isInlineEditing = editingField == ProfileEditableField.NAME,
                        onEditButtonClick = {
                            editingField = if (editingField == ProfileEditableField.NAME) {
                                focusManager.clearFocus()
                                null
                            } else {
                                ProfileEditableField.NAME
                            }
                        },
                        onInlineDone = {
                            focusManager.clearFocus()
                            editingField = null
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        cardTestTag = "profile_name_card",
                        inputTestTag = "profile_name_input",
                        editButtonTestTag = "profile_name_edit_button"
                    )

                    // 2. Birthday (Opens Year/Month/Day Wheel Picker BottomSheet)
                    ProfileFieldCard(
                        label = stringResource(R.string.profile_birthday_label),
                        value = birthInput,
                        onValueChange = {},
                        placeholder = stringResource(R.string.profile_birthday_placeholder),
                        icon = Icons.Default.Cake,
                        iconTint = Color(0xFF8B5CF6),
                        iconBg = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                        isEditable = true,
                        isInlineEditing = false,
                        onCardClick = {
                            focusManager.clearFocus()
                            editingField = null
                            showBirthdayPicker = true
                        },
                        onEditButtonClick = {
                            focusManager.clearFocus()
                            editingField = null
                            showBirthdayPicker = true
                        },
                        cardTestTag = "profile_birthday_card",
                        inputTestTag = "profile_birthday_input",
                        editButtonTestTag = "profile_birthday_picker_button"
                    )

                    // 3. Phone Number (Editable ONLY via the circular NavyDark pen icon)
                    ProfileFieldCard(
                        label = stringResource(R.string.phone_label),
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                            saveMessage = null
                        },
                        placeholder = stringResource(R.string.profile_phone_placeholder),
                        icon = Icons.Default.Phone,
                        iconTint = Color(0xFF10B981),
                        iconBg = Color(0xFF10B981).copy(alpha = 0.12f),
                        isEditable = true,
                        isInlineEditing = editingField == ProfileEditableField.PHONE,
                        onEditButtonClick = {
                            editingField = if (editingField == ProfileEditableField.PHONE) {
                                focusManager.clearFocus()
                                null
                            } else {
                                ProfileEditableField.PHONE
                            }
                        },
                        onInlineDone = {
                            focusManager.clearFocus()
                            editingField = null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        cardTestTag = "profile_phone_card",
                        inputTestTag = "profile_phone_input",
                        editButtonTestTag = "profile_phone_edit_button"
                    )

                    // 4. Email (Strictly non-editable, NO pen icon)
                    ProfileFieldCard(
                        label = stringResource(R.string.email_label),
                        value = emailInput,
                        onValueChange = {},
                        placeholder = stringResource(R.string.profile_email_placeholder),
                        icon = Icons.Default.Email,
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFF3B82F6).copy(alpha = 0.12f),
                        isEditable = false,
                        isInlineEditing = false,
                        cardTestTag = "profile_email_card",
                        inputTestTag = "profile_email_input"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(visible = saveMessage != null) {
                    val statusColor = if (isSaveError) Color(0xFFDC2626) else Color(0xFF059669)
                    val statusBg = if (isSaveError) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
                    val statusBorder = if (isSaveError) Color(0xFFFECACA) else Color(0xFFA7F3D0)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("profile_save_status"),
                        shape = RoundedCornerShape(14.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaveError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = saveMessage.orEmpty(),
                                color = statusColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        editingField = null
                        focusManager.clearFocus()

                        val trimmedName = nameInput.trim()
                        val trimmedBirth = birthInput.trim()
                        val trimmedPhone = phoneInput.trim()
                        val preservedEmail = emailInput.trim().ifBlank { userEmail }

                        isSaving = true
                        saveMessage = null
                        isSaveError = false

                        scope.launch {
                            try {
                                val user = Clerk.userFlow.value ?: Clerk.user
                                if (user != null) {
                                    val nameParts = trimmedName.split(Regex("\\s+")).filter { it.isNotBlank() }
                                    val firstName = nameParts.firstOrNull()
                                    val lastName = if (nameParts.size > 1) {
                                        nameParts.drop(1).joinToString(" ")
                                    } else if (nameParts.size == 1) {
                                        ""
                                    } else {
                                        null
                                    }

                                    val metadataJson = buildJsonObject {
                                        user.unsafeMetadata?.forEach { (key, value) ->
                                            put(key, value)
                                        }
                                        put("phone", JsonPrimitive(trimmedPhone))
                                        put("birthDate", JsonPrimitive(trimmedBirth))
                                    }.toString()

                                    val params = User.UpdateParams(
                                        firstName = firstName,
                                        lastName = lastName,
                                        unsafeMetadata = metadataJson
                                    )

                                    val updateResult = withContext(Dispatchers.IO) {
                                        user.update(params)
                                    }

                                    if (updateResult is ClerkResult.Failure<*>) {
                                        isSaveError = true
                                        saveMessage = extractClerkFailureMessage(context, updateResult)
                                        return@launch
                                    }

                                    withContext(Dispatchers.IO) {
                                        user.reload()
                                    }
                                    val refreshed = Clerk.userFlow.value ?: Clerk.user
                                    val finalName = trimmedName.ifBlank { extractClerkFullName(refreshed).ifBlank { userName } }
                                    val finalEmail = extractClerkEmail(refreshed).ifBlank { preservedEmail }

                                    viewModel.updateAuthProfile(
                                        email = finalEmail,
                                        name = finalName,
                                        photoUrl = refreshed?.imageUrl?.ifBlank { userPhoto } ?: userPhoto,
                                        phone = trimmedPhone,
                                        birthDate = trimmedBirth
                                    )
                                    nameInput = finalName
                                    emailInput = finalEmail
                                    isSaveError = false
                                    saveMessage = context.getString(R.string.profile_saved_success)
                                } else {
                                    val finalName = trimmedName.ifBlank { userName }
                                    viewModel.updateAuthProfile(
                                        email = preservedEmail,
                                        name = finalName,
                                        photoUrl = userPhoto,
                                        phone = trimmedPhone,
                                        birthDate = trimmedBirth
                                    )
                                    nameInput = finalName
                                    isSaveError = false
                                    saveMessage = context.getString(R.string.profile_saved_success)
                                }
                            } catch (e: Exception) {
                                Log.e("ProfileSettingsView", "Failed to update profile", e)
                                isSaveError = true
                                saveMessage = e.message ?: context.getString(R.string.profile_save_failed)
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("profile_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.profile_saving),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save_btn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isEditable: Boolean,
    isInlineEditing: Boolean,
    onCardClick: (() -> Unit)? = null,
    onEditButtonClick: (() -> Unit)? = null,
    onInlineDone: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    cardTestTag: String,
    inputTestTag: String,
    editButtonTestTag: String? = null
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isInlineEditing) {
        if (isInlineEditing) {
            focusRequester.requestFocus()
        }
    }

    val borderColor = if (isInlineEditing) PinkPrimary else Color(0xFFE2E8F0)
    val borderWidth = if (isInlineEditing) 1.5.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) {
                    Modifier.clickable { onCardClick() }
                } else {
                    Modifier
                }
            )
            .testTag(cardTestTag),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isInlineEditing) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = if (isEditable) 8.dp else 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isInlineEditing) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyDark
                        ),
                        cursorBrush = SolidColor(PinkPrimary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = KeyboardActions(
                            onDone = { onInlineDone?.invoke() }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag(inputTestTag)
                    )
                } else {
                    Text(
                        text = value.ifBlank { placeholder },
                        fontSize = 15.sp,
                        fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (value.isNotBlank()) NavyDark else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(inputTestTag)
                    )
                }
            }

            if (isEditable && onEditButtonClick != null) {
                IconButton(
                    onClick = onEditButtonClick,
                    modifier = Modifier
                        .size(48.dp)
                        .then(if (editButtonTestTag != null) Modifier.testTag(editButtonTestTag) else Modifier)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                if (isInlineEditing) PinkPrimary else NavyDark,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInlineEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
