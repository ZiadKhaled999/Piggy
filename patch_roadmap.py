with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

old_roadmap_steps = """
                    val step1Title = stringResource(R.string.onboarding_step_workspace_title, focusName)
                    val step2Title = stringResource(R.string.onboarding_step_intensity_title, intensityName, intensityValue)

                    val steps = listOf(
                        Triple(step1Title, focusDesc, "👥"),
                        Triple(step2Title, intensityDesc, "📈"),
                        Triple(syncTitle, syncDesc, if (isSmsGranted) "⚡" else "📋"),
                        Triple(milestoneTitle, milestoneDesc, "🎯")
                    )"""

new_roadmap_steps = """
                    val step1Title = stringResource(R.string.onboarding_step_workspace_title, focusName)
                    val step2Title = stringResource(R.string.onboarding_step_intensity_title, intensityName, intensityValue)
                    
                    val stepsList = mutableListOf(
                        Triple(step1Title, focusDesc, "👥"),
                        Triple(step2Title, intensityDesc, "📈"),
                        Triple(syncTitle, syncDesc, if (isSmsGranted) "⚡" else "📋"),
                        Triple(milestoneTitle, milestoneDesc, "🎯")
                    )
                    
                    if (relatesToLoans == true) {
                        stepsList.add(0, Triple("Debt Payoff Strategy", "We will use the Debt Avalanche method to save you maximum interest.", "💸"))
                    }
                    if (relatesToAccounts == true) {
                        stepsList.add(1, Triple("Link Accounts", "We will prompt you to securely link all your scattered accounts first.", "🔗"))
                    }
                    if (relatesToEmergency == true) {
                        stepsList.add(Triple("Emergency Buffer", "Your first savings goal will be an emergency fund to stop the relapse cycle.", "🛡️"))
                    }
                    
                    val steps = stepsList.toList()"""

content = content.replace(old_roadmap_steps, new_roadmap_steps)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
