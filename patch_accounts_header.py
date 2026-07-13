import re

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AccountsScreen.kt", "r") as f:
    content = f.read()

header_pattern = r"// PREMIUM GRADIENT HEADER.*?// TRANSACTIONS HEADER"
header_replacement = """// MODERN SLEEK HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(NavyDark) // Sleek Navy background
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Top Bar Actions inside Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Month Selector Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                    .clickable { showMonthBottomSheet = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${monthNames[selectedMonth]} $selectedYear",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Eye Toggle & Account Swapper dedicated circle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { isBalanceVisible = !isBalanceVisible }
                                ) {
                                    Icon(
                                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Balance",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }

                                // SWITCHER DEDICATED CIRCLE
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                        .clickable { showAccountSwitcher = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedAccount == null) {
                                        // All accounts stack representation
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = "All Accounts",
                                            tint = PinkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        // Single account representation
                                        val providerRes = getProviderDrawableRes(selectedAccount.provider)
                                        val logoSource: Any? = when {
                                            selectedAccount.local_logo_path != null -> File(selectedAccount.local_logo_path)
                                            selectedAccount.logo_url != null -> selectedAccount.logo_url
                                            else -> null
                                        }

                                        if (logoSource != null) {
                                            AsyncImage(
                                                model = logoSource,
                                                contentDescription = selectedAccount.provider ?: "Logo",
                                                modifier = Modifier.fillMaxSize().padding(6.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (providerRes != null) {
                                            Image(
                                                painter = painterResource(id = providerRes),
                                                contentDescription = selectedAccount.provider ?: "Logo",
                                                modifier = Modifier.fillMaxSize().padding(6.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            val initial = selectedAccount.name.take(1).uppercase()
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(android.graphics.Color.parseColor(selectedAccount.icon_color))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initial,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Beautiful Budget Card inside the Header
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SPENT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isBalanceVisible) "$currencySymbol ${String.format("%,.2f", totalSpent)}" else "$currencySymbol ••••••",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(30.dp)
                                            .background(Color.White.copy(alpha = 0.1f))
                                    )

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "INCOME",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isBalanceVisible) "$currencySymbol ${String.format("%,.2f", totalIncome)}" else "$currencySymbol ••••••",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                // Progress Bar and Budget String
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val progressColor = if (progress > 0.9f) Color(0xFFEF4444) else PinkPrimary
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = progressColor,
                                        trackColor = Color.White.copy(alpha = 0.05f),
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { showEditBudgetDialog = true },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val spentFormatted = if (isBalanceVisible) String.format("%,.0f", totalSpent) else "••••"
                                        val budgetFormatted = String.format("%,.0f", monthlyBudget)
                                        Text(
                                            text = "$currencySymbol $spentFormatted of $currencySymbol $budgetFormatted budget",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextLight
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Budget",
                                            tint = TextLight,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Savings Net Pill
                        val netSign = if (netSavings >= 0) "+" else ""
                        val pillBg = if (netSavings >= 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        val pillTextColor = if (netSavings >= 0) Color(0xFF34D399) else Color(0xFFF87171)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillBg)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "THIS MONTH NET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillTextColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isBalanceVisible) "$netSign$currencySymbol ${String.format("%,.0f", netSavings)}" else "$netSign$currencySymbol ••••",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = pillTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // TRANSACTIONS HEADER"""

content = re.sub(header_pattern, header_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AccountsScreen.kt", "w") as f:
    f.write(content)
