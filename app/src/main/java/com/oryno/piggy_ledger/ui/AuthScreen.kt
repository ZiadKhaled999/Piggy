package com.oryno.piggy_ledger.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.sso.OAuthProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.oryno.piggy_ledger.BuildConfig
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.posthog.PostHog
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: PiggyLedgerViewModel,
    onAuthSuccess: () -> Unit
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null) {
            val email = user?.primaryEmailAddress?.emailAddress ?: ""
            val name = listOfNotNull(user?.firstName, user?.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { user?.firstName ?: "" }
            val photoUrl = user?.imageUrl ?: ""
            viewModel.signInWithGoogle(email, name, photoUrl)
            viewModel.triggerCloudSync()
            PostHog.capture(event = "user_sign_in", properties = mapOf("method" to "google", "user_id" to email))
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (user == null && !isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with welcome text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_welcome_title_main),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.auth_welcome_subtitle_main),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Middle section with the image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.auth_illustration_1783784211319),
                        contentDescription = "Piggy Ledger Auth",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(32.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Bottom section with the Continue with Google button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.auth_welcome_desc),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                                if (webClientId.isNotBlank() && webClientId != "GOOGLE_WEB_CLIENT_ID_DEFAULT_VALUE") {
                                    try {
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setServerClientId(webClientId)
                                            .setFilterByAuthorizedAccounts(false)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        val credentialManager = CredentialManager.create(context)
                                        val result = credentialManager.getCredential(context, request)
                                        val credential = result.credential

                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            val email = googleIdTokenCredential.id
                                            val displayName = googleIdTokenCredential.displayName ?: email
                                            val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                                            viewModel.signInWithGoogle(email, displayName, photoUrl)
                                            viewModel.triggerCloudSync()
                                            PostHog.capture(event = "user_sign_in", properties = mapOf("method" to "google_credential_manager", "user_id" to email))
                                            isLoading = false
                                            onAuthSuccess()
                                            return@launch
                                        }
                                    } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                                        isLoading = false
                                        return@launch
                                    } catch (e: Exception) {
                                        isLoading = false
                                        val errorMsg = e.message ?: e.toString()
                                        Log.e("AuthScreen", "CredentialManager Sign-In failed", e)
                                        android.widget.Toast.makeText(context, "Google Auth Error: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                        return@launch
                                    }
                                }

                                // Fallback or direct Clerk OAuth flow (only if native client ID is not configured)
                                Clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE)
                                    .onSuccess {
                                        // User flow will trigger LaunchedEffect
                                    }
                                    .onFailure { error ->
                                        isLoading = false
                                        val errorMsg = error?.toString() ?: "Unknown Clerk Error"
                                        android.widget.Toast.makeText(context, "Clerk Auth Error: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF747775))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                tint = Color.Unspecified
                            )
                            Text(
                                text = stringResource(R.string.auth_continue_google),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            ExpressiveLoadingIndicator(size = 40.dp)
        }
    }
}

