with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

target_block = """                } else if (pageIndex in 7..9) {
                    val backgroundColor = when (pageIndex) {
                        7 -> Color(0xFF332D29) // Deep Charcoal / Burnt Amber
                        8 -> Color(0xFF2C3E50) // Muted Navy / Indigo
                        else -> Color(0xFF1E392A) // Deep Emerald / Forest Green
                    }
                    val relatableText = when (pageIndex) {
                        7 -> stringResource(R.string.onboarding_relatable_statement_1)
                        8 -> stringResource(R.string.onboarding_relatable_statement_2)
                        else -> stringResource(R.string.onboarding_relatable_statement_3)
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A1A)) // Dark background outside the card
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_relatable_header),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(backgroundColor)
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Quote Icon Placeholder (You can use an actual icon if available)
                                Text(
                                    text = "“",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 80.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.align(Alignment.Start).offset(y = (-20).dp)
                                )
                                
                                Text(
                                    text = relatableText,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                                
                                // Placeholder image (User will upload later)
                                Image(
                                    painter = painterResource(id = R.drawable.img_piggy_hello),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF55CBA1), // Green button color
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = stringResource(R.string.no_label), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    when (pageIndex) {
                                        7 -> {
                                            relatesToLoans = true
                                            selectedIntent = 1 // Set intent to Loans as a side effect
                                        }
                                        8 -> relatesToAccounts = true
                                        9 -> relatesToEmergency = true
                                    }
                                    if (currentPage < pages.size - 1) currentPage++
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF55CBA1), // Green button color
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = stringResource(R.string.yes_label), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }"""

replacement_block = """                } else if (pageIndex in 7..9) {
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

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Top Navigation Bar with Back Circle & Dash Progress
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Header Title
                            Text(
                                text = stringResource(R.string.onboarding_relatable_header),
                                color = NavyDark,
                                fontSize = if (isSmallScreen) 22.sp else 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 28.sp else 34.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Statement Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(cardBgColor)
                                    .padding(20.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "“",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 60.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 36.sp,
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

                                    // Placeholder image container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isSmallScreen) 150.dp else 190.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_piggy_hello),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Choice Buttons ("No" and "Yes")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
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
                                    containerColor = Color(0xFF63E6BE),
                                    contentColor = NavyDark
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.no_label),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
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
                                    containerColor = Color(0xFF63E6BE),
                                    contentColor = NavyDark
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.yes_label),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                            }
                        }
                    }"""

if target_block in content:
    content = content.replace(target_block, replacement_block)
    print("Successfully replaced relatable block!")
else:
    print("ERROR: Target block not found!")

# Also fix lower back button
content = content.replace("if (currentPage > 0) {", "if (currentPage > 0 && currentPage !in 7..9) {")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)

