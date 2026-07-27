with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

start_marker = "} else if (pageIndex in 7..9) {"
end_marker = "} else if (pageIndex == 10) {"

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx != -1 and end_idx != -1:
    new_page_block = """} else if (pageIndex in 7..9) {
                    val cardBgColor = when (pageIndex) {
                        7 -> Color(0xFFB38952) // Warm Amber / Burnt Ochre
                        8 -> Color(0xFF5B78A7) // Slate Indigo / Periwinkle
                        else -> Color(0xFF386851) // Deep Sage / Forest Green
                    }
                    val relatableText = when (pageIndex) {
                        7 -> stringResource(R.string.onboarding_relatable_statement_1)
                        8 -> stringResource(R.string.onboarding_relatable_statement_2)
                        else -> stringResource(R.string.onboarding_relatable_statement_3)
                    }
                    val imageRes = when (pageIndex) {
                        7 -> R.drawable.img_relatable_debt_1785176852844
                        8 -> R.drawable.img_relatable_accounts_1785176864908
                        else -> R.drawable.img_relatable_emergency_1785176876312
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Absolute Top: Navigation & Progress Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { currentPage-- },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.back_btn),
                                        tint = NavyDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Progress Step Dashes
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val currentStep = pageIndex - 6 // 1, 2, 3
                                    val totalSteps = 5
                                    for (step in 1..totalSteps) {
                                        val isFilled = step <= currentStep + 1
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isFilled) Color(0xFFE5A641) else Color(0xFFE2E8F0))
                                        )
                                    }
                                }
                            }
                        }

                        // Center Area: Header + Card (Scrollable if screen height is constrained)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Header Title
                            Text(
                                text = stringResource(R.string.onboarding_relatable_header),
                                color = NavyDark,
                                fontSize = if (isSmallScreen) 22.sp else 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 28.sp else 32.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Statement Card - 95% Width
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(cardBgColor)
                                    .padding(if (isSmallScreen) 16.dp else 20.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "“",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = if (isSmallScreen) 48.sp else 56.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 32.sp,
                                        modifier = Modifier
                                            .align(Alignment.Start)
                                            .offset(y = (-6).dp)
                                    )

                                    Text(
                                        text = relatableText,
                                        color = Color.White,
                                        fontSize = if (isSmallScreen) 15.sp else 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        lineHeight = if (isSmallScreen) 22.sp else 26.sp,
                                        modifier = Modifier.padding(bottom = 16.dp, start = 4.dp, end = 4.dp)
                                    )

                                    // Custom Generated Illustration container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isSmallScreen) 130.dp else 170.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = imageRes),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Absolute Bottom: Choice Buttons ("No" and "Yes") matching card color & 95% width
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(bottom = 12.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = {
                                    when (pageIndex) {
                                        7 -> relatesToLoans = false
                                        8 -> relatesToAccounts = false
                                        9 -> relatesToEmergency = false
                                    }
                                    if (currentPage < pages.size - 1) currentPage++
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardBgColor,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.no_label),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    when (pageIndex) {
                                        7 -> {
                                            relatesToLoans = true
                                            selectedIntent = 1
                                        }
                                        8 -> relatesToAccounts = true
                                        9 -> relatesToEmergency = true
                                    }
                                    if (currentPage < pages.size - 1) currentPage++
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardBgColor,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.yes_label),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
"""
    content = content[:start_idx] + new_page_block + content[end_idx:]
    print("Successfully replaced layout block!")
else:
    print("ERROR: Markers not found!")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)

