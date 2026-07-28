with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add import for Check icon
import_target = "import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight"
import_replacement = "import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight\nimport androidx.compose.material.icons.filled.Check"

if import_target in content:
    content = content.replace(import_target, import_replacement)
    print("Added Check icon import!")
else:
    print("WARNING: Import target not found!")

# 2. Add OnboardingPageData for index 10 (Streak) in pages list
pages_roadmap_target = """        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_roadmap_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_roadmap_subtitle)
        ),"""

pages_streak_replacement = """        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_streak_title)) },
            subtitle = stringResource(R.string.onboarding_streak_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_roadmap_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_roadmap_subtitle)
        ),"""

if pages_roadmap_target in content:
    content = content.replace(pages_roadmap_target, pages_streak_replacement, 1)
    print("Added Streak OnboardingPageData to pages list!")
else:
    print("WARNING: Pages list target not found!")

# 3. Update Crossfade page index branching
old_page10_marker = "} else if (pageIndex == 10) {"
old_page11_marker = "} else if (pageIndex == 11) {"

# First, replace page 11 with page 12
if old_page11_marker in content:
    content = content.replace(old_page11_marker, "} else if (pageIndex == 12) {")
    print("Updated page 11 to page 12!")

# Second, define page 10 (Streak slide) and update old page 10 (Roadmap) to page 11
streak_slide_code = """} else if (pageIndex == 10) {
                    // STREAK / HABIT SLIDE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 20.dp))

                        Text(
                            text = stringResource(R.string.onboarding_streak_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 32.sp else 40.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.onboarding_streak_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 36.dp else 52.dp))

                        // HABIT Streak Visual Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(if (isSmallScreen) 0.95f else 0.90f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val habitLetters = listOf("H", "A", "B", "I", "T")
                            val days = listOf(
                                stringResource(R.string.onboarding_day_thu),
                                stringResource(R.string.onboarding_day_fri),
                                stringResource(R.string.onboarding_day_sat),
                                stringResource(R.string.onboarding_day_sun),
                                stringResource(R.string.onboarding_day_mon)
                            )
                            val completedSteps = 3 // H, A, B completed (THU, FRI, SAT)

                            // Letters Row (H A B I T)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                habitLetters.forEachIndexed { index, letter ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (index < completedSteps) {
                                            Text(
                                                text = letter,
                                                fontSize = if (isSmallScreen) 36.sp else 44.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NavyDark
                                            )
                                        } else {
                                            Text(
                                                text = letter,
                                                fontSize = if (isSmallScreen) 36.sp else 44.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                            // Circles Row (Checkmark icons for active days, empty circles for upcoming days)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                days.forEachIndexed { index, day ->
                                    val isCompleted = index < completedSteps
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (isCompleted) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isSmallScreen) 48.dp else 56.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFF34D399),
                                                                Color(0xFF059669)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isSmallScreen) 48.dp else 56.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF8FAFC))
                                                    .border(
                                                        width = 3.dp,
                                                        color = Color(0xFFCBD5E1),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = day,
                                            fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) NavyDark else Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 30.dp))
                    }
                } else if (pageIndex == 11) {"""

if old_page10_marker in content:
    content = content.replace(old_page10_marker, streak_slide_code, 1)
    print("Inserted page 10 Streak slide and updated Roadmap to page 11!")
else:
    print("WARNING: Old page 10 marker not found!")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
