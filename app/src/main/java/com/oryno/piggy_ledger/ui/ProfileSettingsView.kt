package com.oryno.piggy_ledger.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.sendCode
import com.clerk.api.emailaddress.verifyCode
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.createEmailAddress
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var isSaveError by remember { mutableStateOf(false) }

    var showBirthdayPicker by remember { mutableStateOf(false) }
    var pendingEmailVerification by remember { mutableStateOf<EmailAddress?>(null) }
    var emailVerificationCode by remember { mutableStateOf("") }
    var emailVerificationError by remember { mutableStateOf<String?>(null) }
    var isVerifyingEmail by remember { mutableStateOf(false) }

    LaunchedEffect(userName, userPhone, userBirth, userEmail) {
        if (!isSaving) {
            if (nameInput.isBlank() && userName.isNotBlank()) nameInput = userName
            if (birthInput.isBlank() && userBirth.isNotBlank()) birthInput = userBirth
            if (phoneInput.isBlank() && userPhone.isNotBlank()) phoneInput = userPhone
            if (emailInput.isBlank() && userEmail.isNotBlank()) emailInput = userEmail
        }
    }

    // Sync the latest profile fields directly from Clerk on screen open
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val activeUser = Clerk.userFlow.value ?: Clerk.user
                if (activeUser != null) {
                    activeUser.reload()
                }
            } catch (e: Exception) {
                Log.w("ProfileSettingsView", "Clerk reload skipped/failed: ${e.message}")
            }
        }
        val refreshedUser = Clerk.userFlow.value ?: Clerk.user ?: clerkUser
        val latestLocalName = viewModel.authUserName.value
        val latestLocalEmail = viewModel.authUserEmail.value
        val latestLocalPhone = viewModel.authUserPhone.value
        val latestLocalBirth = viewModel.authUserBirthDate.value
        val latestLocalPhoto = viewModel.authUserPhotoUrl.value

        if (refreshedUser != null && !isInitializedFromClerk) {
            val clerkName = extractClerkFullName(refreshedUser).ifBlank { latestLocalName }
            val clerkEmail = extractClerkEmail(refreshedUser).ifBlank { latestLocalEmail }
            val clerkPhone = extractClerkPhone(refreshedUser).ifBlank { latestLocalPhone }
            val clerkBirth = extractClerkBirthDate(refreshedUser).ifBlank { latestLocalBirth }
            val clerkPhoto = refreshedUser.imageUrl.ifBlank { latestLocalPhoto }

            nameInput = clerkName
            emailInput = clerkEmail
            phoneInput = clerkPhone
            birthInput = clerkBirth
            isInitializedFromClerk = true

            viewModel.updateAuthProfile(
                email = clerkEmail,
                name = clerkName,
                photoUrl = clerkPhoto,
                phone = clerkPhone,
                birthDate = clerkBirth
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
        val initialMillis = remember(birthInput) {
            try {
                if (birthInput.isNotBlank()) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(birthInput.trim())?.time
                } else null
            } catch (_: Exception) {
                null
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                            birthInput = formatted
                        }
                        showBirthdayPicker = false
                    }
                ) {
                    Text(stringResource(R.string.save_btn), color = PinkPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) {
                    Text(stringResource(R.string.cancel_btn), color = TextLight)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val pendingEmail = pendingEmailVerification
    if (pendingEmail != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifyingEmail) {
                    pendingEmailVerification = null
                    emailVerificationCode = ""
                    emailVerificationError = null
                }
            },
            containerColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.profile_verify_email_title),
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.profile_verify_email_desc, pendingEmail.emailAddress),
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = emailVerificationCode,
                        onValueChange = { value ->
                            if (value.length <= 6 && value.all { it.isDigit() }) {
                                emailVerificationCode = value
                                emailVerificationError = null
                            }
                        },
                        label = { Text(stringResource(R.string.profile_verify_code_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_email_otp_input")
                    )
                    if (emailVerificationError != null) {
                        Text(
                            text = emailVerificationError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailVerificationCode.length < 6) {
                            emailVerificationError = context.getString(R.string.auth_otp_error_length)
                            return@Button
                        }
                        isVerifyingEmail = true
                        emailVerificationError = null
                        scope.launch {
                            try {
                                val verifyResult = withContext(Dispatchers.IO) {
                                    pendingEmail.verifyCode(emailVerificationCode)
                                }
                                when (verifyResult) {
                                    is ClerkResult.Success -> {
                                        val activeUser = Clerk.userFlow.value ?: Clerk.user
                                        val updatePrimaryResult = if (activeUser != null) {
                                            withContext(Dispatchers.IO) {
                                                activeUser.update(
                                                    User.UpdateParams(primaryEmailAddressId = pendingEmail.id)
                                                )
                                            }
                                        } else null

                                        if (updatePrimaryResult is ClerkResult.Failure<*>) {
                                            emailVerificationError = extractClerkFailureMessage(context, updatePrimaryResult)
                                        } else {
                                            withContext(Dispatchers.IO) { activeUser?.reload() }
                                            val updatedUser = Clerk.userFlow.value ?: Clerk.user
                                            val resolvedEmail = extractClerkEmail(updatedUser).ifBlank { pendingEmail.emailAddress }
                                            emailInput = resolvedEmail
                                            viewModel.updateAuthProfile(
                                                email = resolvedEmail,
                                                name = nameInput.trim().ifBlank { userName },
                                                photoUrl = updatedUser?.imageUrl ?: userPhoto,
                                                phone = phoneInput.trim(),
                                                birthDate = birthInput.trim()
                                            )
                                            pendingEmailVerification = null
                                            emailVerificationCode = ""
                                            isSaveError = false
                                            saveMessage = context.getString(R.string.profile_saved_success)
                                        }
                                    }
                                    is ClerkResult.Failure<*> -> {
                                        emailVerificationError = extractClerkFailureMessage(context, verifyResult)
                                    }
                                }
                            } catch (e: Exception) {
                                emailVerificationError = e.message ?: context.getString(R.string.profile_save_failed)
                            } finally {
                                isVerifyingEmail = false
                            }
                        }
                    },
                    enabled = !isVerifyingEmail,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    modifier = Modifier.testTag("profile_email_verify_confirm")
                ) {
                    if (isVerifyingEmail) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.profile_verify_btn))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isVerifyingEmail) {
                            pendingEmailVerification = null
                            emailVerificationCode = ""
                            emailVerificationError = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.cancel_btn), color = TextLight)
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
                        Color(0xFFFCE7F3),
                        Color(0xFFFDF2F8),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0)
                    ),
                    center = androidx.compose.ui.geometry.Offset(vaporX, vaporY),
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
                .padding(top = 68.dp, start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(144.dp)
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
                                    modifier = Modifier.size(84.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        cardTestTag = "profile_name_card",
                        inputTestTag = "profile_name_input"
                    )

                    ProfileFieldCard(
                        label = stringResource(R.string.profile_birthday_label),
                        value = birthInput,
                        onValueChange = {
                            birthInput = it
                            saveMessage = null
                        },
                        placeholder = stringResource(R.string.profile_birthday_placeholder),
                        icon = Icons.Default.Cake,
                        iconTint = Color(0xFF8B5CF6),
                        iconBg = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        cardTestTag = "profile_birthday_card",
                        inputTestTag = "profile_birthday_input",
                        trailingContent = {
                            IconButton(
                                onClick = { showBirthdayPicker = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("profile_birthday_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = stringResource(R.string.profile_select_birthday),
                                    tint = Color(0xFF8B5CF6)
                                )
                            }
                        }
                    )

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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        cardTestTag = "profile_phone_card",
                        inputTestTag = "profile_phone_input"
                    )

                    ProfileFieldCard(
                        label = stringResource(R.string.email_label),
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            saveMessage = null
                        },
                        placeholder = stringResource(R.string.profile_email_placeholder),
                        icon = Icons.Default.Email,
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFF3B82F6).copy(alpha = 0.12f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
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
                        val trimmedName = nameInput.trim()
                        val trimmedBirth = birthInput.trim()
                        val trimmedPhone = phoneInput.trim()
                        val trimmedEmail = emailInput.trim()

                        if (trimmedEmail.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                            isSaveError = true
                            saveMessage = context.getString(R.string.profile_invalid_email)
                            return@Button
                        }

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

                                    val currentPrimaryEmail = extractClerkEmail(user)
                                    var emailRequiresVerification = false
                                    if (trimmedEmail.isNotBlank() && !trimmedEmail.equals(currentPrimaryEmail, ignoreCase = true)) {
                                        val existingEmail = user.emailAddresses?.firstOrNull {
                                            it.emailAddress.equals(trimmedEmail, ignoreCase = true)
                                        }
                                        if (existingEmail != null) {
                                            if (existingEmail.verification?.status == Verification.Status.VERIFIED) {
                                                val primaryRes = withContext(Dispatchers.IO) {
                                                    user.update(User.UpdateParams(primaryEmailAddressId = existingEmail.id))
                                                }
                                                if (primaryRes is ClerkResult.Failure<*>) {
                                                    isSaveError = true
                                                    saveMessage = extractClerkFailureMessage(context, primaryRes)
                                                    return@launch
                                                }
                                            } else {
                                                val sendRes = withContext(Dispatchers.IO) { existingEmail.sendCode() }
                                                when (sendRes) {
                                                    is ClerkResult.Success -> {
                                                        pendingEmailVerification = sendRes.value
                                                        emailRequiresVerification = true
                                                    }
                                                    is ClerkResult.Failure<*> -> {
                                                        isSaveError = true
                                                        saveMessage = extractClerkFailureMessage(context, sendRes)
                                                        return@launch
                                                    }
                                                }
                                            }
                                        } else {
                                            val createEmailRes = withContext(Dispatchers.IO) {
                                                user.createEmailAddress(trimmedEmail)
                                            }
                                            when (createEmailRes) {
                                                is ClerkResult.Success -> {
                                                    val created = createEmailRes.value
                                                    if (created.verification?.status == Verification.Status.VERIFIED) {
                                                        val primaryRes = withContext(Dispatchers.IO) {
                                                            user.update(User.UpdateParams(primaryEmailAddressId = created.id))
                                                        }
                                                        if (primaryRes is ClerkResult.Failure<*>) {
                                                            isSaveError = true
                                                            saveMessage = extractClerkFailureMessage(context, primaryRes)
                                                            return@launch
                                                        }
                                                    } else {
                                                        val sendRes = withContext(Dispatchers.IO) { created.sendCode() }
                                                        when (sendRes) {
                                                            is ClerkResult.Success -> {
                                                                pendingEmailVerification = sendRes.value
                                                                emailRequiresVerification = true
                                                            }
                                                            is ClerkResult.Failure<*> -> {
                                                                isSaveError = true
                                                                saveMessage = extractClerkFailureMessage(context, sendRes)
                                                                return@launch
                                                            }
                                                        }
                                                    }
                                                }
                                                is ClerkResult.Failure<*> -> {
                                                    isSaveError = true
                                                    saveMessage = extractClerkFailureMessage(context, createEmailRes)
                                                    return@launch
                                                }
                                            }
                                        }
                                    }

                                    withContext(Dispatchers.IO) {
                                        user.reload()
                                    }
                                    val refreshed = Clerk.userFlow.value ?: Clerk.user
                                    val finalEmail = if (emailRequiresVerification) {
                                        extractClerkEmail(refreshed).ifBlank { userEmail }
                                    } else {
                                        trimmedEmail.ifBlank { extractClerkEmail(refreshed).ifBlank { userEmail } }
                                    }
                                    val finalName = trimmedName.ifBlank { extractClerkFullName(refreshed).ifBlank { userName } }
                                    viewModel.updateAuthProfile(
                                        email = finalEmail,
                                        name = finalName,
                                        photoUrl = refreshed?.imageUrl?.ifBlank { userPhoto } ?: userPhoto,
                                        phone = trimmedPhone,
                                        birthDate = trimmedBirth
                                    )
                                    if (!emailRequiresVerification) {
                                        isSaveError = false
                                        saveMessage = context.getString(R.string.profile_saved_success)
                                    }
                                } else {
                                    viewModel.updateAuthProfile(
                                        email = trimmedEmail.ifBlank { userEmail },
                                        name = trimmedName.ifBlank { userName },
                                        photoUrl = userPhoto,
                                        phone = trimmedPhone,
                                        birthDate = trimmedBirth
                                    )
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    cardTestTag: String,
    inputTestTag: String,
    trailingContent: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) PinkPrimary else Color(0xFFE2E8F0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(cardTestTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        border = BorderStroke(if (isFocused) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextLight,
                    modifier = Modifier.padding(start = 4.dp)
                )
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyDark
                    ),
                    keyboardOptions = keyboardOptions,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = PinkPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag(inputTestTag)
                )
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
