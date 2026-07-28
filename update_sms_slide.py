with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add Person icon import if missing
if "import androidx.compose.material.icons.filled.Person" not in content:
    content = content.replace(
        "import androidx.compose.material.icons.filled.Check",
        "import androidx.compose.material.icons.filled.Check\nimport androidx.compose.material.icons.filled.Person"
    )

# 2. Add SmsSkeletonRow composable at the end of the file or above OnboardingScreen
skeleton_code = """
@Composable
private fun SmsSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE2E8F0))
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}
"""

if "@Composable\nprivate fun SmsSkeletonRow" not in content:
    content += "\n" + skeleton_code

# 3. Add pageIndex == 3 before pageIndex == 4 in Crossfade
old_crossfade_start = """            Crossfade(
                targetState = currentPage,
                label = "onboarding_page_fade"
            ) { pageIndex ->
                if (pageIndex == 4) {"""

new_crossfade_start = """            Crossfade(
                targetState = currentPage,
                label = "onboarding_page_fade"
            ) { pageIndex ->
                if (pageIndex == 3) {
                    // SMS / MESSAGES PERMISSION SLIDE - Phone Mockup matching reference design
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 12.dp))

                        // Phone Frame with Messages Interface
                        Box(
                            modifier = Modifier
                                .width(if (isSmallScreen) 250.dp else 290.dp)
                                .height(if (isSmallScreen) 270.dp else 310.dp)
                                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.5.dp,
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Top Navigation Chevron
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // App Title ("Messages" / "الرسائل")
                                Text(
                                    text = stringResource(R.string.onboarding_messages_header),
                                    fontSize = if (isSmallScreen) 22.sp else 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                                // Row 1: Detailed Bank SMS item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF94A3B8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.onboarding_sms_mock_sender),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NavyDark
                                            )
                                            Text(
                                                text = "9:41 AM",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = stringResource(R.string.onboarding_sms_mock_body),
                                            fontSize = 11.sp,
                                            color = TextLight,
                                            lineHeight = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Row 2: Skeleton Placeholder 1
                                SmsSkeletonRow()

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Row 3: Skeleton Placeholder 2
                                SmsSkeletonRow()

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Row 4: Skeleton Placeholder 3
                                SmsSkeletonRow()
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Main Title
                        Text(
                            text = stringResource(R.string.onboarding_sms_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 28.sp else 34.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtitle
                        Text(
                            text = stringResource(R.string.onboarding_sms_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Enable SMS Button
                        Button(
                            onClick = { requestSmsPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp)
                                .testTag("grant_sms_permission_button"),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_sms_btn),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Skip Button
                        TextButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.onboarding_sms_denied), Toast.LENGTH_SHORT).show()
                                currentPage++
                            },
                            modifier = Modifier.testTag("skip_sms_permission_button")
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_sms_skip),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (pageIndex == 4) {"""

if old_crossfade_start in content:
    content = content.replace(old_crossfade_start, new_crossfade_start, 1)
    print("Successfully added pageIndex == 3 to Crossfade!")
else:
    print("ERROR: Crossfade start pattern not found!")

# 4. Remove old pageIndex == 3 block from default else
old_page3_block = """                        if (pageIndex == 3) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { requestSmsPermissions() },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(50.dp)
                                    .testTag("grant_sms_permission_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_sms_btn),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, context.getString(R.string.onboarding_sms_denied), Toast.LENGTH_SHORT).show()
                                    currentPage++
                                },
                                modifier = Modifier.testTag("skip_sms_permission_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_sms_skip),
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }"""

if old_page3_block in content:
    content = content.replace(old_page3_block, "")
    print("Successfully removed old pageIndex == 3 block!")
else:
    print("WARNING: Old pageIndex == 3 block not found!")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
