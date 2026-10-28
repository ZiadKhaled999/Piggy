package com.oryno.piggy_ledger.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.BlackAccent
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkAccent
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanBottomSheet(
    appCurrency: String,
    onConfirm: (
        isLent: Boolean,
        amount: Double,
        contactName: String,
        phone: String?,
        email: String?,
        photoUri: String?,
        social: String?,
        note: String,
        deadline: Long?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form State
    var isLent by remember { mutableStateOf(true) }
    var amountStr by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var contactPhotoUri by remember { mutableStateOf<String?>(null) }
    var socialDetails by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf<Long?>(null) }

    // Step Tracking (1: Amount/Type, 2: Contact, 3: Memo/Deadline, 4: Review/Confirm)
    var currentStep by remember { mutableIntStateOf(1) }
    var isSuccess by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val currencySymbol = getCurrencySymbol(appCurrency)
    val activeColor = if (isLent) PinkAccent else BlackAccent
    val activeBgColor = if (isLent) PinkAccent.copy(alpha = 0.06f) else BlackAccent.copy(alpha = 0.06f)

    // Auto-dismiss and trigger onConfirm after success screen
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(1700)
            onConfirm(
                isLent,
                amount,
                contactName,
                phoneNumber.ifBlank { null },
                emailAddress.ifBlank { null },
                contactPhotoUri,
                socialDetails.ifBlank { null },
                note,
                deadlineDate
            )
            onDismiss()
        }
    }

    // Contact Picker Launcher & Permission
    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasContactPermission = isGranted
    }
    val contactPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null && hasContactPermission) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        contactName = cursor.getString(nameIndex) ?: ""
                    }
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val photoUriIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                    if (photoUriIndex != -1) {
                        contactPhotoUri = cursor.getString(photoUriIndex)
                    }

                    if (idIndex != -1 && hasPhoneIndex != -1) {
                        val id = cursor.getString(idIndex)
                        val hasPhone = cursor.getInt(hasPhoneIndex) > 0

                        if (hasPhone) {
                            val pCur = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            if (pCur != null && pCur.moveToFirst()) {
                                val phoneIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex != -1) {
                                    phoneNumber = pCur.getString(phoneIndex) ?: ""
                                }
                                pCur.close()
                            }
                        }

                        val emailCur = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        if (emailCur != null && emailCur.moveToFirst()) {
                            val emailIndex = emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                            if (emailIndex != -1) {
                                emailAddress = emailCur.getString(emailIndex) ?: ""
                            }
                            emailCur.close()
                        }
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isSuccess) {
                onConfirm(
                    isLent,
                    amount,
                    contactName,
                    phoneNumber.ifBlank { null },
                    emailAddress.ifBlank { null },
                    contactPhotoUri,
                    socialDetails.ifBlank { null },
                    note,
                    deadlineDate
                )
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = if (isSuccess) Color(0xFF10B981) else Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AnimatedContent(
            targetState = isSuccess,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "SuccessTransition"
        ) { targetSuccess ->
            if (targetSuccess) {
                // Success View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 500.dp)
                        .background(Color(0xFF10B981))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onConfirm(
                                isLent,
                                amount,
                                contactName,
                                phoneNumber.ifBlank { null },
                                emailAddress.ifBlank { null },
                                contactPhotoUri,
                                socialDetails.ifBlank { null },
                                note,
                                deadlineDate
                            )
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val prefix = if (isLent) "+" else "-"
                        Text(
                            text = "$prefix $currencySymbol ${String.format(Locale.US, "%,.2f", amount)}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isLent) stringResource(R.string.lent_to_person, contactName) else stringResource(R.string.borrowed_from_person, contactName),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center
                        )

                        if (note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "“$note”",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.weight(1.2f))

                        Text(
                            text = stringResource(R.string.tap_anywhere_to_close),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Multi-Step Form Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Drag Pill Indicator
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step Indicator Bar & Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentStep--
                                },
                                shape = CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = NavyDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }

                        // Step Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..4).forEach { stepIndex ->
                                val isSelected = currentStep == stepIndex
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(if (isSelected) 24.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) activeColor else Color(0xFFCBD5E1))
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.step_count_format, currentStep, 4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Content for Steps
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn(tween(220))) togetherWith
                                        (slideOutHorizontally { width -> -width } + fadeOut(tween(180)))
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn(tween(220))) togetherWith
                                        (slideOutHorizontally { width -> width } + fadeOut(tween(180)))
                            }
                        },
                        label = "StepContentTransition"
                    ) { step ->
                        when (step) {
                            1 -> StepOneAmountAndType(
                                isLent = isLent,
                                amountStr = amountStr,
                                currencySymbol = currencySymbol,
                                activeColor = activeColor,
                                onTypeChanged = { isLent = it },
                                onDigitClick = { digit ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (amountStr.length < 9) {
                                        if (amountStr == "0") {
                                            amountStr = digit.toString()
                                        } else {
                                            val parts = amountStr.split(".")
                                            if (parts.size == 2 && parts[1].length >= 2) {
                                                // Max 2 decimal digits
                                            } else {
                                                amountStr += digit
                                            }
                                        }
                                    }
                                },
                                onDecimalClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (!amountStr.contains(".")) {
                                        amountStr = if (amountStr.isEmpty()) "0." else "$amountStr."
                                    }
                                },
                                onBackspaceClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (amountStr.isNotEmpty()) {
                                        amountStr = amountStr.dropLast(1)
                                    }
                                },
                                onNext = {
                                    if (amount > 0.0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentStep = 2
                                    } else {
                                        ToastUtil.show(context, context.getString(R.string.enter_amount_first), Toast.LENGTH_SHORT)
                                    }
                                }
                            )

                            2 -> StepTwoContactDetails(
                                contactName = contactName,
                                phoneNumber = phoneNumber,
                                emailAddress = emailAddress,
                                socialDetails = socialDetails,
                                contactPhotoUri = contactPhotoUri,
                                activeColor = activeColor,
                                onContactNameChange = { contactName = it },
                                onPhoneNumberChange = { phoneNumber = it },
                                onEmailAddressChange = { emailAddress = it },
                                onSocialDetailsChange = { socialDetails = it },
                                onImportContactsClick = {
                                    if (hasContactPermission) {
                                        contactPickerLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                onNext = {
                                    if (contactName.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentStep = 3
                                    } else {
                                        ToastUtil.show(context, "Please enter a contact name", Toast.LENGTH_SHORT)
                                    }
                                }
                            )

                            3 -> StepThreeMemoAndDeadline(
                                note = note,
                                deadlineDate = deadlineDate,
                                activeColor = activeColor,
                                onNoteChange = { if (it.length <= 60) note = it },
                                onDeadlineChange = { deadlineDate = it },
                                onNext = {
                                    if (note.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentStep = 4
                                    } else {
                                        ToastUtil.show(context, "Please enter a flashback note", Toast.LENGTH_SHORT)
                                    }
                                }
                            )

                            4 -> StepFourReviewAndConfirm(
                                isLent = isLent,
                                amount = amount,
                                currencySymbol = currencySymbol,
                                contactName = contactName,
                                phoneNumber = phoneNumber,
                                emailAddress = emailAddress,
                                contactPhotoUri = contactPhotoUri,
                                note = note,
                                deadlineDate = deadlineDate,
                                activeColor = activeColor,
                                onConfirmed = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isSuccess = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * STEP 1: Select Type (Lent/Borrowed) + Hero Amount + Custom Keypad
 */
@Composable
private fun StepOneAmountAndType(
    isLent: Boolean,
    amountStr: String,
    currencySymbol: String,
    activeColor: Color,
    onTypeChanged: (Boolean) -> Unit,
    onDigitClick: (Int) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onNext: () -> Unit
) {
    val currentAmount = amountStr.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLent) Color.White else Color.Transparent)
                    .border(
                        if (isLent) BorderStroke(1.5.dp, PinkAccent) else BorderStroke(0.dp, Color.Transparent),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTypeChanged(true) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (isLent) PinkAccent else TextLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.i_lent),
                        fontWeight = if (isLent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLent) PinkAccent else TextLight,
                        fontSize = 14.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isLent) Color.White else Color.Transparent)
                    .border(
                        if (!isLent) BorderStroke(1.5.dp, BlackAccent) else BorderStroke(0.dp, Color.Transparent),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTypeChanged(false) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (!isLent) BlackAccent else TextLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.i_borrowed),
                        fontWeight = if (!isLent) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isLent) BlackAccent else TextLight,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Amount Display
        val displayAmount = if (amountStr.isEmpty()) "0" else amountStr
        val amountColor = if (currentAmount > 0.0) activeColor else NavyDark

        Text(
            text = "$currencySymbol $displayAmount",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Keypad
        CustomNumericKeypad(
            onDigitClick = onDigitClick,
            onDecimalClick = onDecimalClick,
            onBackspaceClick = onBackspaceClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Next Button
        Button(
            onClick = onNext,
            enabled = currentAmount > 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColor,
                disabledContainerColor = Color(0xFFE2E8F0)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.btn_next_contact),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentAmount > 0.0) Color.White else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (currentAmount > 0.0) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * STEP 2: Contact Person Details (Import + Manual Inputs)
 */
@Composable
private fun StepTwoContactDetails(
    contactName: String,
    phoneNumber: String,
    emailAddress: String,
    socialDetails: String,
    contactPhotoUri: String?,
    activeColor: Color,
    onContactNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailAddressChange: (String) -> Unit,
    onSocialDetailsChange: (String) -> Unit,
    onImportContactsClick: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Import Button Banner
        Surface(
            onClick = onImportContactsClick,
            color = activeColor.copy(alpha = 0.08f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!contactPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(contactPhotoUri),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, activeColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(activeColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (contactName.isNotBlank()) contactName else stringResource(R.string.import_from_contacts),
                        color = NavyDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (contactName.isNotBlank()) stringResource(R.string.contact_imported_success_desc) else stringResource(R.string.tap_to_autofill_contact),
                        color = TextLight,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Contact Name Input
        OutlinedTextField(
            value = contactName,
            onValueChange = onContactNameChange,
            label = { Text(stringResource(R.string.contact_name_label)) },
            placeholder = { Text(stringResource(R.string.mike_smith_placeholder)) },
            textStyle = TextStyle(fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = activeColor,
                focusedLabelColor = activeColor,
                unfocusedLabelColor = TextLight,
                cursorColor = activeColor
            )
        )

        // Phone & Email Inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text(stringResource(R.string.phone_optional)) },
                placeholder = { Text(stringResource(R.string.phone_placeholder)) },
                textStyle = TextStyle(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 13.sp),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = activeColor,
                    focusedLabelColor = activeColor,
                    unfocusedLabelColor = TextLight,
                    cursorColor = activeColor
                )
            )

            OutlinedTextField(
                value = emailAddress,
                onValueChange = onEmailAddressChange,
                label = { Text(stringResource(R.string.email_optional)) },
                placeholder = { Text(stringResource(R.string.email_placeholder)) },
                textStyle = TextStyle(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 13.sp),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = activeColor,
                    focusedLabelColor = activeColor,
                    unfocusedLabelColor = TextLight,
                    cursorColor = activeColor
                )
            )
        }

        // Social Details
        OutlinedTextField(
            value = socialDetails,
            onValueChange = onSocialDetailsChange,
            label = { Text(stringResource(R.string.social_optional)) },
            placeholder = { Text(stringResource(R.string.social_placeholder)) },
            textStyle = TextStyle(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = activeColor,
                focusedLabelColor = activeColor,
                unfocusedLabelColor = TextLight,
                cursorColor = activeColor
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Next Button
        Button(
            onClick = onNext,
            enabled = contactName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColor,
                disabledContainerColor = Color(0xFFE2E8F0)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.btn_next_memo),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (contactName.isNotBlank()) Color.White else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (contactName.isNotBlank()) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * STEP 3: Flashback Note & Repayment Deadline
 */
@Composable
private fun StepThreeMemoAndDeadline(
    note: String,
    deadlineDate: Long?,
    activeColor: Color,
    onNoteChange: (String) -> Unit,
    onDeadlineChange: (Long?) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    val showDeadlinePicker = {
        val calendar = Calendar.getInstance()
        deadlineDate?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                onDeadlineChange(selectedCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Flashback Note Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.memo_flashback_header),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextLight,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text(stringResource(R.string.flashback_placeholder), color = Color(0xFF94A3B8)) },
                textStyle = TextStyle(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = activeColor,
                    cursorColor = activeColor
                )
            )
        }

        // Deadline Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.repayment_deadline_header),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextLight,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Date Selection Card
            Surface(
                onClick = { showDeadlinePicker() },
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (deadlineDate != null) activeColor else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (deadlineDate != null) activeColor.copy(alpha = 0.15f) else Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (deadlineDate != null) activeColor else TextLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (deadlineDate != null) {
                                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    sdf.format(Date(deadlineDate))
                                } else stringResource(R.string.no_strict_deadline),
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (deadlineDate != null) stringResource(R.string.tap_to_change_due_date) else stringResource(R.string.tap_to_set_due_date),
                                color = TextLight,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Checkbox(
                        checked = deadlineDate != null,
                        onCheckedChange = { checked ->
                            if (checked) showDeadlinePicker() else onDeadlineChange(null)
                        },
                        colors = CheckboxDefaults.colors(checkedColor = activeColor)
                    )
                }
            }

            // Quick Preset Chips
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetDateChip(stringResource(R.string.preset_1_week), activeColor, modifier = Modifier.weight(1f)) {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                    onDeadlineChange(cal.timeInMillis)
                }
                PresetDateChip(stringResource(R.string.preset_1_month), activeColor, modifier = Modifier.weight(1f)) {
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                    onDeadlineChange(cal.timeInMillis)
                }
                PresetDateChip(stringResource(R.string.preset_3_months), activeColor, modifier = Modifier.weight(1f)) {
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }
                    onDeadlineChange(cal.timeInMillis)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Next Button
        Button(
            onClick = onNext,
            enabled = note.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColor,
                disabledContainerColor = Color(0xFFE2E8F0)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.btn_next_review),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (note.isNotBlank()) Color.White else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (note.isNotBlank()) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PresetDateChip(
    label: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NavyDark
            )
        }
    }
}

