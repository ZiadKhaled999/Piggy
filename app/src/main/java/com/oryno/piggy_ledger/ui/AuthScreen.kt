package com.oryno.piggy_ledger.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.clerk.api.Clerk
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.sendCode
import com.clerk.api.signin.verifyCode
import com.clerk.api.signin.verifyWithPassword
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendEmailCode
import com.clerk.api.signup.verifyCode
import com.oryno.piggy_ledger.R
import com.posthog.PostHog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthRoute {
    WELCOME,
    SIGN_IN,
    SU_EMAIL,
    SU_OTP,
    SU_AVATAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: PiggyLedgerViewModel,
    onAuthSuccess: () -> Unit
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentRoute by remember { mutableStateOf(AuthRoute.WELCOME) }
    var showActionSheet by remember { mutableStateOf(false) }

    // Form State
    var siEmail by remember { mutableStateOf("") }

    var suEmail by remember { mutableStateOf("") }
    var suOtp by remember { mutableStateOf("") }
    
    var suAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var suAvatarScale by remember { mutableFloatStateOf(1f) }
    var suAvatarOffset by remember { mutableStateOf(Offset.Zero) }

    var suName by remember { mutableStateOf("") }
    var suPassword by remember { mutableStateOf("") }

    var activeSignUp by remember { mutableStateOf<SignUp?>(null) }
    var activeSignIn by remember { mutableStateOf<SignIn?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCompletingProfile by rememberSaveable { mutableStateOf(false) }

    // Auto-login if session exists
    LaunchedEffect(user, isCompletingProfile) {
        if (user != null && !isCompletingProfile) {
            val email = user?.primaryEmailAddress?.emailAddress ?: ""
            val name = listOfNotNull(user?.firstName, user?.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { user?.firstName ?: "" }
            val photoUrl = user?.imageUrl ?: suAvatarUri?.toString() ?: ""
            viewModel.setAuthUser(email, name, photoUrl)
            viewModel.triggerCloudSync()
            PostHog.capture(event = "user_sign_in", properties = mapOf("method" to "clerk", "user_id" to email))
            onAuthSuccess()
        }
    }

    // Helper for navigation
    fun navigateTo(route: AuthRoute) {
        errorMessage = null
        currentRoute = route
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith 
                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith 
                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                }
            },
            label = "AuthNavigation"
        ) { route ->
            when (route) {
                AuthRoute.WELCOME -> {
                    // Welcome screen with Get Started button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.auth_e2e_encrypted),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.auth_welcome_title),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.auth_welcome_tagline),
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.auth_illustration_1783784211319),
                            contentDescription = stringResource(R.string.auth_welcome_title),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.widthIn(max = 480.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.auth_welcome_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                            Button(
                                onClick = { showActionSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_get_started),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                AuthRoute.SIGN_IN -> {
                    // Sign In Screen (Passwordless Email OTP)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.WELCOME) }) {
                                Icon(Icons.Default.ArrowBack, stringResource(R.string.auth_back))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.auth_signin_title),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.auth_signin_subtitle),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        // Email Field with Leading Icon
                        OutlinedTextField(
                            value = siEmail,
                            onValueChange = { siEmail = it; errorMessage = null },
                            label = { Text(stringResource(R.string.auth_email_label)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                val trimmedEmail = siEmail.trim()
                                if (trimmedEmail.isBlank()) {
                                    errorMessage = context.getString(R.string.auth_error_enter_email)
                                    return@Button
                                }
                                isLoading = true
                                isCompletingProfile = false
                                scope.launch {
                                    try {
                                        suEmail = trimmedEmail
                                        Clerk.auth.signIn {
                                            email = trimmedEmail
                                        }.onSuccess { res ->
                                            activeSignIn = res
                                            activeSignUp = null
                                            val sid = res.createdSessionId
                                            if (!sid.isNullOrBlank()) {
                                                Clerk.auth.setActive(sid).onFailure { setActiveErr ->
                                                    isLoading = false
                                                    errorMessage = getClerkErrorMessage(setActiveErr)
                                                }
                                            } else {
                                                // Prepare & send email OTP code
                                                res.sendCode {
                                                    email = trimmedEmail
                                                }.onSuccess { preparedSi ->
                                                    activeSignIn = preparedSi
                                                    isLoading = false
                                                    navigateTo(AuthRoute.SU_OTP)
                                                }.onFailure { sendErr ->
                                                    isLoading = false
                                                    errorMessage = context.getString(R.string.auth_error_send_otp_failed, getClerkErrorMessage(sendErr))
                                                }
                                            }
                                        }.onFailure { signInErr ->
                                            isLoading = false
                                            val errMessage = getClerkErrorMessage(signInErr)
                                            if (errMessage.contains("identifier is invalid", ignoreCase = true) ||
                                                errMessage.contains("not found", ignoreCase = true)) {
                                                errorMessage = context.getString(R.string.auth_account_not_found)
                                            } else {
                                                errorMessage = context.getString(R.string.auth_error_signin_failed, errMessage)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = context.getString(R.string.auth_error_signin_failed, e.message ?: "")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.auth_signin_send_code), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch to Sign Up
                        TextButton(
                            onClick = {
                                suEmail = siEmail
                                navigateTo(AuthRoute.SU_EMAIL)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.auth_dont_have_account),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AuthRoute.SU_EMAIL -> {
                    // Sign Up: Name, Email & Password
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.WELCOME) }) {
                                Icon(Icons.Default.ArrowBack, stringResource(R.string.auth_back))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.auth_signup_title),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.auth_signup_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        // Full Name Field with Person Icon
                        OutlinedTextField(
                            value = suName,
                            onValueChange = { suName = it; errorMessage = null },
                            label = { Text(stringResource(R.string.auth_fullname_label)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email Field with Alternate Email Icon
                        OutlinedTextField(
                            value = suEmail,
                            onValueChange = { suEmail = it; errorMessage = null },
                            label = { Text(stringResource(R.string.auth_email_address_label)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Password Field with Lock Icon & Dynamic Strength Indicator
                        var pwdVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = suPassword,
                            onValueChange = { suPassword = it; errorMessage = null },
                            label = { Text(stringResource(R.string.auth_password_label)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { pwdVisible = !pwdVisible }) {
                                    Icon(
                                        imageVector = if (pwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        AnimatedVisibility(visible = suPassword.isNotEmpty()) {
                            val strength = calculatePasswordStrength(suPassword)
                            val strengthColor = when (strength) {
                                1 -> Color(0xFFF43F5E) // Red
                                2 -> Color(0xFFF59E0B) // Amber
                                3 -> Color(0xFF3B82F6) // Blue
                                4 -> Color(0xFF10B981) // Green
                                else -> Color.Transparent
                            }
                            val strengthText = when (strength) {
                                1 -> stringResource(R.string.auth_strength_weak)
                                2 -> stringResource(R.string.auth_strength_fair)
                                3 -> stringResource(R.string.auth_strength_good)
                                4 -> stringResource(R.string.auth_strength_strong)
                                else -> ""
                            }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, start = 4.dp, end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        for (i in 1..4) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        if (strength >= i) strengthColor else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = strengthText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = strengthColor,
                                        modifier = Modifier.wrapContentWidth(),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = {
                                if (suName.isBlank() || suEmail.isBlank() || suPassword.isBlank()) {
                                    errorMessage = context.getString(R.string.auth_error_enter_all_fields)
                                    return@Button
                                }
                                isCompletingProfile = true
                                isLoading = true
                                scope.launch {
                                    try {
                                        val parts = suName.trim().split(" ")
                                        val first = parts.firstOrNull() ?: suName.trim()
                                        val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

                                        Clerk.auth.signUp {
                                            email = suEmail.trim()
                                            password = suPassword
                                            firstName = first
                                            if (last.isNotBlank()) {
                                                lastName = last
                                            }
                                        }.onSuccess { su ->
                                            activeSignUp = su
                                            if (su.status == SignUp.Status.COMPLETE) {
                                                val sid = su.createdSessionId
                                                if (!sid.isNullOrBlank()) {
                                                    Clerk.auth.setActive(sid)
                                                }
                                                isLoading = false
                                                navigateTo(AuthRoute.SU_AVATAR)
                                            } else {
                                                su.sendEmailCode()
                                                    .onSuccess {
                                                        isLoading = false
                                                        navigateTo(AuthRoute.SU_OTP)
                                                    }
                                                    .onFailure { err ->
                                                        isLoading = false
                                                        errorMessage = context.getString(R.string.auth_error_send_otp_failed, getClerkErrorMessage(err))
                                                    }
                                            }
                                        }.onFailure { err ->
                                            isLoading = false
                                            errorMessage = context.getString(R.string.auth_error_signup_failed, getClerkErrorMessage(err))
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = context.getString(R.string.auth_error_signup_failed, e.message ?: "")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.auth_btn_next), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch to Sign In
                        TextButton(
                            onClick = {
                                siEmail = suEmail
                                navigateTo(AuthRoute.SIGN_IN)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.auth_already_have_account),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AuthRoute.SU_OTP -> {
                    // OTP Verification Screen
                    var resendTimer by remember { mutableIntStateOf(30) }
                    
                    LaunchedEffect(Unit) {
                        while (resendTimer > 0) {
                            delay(1000)
                            resendTimer--
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = {
                                if (activeSignUp != null) navigateTo(AuthRoute.SU_EMAIL)
                                else navigateTo(AuthRoute.SIGN_IN)
                            }) {
                                Icon(Icons.Default.ArrowBack, stringResource(R.string.auth_back))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Header Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.auth_otp_title),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.auth_otp_subtitle, suEmail),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(40.dp))

                        // Styled 6-Digit OTP Field
                        BasicTextField(
                            value = suOtp,
                            onValueChange = { 
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    suOtp = it
                                    errorMessage = null
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(6) { index ->
                                        val char = when {
                                            index < suOtp.length -> suOtp[index].toString()
                                            else -> ""
                                        }
                                        val isFocused = index == suOtp.length
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.85f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .border(
                                                    width = if (isFocused) 2.dp else 1.dp,
                                                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Resend Button with Timer
                        TextButton(
                            onClick = {
                                if (resendTimer == 0) {
                                    isLoading = true
                                    scope.launch {
                                        if (activeSignUp != null) {
                                            activeSignUp?.sendEmailCode()
                                                ?.onSuccess {
                                                    isLoading = false
                                                    resendTimer = 30
                                                    errorMessage = null
                                                }?.onFailure {
                                                    isLoading = false
                                                    errorMessage = context.getString(R.string.auth_otp_error_resend, getClerkErrorMessage(it))
                                                }
                                        } else if (activeSignIn != null) {
                                            val targetEmail = suEmail.trim().ifBlank { siEmail.trim() }.ifBlank { activeSignIn?.identifier ?: "" }
                                            activeSignIn?.sendCode {
                                                email = targetEmail
                                            }
                                                ?.onSuccess {
                                                    isLoading = false
                                                    resendTimer = 30
                                                    errorMessage = null
                                                }?.onFailure {
                                                    isLoading = false
                                                    errorMessage = context.getString(R.string.auth_otp_error_resend, getClerkErrorMessage(it))
                                                }
                                        }
                                    }
                                }
                            },
                            enabled = resendTimer == 0 && !isLoading
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (resendTimer > 0) Icons.Default.HourglassEmpty else Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (resendTimer > 0) stringResource(R.string.auth_otp_resend_timer, resendTimer) else stringResource(R.string.auth_otp_resend),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (resendTimer > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = {
                                if (suOtp.length < 6) {
                                    errorMessage = context.getString(R.string.auth_otp_error_length)
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    if (activeSignUp != null) {
                                        activeSignUp?.verifyCode(suOtp, VerificationType.EMAIL)
                                            ?.onSuccess { su ->
                                                activeSignUp = su
                                                if (su.status == SignUp.Status.COMPLETE) {
                                                    val sid = su.createdSessionId
                                                    if (!sid.isNullOrBlank()) {
                                                        Clerk.auth.setActive(sid)
                                                    }
                                                }
                                                isLoading = false
                                                navigateTo(AuthRoute.SU_AVATAR)
                                            }?.onFailure {
                                                isLoading = false
                                                errorMessage = context.getString(R.string.auth_otp_error_invalid, getClerkErrorMessage(it))
                                            }
                                    } else if (activeSignIn != null) {
                                        activeSignIn?.verifyCode(suOtp)
                                            ?.onSuccess { si ->
                                                activeSignIn = si
                                                val sid = si.createdSessionId
                                                if (!sid.isNullOrBlank()) {
                                                    Clerk.auth.setActive(sid).onFailure { setActiveErr ->
                                                        isLoading = false
                                                        errorMessage = getClerkErrorMessage(setActiveErr)
                                                    }
                                                } else {
                                                    isLoading = false
                                                    errorMessage = context.getString(R.string.auth_error_signin_incomplete)
                                                }
                                            }?.onFailure {
                                                isLoading = false
                                                errorMessage = context.getString(R.string.auth_otp_error_invalid, getClerkErrorMessage(it))
                                            }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.auth_btn_next), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AuthRoute.SU_AVATAR -> {
                    // Sign Up: Avatar & Crop
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        if (uri != null) {
                            suAvatarUri = uri
                            suAvatarScale = 1f
                            suAvatarOffset = Offset.Zero
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.WELCOME) }) {
                                Icon(Icons.Default.ArrowBack, stringResource(R.string.auth_back))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.auth_avatar_title),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.auth_avatar_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .clickable { launcher.launch("image/*") }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        suAvatarScale = (suAvatarScale * zoom).coerceIn(1f, 5f)
                                        suAvatarOffset += pan
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (suAvatarUri != null) {
                                AsyncImage(
                                    model = suAvatarUri,
                                    contentDescription = stringResource(R.string.auth_avatar_title),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = suAvatarScale,
                                            scaleY = suAvatarScale,
                                            translationX = suAvatarOffset.x,
                                            translationY = suAvatarOffset.y
                                        )
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = stringResource(R.string.auth_avatar_choose),
                                        modifier = Modifier.size(52.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_avatar_choose),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(if (suAvatarUri != null) R.string.auth_avatar_change else R.string.auth_avatar_choose))
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                isCompletingProfile = false
                                val email = user?.primaryEmailAddress?.emailAddress ?: suEmail.trim()
                                val name = listOfNotNull(user?.firstName, user?.lastName)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                    .ifBlank { suName.trim().ifBlank { "User" } }
                                val photoUrl = suAvatarUri?.toString() ?: user?.imageUrl ?: ""
                                viewModel.setAuthUser(email, name, photoUrl)
                                viewModel.triggerCloudSync()
                                PostHog.capture(event = "user_sign_in", properties = mapOf("method" to "clerk", "user_id" to email))
                                onAuthSuccess()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.auth_avatar_finish), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                isLoading = true
                                isCompletingProfile = false
                                val email = user?.primaryEmailAddress?.emailAddress ?: suEmail.trim()
                                val name = listOfNotNull(user?.firstName, user?.lastName)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                    .ifBlank { suName.trim().ifBlank { "User" } }
                                val photoUrl = user?.imageUrl ?: ""
                                viewModel.setAuthUser(email, name, photoUrl)
                                viewModel.triggerCloudSync()
                                PostHog.capture(event = "user_sign_in", properties = mapOf("method" to "clerk", "user_id" to email))
                                onAuthSuccess()
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                stringResource(R.string.auth_avatar_skip),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Responsive Bottom Sheet for Get Started
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.auth_sheet_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 2/3 Component: Create Account (Sign Up) - Elegant Solid Black with high-contrast White text
                        Surface(
                            onClick = {
                                showActionSheet = false
                                navigateTo(AuthRoute.SU_EMAIL)
                            },
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF111827), // Deep sleek black
                            contentColor = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_sign_up_btn),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 1/3 Component: Sign In (Log In) - Crisp Clean White with subtle border and bold dark typography
                        Surface(
                            onClick = {
                                showActionSheet = false
                                navigateTo(AuthRoute.SIGN_IN)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            contentColor = Color(0xFF111827),
                            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_sign_in_btn),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    color = Color(0xFF111827),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun calculatePasswordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var strength = 0
    if (password.length >= 8) strength++
    if (password.any { it.isLowerCase() }) strength++
    if (password.any { it.isUpperCase() }) strength++
    if (password.any { it.isDigit() }) strength++
    if (password.any { !it.isLetterOrDigit() }) strength++
    return minOf(4, strength)
}

private fun getClerkErrorMessage(err: Any?): String {
    if (err is ClerkResult.Failure<*>) {
        val errorResponse = err.error as? ClerkErrorResponse
        val clerkError = errorResponse?.errors?.firstOrNull()
        return clerkError?.longMessage ?: clerkError?.message ?: err.throwable?.message ?: "Authentication error"
    }
    return err?.toString() ?: "Authentication error"
}
