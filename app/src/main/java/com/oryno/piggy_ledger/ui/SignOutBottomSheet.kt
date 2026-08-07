package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignOutBottomSheet(
    viewModel: PiggyLedgerViewModel,
    onDismiss: () -> Unit,
    onSignOutSuccess: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val logoutState by viewModel.logoutState.collectAsState()

    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            viewModel.resetLogoutState()
            onSignOutSuccess()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (logoutState !is LogoutState.Syncing) {
                viewModel.resetLogoutState()
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            if (logoutState !is LogoutState.Syncing) {
                BottomSheetDefaults.DragHandle(color = Color(0xFF1E293B).copy(alpha = 0.2f))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = logoutState) {
                is LogoutState.Idle -> {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)), // Red-100
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = Color(0xFFDC2626), // Red-600
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Title
                    Text(
                        text = stringResource(R.string.logout_sheet_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )

                    // Subtitle with highlighted bold red string
                    val subtitleText = buildAnnotatedString {
                        append(stringResource(R.string.logout_sheet_subtitle_prefix))
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(R.string.logout_sheet_subtitle_highlight))
                        }
                        append(stringResource(R.string.logout_sheet_subtitle_suffix))
                    }

                    Text(
                        text = subtitleText,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Button(
                        onClick = {
                            viewModel.performSyncAndLogout(forceDeleteIfOffline = false, onSuccess = onSignOutSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button_yes),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetLogoutState()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button_stay),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                is LogoutState.Syncing -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpressiveLoadingIndicator(size = 44.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.logout_syncing_msg),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                is LogoutState.OfflineError -> {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)), // Amber-100
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFD97706), // Amber-600
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Title
                    Text(
                        text = stringResource(R.string.logout_offline_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )

                    // Subtitle with highlighted warning
                    val offlineText = buildAnnotatedString {
                        append(stringResource(R.string.logout_offline_subtitle, state.unsyncedCount))
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(R.string.logout_offline_highlight))
                        }
                        append(".")
                    }

                    Text(
                        text = offlineText,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Button(
                        onClick = {
                            viewModel.performSyncAndLogout(forceDeleteIfOffline = true, onSuccess = onSignOutSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button_delete_anyway),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetLogoutState()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button_stay),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                is LogoutState.Error -> {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.logout_sync_failed),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = state.message,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.performSyncAndLogout(forceDeleteIfOffline = false, onSuccess = onSignOutSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB), // Blue primary
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.logout_retry),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.performSyncAndLogout(forceDeleteIfOffline = true, onSuccess = onSignOutSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button_delete_anyway),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.resetLogoutState()
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button_stay),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                is LogoutState.Success -> {
                    // Handled in LaunchedEffect
                }
            }
        }
    }
}
