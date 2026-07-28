with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace Crossfade start to handle pageIndex == 4
old_crossfade_start = """            Crossfade(
                targetState = currentPage,
                label = "onboarding_page_fade"
            ) { pageIndex ->
                if (pageIndex == 5) {"""

new_crossfade_start = """            Crossfade(
                targetState = currentPage,
                label = "onboarding_page_fade"
            ) { pageIndex ->
                if (pageIndex == 4) {
                    // NOTIFICATION PERMISSION SLIDE - Cloned phone mockup + card theme
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 12.dp))

                        // Phone Frame with Floating Notification Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (isSmallScreen) 0.84f else 0.88f)
                                .height(if (isSmallScreen) 210.dp else 250.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Phone Clock
                                Text(
                                    text = "9:41",
                                    fontSize = if (isSmallScreen) 34.sp else 40.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(top = if (isSmallScreen) 12.dp else 16.dp)
                                )

                                Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 14.dp))

                                // Floating Notification Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(if (isSmallScreen) 12.dp else 14.dp)
                                    ) {
                                        // Header Row: App Icon, App Name, Time
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PinkPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_piggy_hello),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = stringResource(R.string.piggy_ledger_brand).uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextLight,
                                                letterSpacing = 0.5.sp
                                            )

                                            Spacer(modifier = Modifier.weight(1f))

                                            Text(
                                                text = "9:41 AM",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Notification Title
                                        Text(
                                            text = stringResource(R.string.onboarding_notif_card_title),
                                            fontSize = if (isSmallScreen) 13.sp else 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // Notification Body
                                        Text(
                                            text = stringResource(R.string.onboarding_notif_card_body),
                                            fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                            color = TextLight,
                                            lineHeight = if (isSmallScreen) 15.sp else 17.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Main Title
                        Text(
                            text = stringResource(R.string.onboarding_notif_title),
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
                            text = stringResource(R.string.onboarding_notif_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Enable Notifications Button
                        Button(
                            onClick = { requestNotificationPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp)
                                .testTag("grant_notif_permission_button"),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_notif_btn),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Skip Button
                        TextButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.onboarding_notif_denied), Toast.LENGTH_SHORT).show()
                                currentPage++
                            },
                            modifier = Modifier.testTag("skip_notif_permission_button")
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_notif_skip),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (pageIndex == 5) {"""

if old_crossfade_start in content:
    content = content.replace(old_crossfade_start, new_crossfade_start, 1)
    print("Successfully added pageIndex == 4 notification permission slide!")
else:
    print("WARNING: Crossfade start target not found!")

# 2. Remove old if (pageIndex == 4) block from the default else block
old_if_page4 = """                        if (pageIndex == 4) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { requestNotificationPermissions() },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(50.dp)
                                    .testTag("grant_notif_permission_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_notif_btn),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, context.getString(R.string.onboarding_notif_denied), Toast.LENGTH_SHORT).show()
                                    currentPage++
                                },
                                modifier = Modifier.testTag("skip_notif_permission_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_notif_skip),
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }"""

if old_if_page4 in content:
    content = content.replace(old_if_page4, "")
    print("Successfully removed old pageIndex == 4 block from else branch!")
else:
    print("WARNING: Old page 4 block in else branch not found!")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