/**
 * STEP 4: Review Entry Summary + Swipe Right to Confirm Bar
 */
@Composable
private fun StepFourReviewAndConfirm(
    isLent: Boolean,
    amount: Double,
    currencySymbol: String,
    contactName: String,
    phoneNumber: String,
    emailAddress: String,
    contactPhotoUri: String?,
    note: String,
    deadlineDate: Long?,
    activeColor: Color,
    onConfirmed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Receipt Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.05f)),
            border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type & Amount Badge
                Surface(
                    color = activeColor,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isLent) stringResource(R.string.badge_i_lent) else stringResource(R.string.badge_i_borrowed),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "$currencySymbol ${String.format(Locale.US, "%,.2f", amount)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = activeColor
                )

                HorizontalDivider(color = activeColor.copy(alpha = 0.15f), thickness = 1.dp)

                // Contact Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!contactPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(contactPhotoUri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, activeColor, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(activeColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        val subInfo = phoneNumber.ifBlank { emailAddress }
                        if (subInfo.isNotBlank()) {
                            Text(
                                text = subInfo,
                                fontSize = 12.sp,
                                color = TextLight
                            )
                        }
                    }
                }

                // Note & Deadline Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.memo_summary_format, note),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NavyDark
                    )

                    val deadlineText = if (deadlineDate != null) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        stringResource(R.string.due_summary_format, sdf.format(Date(deadlineDate)))
                    } else {
                        stringResource(R.string.open_ended_no_deadline)
                    }
                    Text(
                        text = deadlineText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Swipe Right to Confirm Bar
        SwipeRightToConfirmBar(
            isEnabled = true,
            activeColor = activeColor,
            onConfirmed = onConfirmed
        )
    }
}

