import re

# Patch DashboardScreen.kt to enhance UI/UX and profile handling
with open("app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt", "r") as f:
    content = f.read()

# 1. Update Profile Header to only show image
content = content.replace(
    """                        } else if (!user?.firstName.isNullOrBlank() || authUserName.isNotBlank()) {
                            val initial = user?.firstName?.take(1) ?: authUserName.take(1)
                            Text(
                                text = initial.uppercase(),
                                color = NavyDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {""",
    """                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = NavyDark,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {"""
)
# Wait, the above logic is a bit wrong. Let's fix it properly.

# Let's just redefine the Profile Box
profile_box_pattern = r"""                    Box\(\s+modifier = Modifier\s+\.size\(52\.dp\)\s+\.clip\(CircleShape\)\s+\.background\(Color\.White\)\s+\.border\(1\.dp, Color\(0xFFE2E8F0\), CircleShape\)\s+\.clickable \{ showProfileBottomSheet = true \}\s+\.padding\(4\.dp\),\s+contentAlignment = Alignment\.Center\s+\) \{.*?\}"""
profile_box_replacement = """                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable { showProfileBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUrl = user?.imageUrl
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = NavyDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }"""

content = re.sub(profile_box_pattern, profile_box_replacement, content, flags=re.DOTALL)

# 2. Redesign Metrics Cards
metrics_pattern = r"""                    // Analytics Square
                    Card\(
                        modifier = Modifier
                            \.weight\(1f\)
                            \.height\(160\.dp\)
                            \.clickable \{ onNavigateToAnalytics\(\) \},
                        shape = RoundedCornerShape\(24\.dp\),
                        colors = CardDefaults\.cardColors\(containerColor = NavyDark\)
                    \) \{
                        Column\(
                            modifier = Modifier\.fillMaxSize\(\)\.padding\(20\.dp\),
                            verticalArrangement = Arrangement\.SpaceBetween
                        \) \{
                            Box\(
                                modifier = Modifier
                                    \.size\(44\.dp\)
                                    \.clip\(CircleShape\)
                                    \.background\(Color\.White\.copy\(alpha = 0\.1f\)\),
                                contentAlignment = Alignment\.Center
                            \) \{
                                Icon\(
                                    imageVector = Icons\.Default\.InsertChartOutlined,
                                    contentDescription = null,
                                    tint = Color\.White,
                                    modifier = Modifier\.size\(22\.dp\)
                                \)
                            \}
                            Column \{
                                Text\("Monthly Spent", color = Color\.White\.copy\(alpha = 0\.6f\), fontSize = 12\.sp, fontWeight = FontWeight\.Medium\)
                                Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                                val totalSpent = accountTransactions\.filter \{ it\.amount < 0 \}\.sumOf \{ Math\.abs\(it\.amount\) \}
                                Text\(
                                    text = "\$${String\.format\("%.0f", totalSpent\)}",
                                    color = Color\.White,
                                    fontSize = 22\.sp,
                                    fontWeight = FontWeight\.Bold
                                \)
                            \}
                        \}
                    \}

                    // Loans Square
                    Card\(
                        modifier = Modifier
                            \.weight\(1f\)
                            \.height\(160\.dp\)
                            \.clickable \{ onNavigateToLoans\(\) \},
                        shape = RoundedCornerShape\(24\.dp\),
                        colors = CardDefaults\.cardColors\(containerColor = Color\.White\),
                        elevation = CardDefaults\.cardElevation\(defaultElevation = 2\.dp\)
                    \) \{
                        Column\(
                            modifier = Modifier\.fillMaxSize\(\)\.padding\(20\.dp\),
                            verticalArrangement = Arrangement\.SpaceBetween
                        \) \{
                            Box\(
                                modifier = Modifier
                                    \.size\(44\.dp\)
                                    \.clip\(CircleShape\)
                                    \.background\(PinkPrimary\.copy\(alpha = 0\.1f\)\),
                                contentAlignment = Alignment\.Center
                            \) \{
                                Icon\(
                                    imageVector = Icons\.Default\.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier\.size\(22\.dp\)
                                \)
                            \}
                            Column \{
                                Text\("Loans & Payoffs", color = TextLight, fontSize = 12\.sp, fontWeight = FontWeight\.Medium\)
                                Spacer\(modifier = Modifier\.height\(4\.dp\)\)
                                Text\(
                                    text = "\$${String\.format\("%.0f", totalLoan\)}",
                                    color = NavyDark,
                                    fontSize = 22\.sp,
                                    fontWeight = FontWeight\.Bold
                                \)
                            \}
                        \}
                    \}"""

metrics_replacement = """                    // Analytics Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(140.dp).clickable { onNavigateToAnalytics() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            Column {
                                Text("Spent", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                val totalSpent = accountTransactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
                                Text(
                                    text = "$${String.format("%,.0f", totalSpent)}",
                                    color = NavyDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Loans Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(140.dp).clickable { onNavigateToLoans() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(24.dp))
                            Column {
                                Text("Payoffs", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "$${String.format("%,.0f", totalLoan)}",
                                    color = NavyDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }"""

content = re.sub(metrics_pattern, metrics_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt", "w") as f:
    f.write(content)
