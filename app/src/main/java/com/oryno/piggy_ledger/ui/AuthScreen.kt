package com.oryno.piggy_ledger.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
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
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendEmailCode
import com.clerk.api.signup.update
import com.clerk.api.signup.verifyCode
import com.oryno.piggy_ledger.R
import com.posthog.PostHog
import kotlinx.coroutines.launch

enum class AuthRoute {
    WELCOME,
    SIGN_IN,
    SU_EMAIL,
    SU_OTP,
    SU_AVATAR,
    SU_NAME,
    SU_PREVIEW
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
    var siPassword by remember { mutableStateOf("") }

    var suEmail by remember { mutableStateOf("") }
    var suOtp by remember { mutableStateOf("") }
    
    var suAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var suAvatarScale by remember { mutableFloatStateOf(1f) }
    var suAvatarOffset by remember { mutableStateOf(Offset.Zero) }

    var suName by remember { mutableStateOf("") }
    var suPassword by remember { mutableStateOf("") }

    var activeSignUp by remember { mutableStateOf<SignUp?>(null) }
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
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("End-to-End Encrypted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("Piggy Ledger", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                            Text("Secure, simple, and smart.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                        }

                        Image(
                            painter = painterResource(id = R.drawable.auth_illustration_1783784211319),
                            contentDescription = "Welcome",
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Track your expenses, set goals, and save money effortlessly.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Button(
                                onClick = { showActionSheet = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                AuthRoute.SIGN_IN -> {
                    // Sign In Screen
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.WELCOME) }) { Icon(Icons.Default.ArrowBack, "Back") }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sign In", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = siEmail,
                            onValueChange = { siEmail = it; errorMessage = null },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        var pwdVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = siPassword,
                            onValueChange = { siPassword = it; errorMessage = null },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { pwdVisible = !pwdVisible }) {
                                    Icon(if (pwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (siEmail.isBlank() || siPassword.isBlank()) {
                                    errorMessage = "Warning: One of the two fields are wrong"
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    try {
                                        Clerk.auth.signInWithPassword {
                                            identifier = siEmail
                                            password = siPassword
                                        }.onSuccess { res ->
                                            val sid = res.createdSessionId
                                            if (!sid.isNullOrBlank()) {
                                                Clerk.auth.setActive(sid)
                                            } else {
                                                isLoading = false
                                                errorMessage = "Warning: One of the two fields are wrong"
                                            }
                                        }.onFailure {
                                            isLoading = false
                                            errorMessage = "Warning: One of the two fields are wrong"
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = "An error occurred"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            else Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AuthRoute.SU_EMAIL -> {
                    // Sign Up: Email & Password
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.WELCOME) }) { Icon(Icons.Default.ArrowBack, null) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Enter your email and a strong password", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(48.dp))

                        OutlinedTextField(
                            value = suEmail,
                            onValueChange = { suEmail = it; errorMessage = null },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var pwdVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = suPassword,
                            onValueChange = { suPassword = it; errorMessage = null },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { pwdVisible = !pwdVisible }) {
                                    Icon(if (pwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                        }

                        Spacer(modifier = Modifier.height(64.dp))
                        Button(
                            onClick = {
                                if (suEmail.isBlank() || suPassword.isBlank()) {
                                    errorMessage = "Please enter both email and password"
                                    return@Button
                                }
                                isCompletingProfile = true
                                isLoading = true
                                scope.launch {
                                    Clerk.auth.signUp {
                                        email = suEmail
                                        password = suPassword
                                    }.onSuccess { su ->
                                        activeSignUp = su
                                        if (su.status == SignUp.Status.COMPLETE) {
                                            val sid = su.createdSessionId
                                            if (!sid.isNullOrBlank()) {
                                                Clerk.auth.setActive(sid)
                                            }
                                            isLoading = false
                                        } else {
                                            su.sendEmailCode()
                                                .onSuccess {
                                                    isLoading = false
                                                    navigateTo(AuthRoute.SU_OTP)
                                                }
                                                .onFailure { err ->
                                                    isLoading = false
                                                    errorMessage = "Failed to send OTP: $err"
                                                }
                                        }
                                    }.onFailure { err ->
                                        isLoading = false
                                        errorMessage = "Failed to start sign up: $err"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            else Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AuthRoute.SU_OTP -> {
                    // Sign Up: OTP
                    var resendTimer by remember { mutableIntStateOf(30) }
                    
                    LaunchedEffect(Unit) {
                        while (resendTimer > 0) {
                            delay(1000)
                            resendTimer--
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.SU_EMAIL) }) { Icon(Icons.Default.ArrowBack, null) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Verify your email", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("We sent an OTP to $suEmail", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(48.dp))

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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(
                                                    width = if (isFocused) 2.dp else 1.dp,
                                                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = char, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        )

                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        TextButton(
                            onClick = {
                                if (resendTimer == 0) {
                                    isLoading = true
                                    scope.launch {
                                        activeSignUp?.sendEmailCode()
                                            ?.onSuccess {
                                                isLoading = false
                                                resendTimer = 30
                                                errorMessage = null
                                            }?.onFailure {
                                                isLoading = false
                                                errorMessage = "Failed to resend code"
                                            }
                                    }
                                }
                            },
                            enabled = resendTimer == 0 && !isLoading
                        ) {
                            Text(
                                text = if (resendTimer > 0) "Resend code in ${resendTimer}s" else "Resend code",
                                fontWeight = FontWeight.SemiBold,
                                color = if (resendTimer > 0) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                if (suOtp.length < 6) {
                                    errorMessage = "Please enter the full 6-digit code"
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
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
                                            errorMessage = "Invalid OTP"
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            else Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AuthRoute.SU_AVATAR -> {
                    // Sign Up: Avatar & Crop
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        if (uri != null) { suAvatarUri = uri; suAvatarScale = 1f; suAvatarOffset = Offset.Zero }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.SU_EMAIL) }) { Icon(Icons.Default.ArrowBack, null) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Upload Image Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Choose, crop, and zoom your image", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(32.dp))

                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                    contentDescription = "Avatar",
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
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add", modifier = Modifier.size(48.dp), tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(onClick = { launcher.launch("image/*") }) {
                            Text("Choose Image")
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { navigateTo(AuthRoute.SU_NAME) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AuthRoute.SU_NAME -> {
                    // Sign Up: Name
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.SU_AVATAR) }) { Icon(Icons.Default.ArrowBack, null) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Add your name", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = suName,
                            onValueChange = { suName = it; errorMessage = null },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                        }

                        Spacer(modifier = Modifier.height(64.dp))
                        Button(
                            onClick = {
                                if (suName.isBlank()) {
                                    errorMessage = "Please enter your name"
                                    return@Button
                                }
                                navigateTo(AuthRoute.SU_PREVIEW)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AuthRoute.SU_PREVIEW -> {
                    // Sign Up: Preview
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            IconButton(onClick = { navigateTo(AuthRoute.SU_NAME) }) { Icon(Icons.Default.ArrowBack, null) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Review Details", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("If anything's wrong, edit now.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(32.dp))

                        // Preview Avatar
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (suAvatarUri != null) {
                                    AsyncImage(
                                        model = suAvatarUri,
                                        contentDescription = "Avatar",
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
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                }
                            }
                            IconButton(
                                onClick = { navigateTo(AuthRoute.SU_AVATAR) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(x = 40.dp, y = 10.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Preview Name
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Name", fontSize = 12.sp, color = Color.Gray)
                                    Text(suName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { navigateTo(AuthRoute.SU_NAME) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview Email
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Email", fontSize = 12.sp, color = Color.Gray)
                                    Text(suEmail, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { navigateTo(AuthRoute.SU_EMAIL) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Email", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val parts = suName.trim().split(" ")
                                        val first = parts.firstOrNull() ?: suName
                                        val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                                        
                                        activeSignUp?.update {
                                            firstName = first
                                            lastName = last
                                        }?.onSuccess { finalSu ->
                                            if (finalSu.status == SignUp.Status.COMPLETE) {
                                                val sid = finalSu.createdSessionId
                                                if (!sid.isNullOrBlank()) {
                                                    Clerk.auth.setActive(sid)
                                                }
                                            }
                                            isLoading = false
                                            isCompletingProfile = false
                                        }?.onFailure { err ->
                                            isLoading = false
                                            errorMessage = "Failed to update name: $err"
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = "An error occurred"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(100.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) ExpressiveLoadingIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            else Text("Finish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Bottom Sheet for Get Started
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Welcome to Piggy Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showActionSheet = false
                            navigateTo(AuthRoute.SIGN_IN)
                        },
                        modifier = Modifier.weight(2f).height(54.dp),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            showActionSheet = false
                            navigateTo(AuthRoute.SU_EMAIL)
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(100.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Sign Up", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
