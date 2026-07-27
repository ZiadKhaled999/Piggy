with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("else if (pageIndex == 8) {", "else if (pageIndex == 11) {")
content = content.replace("else if (pageIndex == 7) {", "else if (pageIndex == 10) {")

# I need to insert the handler for 7, 8, 9 right after pageIndex == 6.
handler = """
                } else if (pageIndex in 7..9) {
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
                    }
"""

content = content.replace("} else if (pageIndex == 10) {", handler + "} else if (pageIndex == 10) {")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
