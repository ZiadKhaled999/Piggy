import re

# Patch HearAboutUsScreen.kt
with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'r') as f:
    content = f.read()

old_title_block = """    val fullTitleText = stringResource(id = R.string.how_did_you_hear_about_us)
    val highlightWord = "Piggy Ledger"
    val annotatedTitle = remember(fullTitleText) {
        buildAnnotatedString {
            val startIndex = fullTitleText.indexOf(highlightWord)
            if (startIndex != -1) {
                append(fullTitleText.substring(0, startIndex))
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(highlightWord)
                }
                append(fullTitleText.substring(startIndex + highlightWord.length))
            } else {
                append(fullTitleText)
            }
        }
    }"""

new_title_block = """    val brandName = stringResource(id = R.string.piggy_ledger_brand)
    val fullTitleText = stringResource(id = R.string.how_did_you_hear_about_us, brandName)
    val annotatedTitle = remember(fullTitleText, brandName) {
        buildAnnotatedString {
            val startIndex = fullTitleText.indexOf(brandName)
            if (startIndex != -1) {
                append(fullTitleText.substring(0, startIndex))
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(brandName)
                }
                append(fullTitleText.substring(startIndex + brandName.length))
            } else {
                append(fullTitleText)
            }
        }
    }"""

content = content.replace(old_title_block, new_title_block)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'w') as f:
    f.write(content)


# Patch OnboardingScreen.kt
with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    onboarding_content = f.read()

# 1. Update Page Data for Choose Your Pace
onboarding_content = onboarding_content.replace(
    'title = buildAnnotatedString { append("Choose Your Pace") },\n            subtitle = "Decide how you want to reach your financial milestones."',
    'title = buildAnnotatedString { append(stringResource(R.string.onboarding_choose_pace_title)) },\n            subtitle = stringResource(R.string.onboarding_choose_pace_subtitle)'
)

# 2. Update rateLabel in Intensity card
onboarding_content = onboarding_content.replace(
    'text = "Saving Rate: $rateLabel",',
    'text = stringResource(R.string.onboarding_saving_rate_label, rateLabel),'
)

# 3. Update roadmap steps logic
old_roadmap_code = """                    val focusDesc = when (selectedIntent) {
                        0 -> "Private vault configured to keep your core balance safe."
                        1 -> "Tailored to track lent/borrowed cash and deadlines."
                        else -> "Prepared to automatically organize incoming receipts."
                    }

                    val intensityName = when (selectedIntensity) {
                        0 -> stringResource(R.string.onboarding_personalize_intensity_casual)
                        1 -> stringResource(R.string.onboarding_personalize_intensity_balanced)
                        else -> stringResource(R.string.onboarding_personalize_intensity_aggressive)
                    }

                    val intensityValue = when (selectedIntensity) {
                        0 -> "5% – 10%"
                        1 -> "15% – 20%"
                        else -> "30%+"
                    }

                    val intensityDesc = when (selectedIntensity) {
                        0 -> "A light, steady habit to build savings without strain."
                        1 -> "The perfect pace for hitting your major milestones."
                        else -> "High-velocity savings mode to smash buffers in record time."
                    }

                    val isSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    
                    val syncTitle = "Automation Assistant"
                    val syncDesc = if (isSmsGranted) {
                        "Vodafone Cash, Orange Cash, and bank alert SMS parsed automatically!"
                    } else {
                        "Log transactions manually. Enable SMS auto-sync anytime in Settings."
                    }

                    val milestoneTitle = "Your First Major Goal"
                    val milestoneDesc = when (selectedIntent) {
                        0 -> "Create a vault and set a deposit to start your streak!"
                        1 -> "Log your first lent/borrowed deal to see net dues."
                        else -> "Complete a transfer and let our automation handle it."
                    }

                    val steps = listOf(
                        Triple("Workspace Configured: $focusName", focusDesc, "👥"),
                        Triple("Savings Rate Configured: $intensityName ($intensityValue)", intensityDesc, "📈"),
                        Triple(syncTitle, syncDesc, if (isSmsGranted) "⚡" else "📋"),
                        Triple(milestoneTitle, milestoneDesc, "🎯")
                    )"""

new_roadmap_code = """                    val focusDesc = when (selectedIntent) {
                        0 -> stringResource(R.string.onboarding_step_workspace_desc_personal)
                        1 -> stringResource(R.string.onboarding_step_workspace_desc_loans)
                        else -> stringResource(R.string.onboarding_step_workspace_desc_auto)
                    }

                    val intensityName = when (selectedIntensity) {
                        0 -> stringResource(R.string.onboarding_personalize_intensity_casual)
                        1 -> stringResource(R.string.onboarding_personalize_intensity_balanced)
                        else -> stringResource(R.string.onboarding_personalize_intensity_aggressive)
                    }

                    val intensityValue = when (selectedIntensity) {
                        0 -> "5% – 10%"
                        1 -> "15% – 20%"
                        else -> "30%+"
                    }

                    val intensityDesc = when (selectedIntensity) {
                        0 -> stringResource(R.string.onboarding_step_intensity_desc_casual)
                        1 -> stringResource(R.string.onboarding_step_intensity_desc_balanced)
                        else -> stringResource(R.string.onboarding_step_intensity_desc_aggressive)
                    }

                    val isSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    
                    val syncTitle = stringResource(R.string.onboarding_step_sync_title)
                    val syncDesc = if (isSmsGranted) {
                        stringResource(R.string.onboarding_step_sync_desc_granted)
                    } else {
                        stringResource(R.string.onboarding_step_sync_desc_manual)
                    }

                    val milestoneTitle = stringResource(R.string.onboarding_step_milestone_title)
                    val milestoneDesc = when (selectedIntent) {
                        0 -> stringResource(R.string.onboarding_step_milestone_desc_personal)
                        1 -> stringResource(R.string.onboarding_step_milestone_desc_loans)
                        else -> stringResource(R.string.onboarding_step_milestone_desc_auto)
                    }

                    val step1Title = stringResource(R.string.onboarding_step_workspace_title, focusName)
                    val step2Title = stringResource(R.string.onboarding_step_intensity_title, intensityName, intensityValue)

                    val steps = listOf(
                        Triple(step1Title, focusDesc, "👥"),
                        Triple(step2Title, intensityDesc, "📈"),
                        Triple(syncTitle, syncDesc, if (isSmsGranted) "⚡" else "📋"),
                        Triple(milestoneTitle, milestoneDesc, "🎯")
                    )"""

onboarding_content = onboarding_content.replace(old_roadmap_code, new_roadmap_code)

# 4. Update thinkingPhase text
old_thinking_code = """                                val text = when(thinkingPhase) {
                                    0 -> "Thinking..."
                                    1 -> "Sketching..."
                                    else -> "Making Plan..."
                                }"""

new_thinking_code = """                                val text = when(thinkingPhase) {
                                    0 -> stringResource(R.string.onboarding_ai_thinking)
                                    1 -> stringResource(R.string.onboarding_ai_sketching)
                                    else -> stringResource(R.string.onboarding_ai_making_plan)
                                }"""

onboarding_content = onboarding_content.replace(old_thinking_code, new_thinking_code)

# 5. Update syncing text
onboarding_content = onboarding_content.replace(
    'text = "syncing",',
    'text = stringResource(R.string.onboarding_ai_syncing),'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(onboarding_content)

print("Screens patched successfully!")