/**
 * Reusable "Swipe right to confirm" track with animated thumb sliding and fill color.
 */
@Composable
private fun SwipeRightToConfirmBar(
    isEnabled: Boolean,
    activeColor: Color = Color(0xFF10B981),
    onConfirmed: () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        val density = LocalDensity.current
        var trackWidthPx by remember { mutableFloatStateOf(0f) }
        val thumbSizeDp = 48.dp
        val thumbSizePx = with(density) { thumbSizeDp.toPx() }
        val maxDragPx = (trackWidthPx - thumbSizePx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)

        val dragOffset = remember { androidx.compose.animation.core.Animatable(0f) }
        val scope = rememberCoroutineScope()

        val progress = if (maxDragPx > 0f) (dragOffset.value / maxDragPx).coerceIn(0f, 1f) else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF1F5F9))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(28.dp))
                .onSizeChanged { size ->
                    trackWidthPx = size.width.toFloat()
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Active Fill Bar behind thumb as it slides
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (dragOffset.value + thumbSizePx + with(density) { 4.dp.toPx() }).toDp() })
                    .clip(RoundedCornerShape(28.dp))
                    .background(activeColor.copy(alpha = 0.18f + (progress * 0.52f)))
            )

            // Center Guide Text
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.swipe_right_to_confirm),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) Color(0xFF475569) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            // Draggable Arrow Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.value.roundToInt() + with(density) { 4.dp.roundToPx() }, 0) }
                    .size(thumbSizeDp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(if (progress > 0.8f) activeColor else Color.White)
                    .pointerInput(maxDragPx, isEnabled) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (maxDragPx > 0f && dragOffset.value >= maxDragPx * 0.65f) {
                                        dragOffset.animateTo(
                                            targetValue = maxDragPx,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        onConfirmed()
                                        delay(200)
                                        dragOffset.snapTo(0f)
                                    } else {
                                        dragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                                    dragOffset.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe to confirm",
                    tint = if (progress > 0.8f) Color.White else activeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Ultra-smooth, rounded capsule numeric keypad matching modern fintech designs.
 */
@Composable
private fun CustomNumericKeypad(
    onDigitClick: (Int) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00F8FAFC),
                        Color(0xFFF8FAFC).copy(alpha = 0.5f)
                    )
                )
            )
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(text = "1", modifier = Modifier.weight(1f)) { onDigitClick(1) }
            SmoothKeypadPillButton(text = "2", modifier = Modifier.weight(1f)) { onDigitClick(2) }
            SmoothKeypadPillButton(text = "3", modifier = Modifier.weight(1f)) { onDigitClick(3) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(text = "4", modifier = Modifier.weight(1f)) { onDigitClick(4) }
            SmoothKeypadPillButton(text = "5", modifier = Modifier.weight(1f)) { onDigitClick(5) }
            SmoothKeypadPillButton(text = "6", modifier = Modifier.weight(1f)) { onDigitClick(6) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(text = "7", modifier = Modifier.weight(1f)) { onDigitClick(7) }
            SmoothKeypadPillButton(text = "8", modifier = Modifier.weight(1f)) { onDigitClick(8) }
            SmoothKeypadPillButton(text = "9", modifier = Modifier.weight(1f)) { onDigitClick(9) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(
                text = ".",
                modifier = Modifier.weight(1f),
                isDot = true
            ) { onDecimalClick() }
            SmoothKeypadPillButton(text = "0", modifier = Modifier.weight(1f)) { onDigitClick(0) }
            SmoothKeypadIconButton(
                modifier = Modifier.weight(1f),
                onClick = onBackspaceClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun SmoothKeypadPillButton(
    text: String,
    modifier: Modifier = Modifier,
    isDot: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "KeypadScale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFDFE4EA) else Color(0xFFEFF2F6),
        animationSpec = tween(100),
        label = "KeypadBgColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(27.dp),
        color = buttonBgColor,
        contentColor = Color(0xFF0F172A),
        interactionSource = interactionSource,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (isDot) 28.sp else 24.sp,
                fontWeight = if (isDot) FontWeight.Bold else FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmoothKeypadIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "KeypadIconScale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFDFE4EA) else Color(0xFFEFF2F6),
        animationSpec = tween(100),
        label = "KeypadIconBgColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(27.dp),
        color = buttonBgColor,
        contentColor = Color(0xFF0F172A),
        interactionSource = interactionSource,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
