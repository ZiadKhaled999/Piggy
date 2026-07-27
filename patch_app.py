import re

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerApp.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState(initial = null)',
    'val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState(initial = null)\n    val hasHeardAboutUs by viewModel.hasHeardAboutUs.collectAsState(initial = null)'
)

content = content.replace(
    'LaunchedEffect(hasOnboarded, hasLanguageSelected, isAuthenticated)',
    'LaunchedEffect(hasOnboarded, hasLanguageSelected, hasHeardAboutUs, isAuthenticated)'
)

content = content.replace(
    '                        } else if (hasOnboarded == false) {\n                            navController.navigate(Screen.Onboarding) {',
    '                        } else if (hasHeardAboutUs == false) {\n                            navController.navigate(Screen.HearAboutUs) {\n                                popUpTo(Screen.Splash) { inclusive = true }\n                            }\n                        } else if (hasOnboarded == false) {\n                            navController.navigate(Screen.Onboarding) {'
)

content = content.replace(
    'hasOnboarded == true && hasLanguageSelected == true && isAuthenticated == true',
    'hasOnboarded == true && hasLanguageSelected == true && hasHeardAboutUs == true && isAuthenticated == true'
)

content = content.replace(
    'viewModel.completeLanguageSelection()\n                            navController.navigate(Screen.Onboarding) {\n                                popUpTo(Screen.LanguageSelection) { inclusive = true }\n                            }',
    'viewModel.completeLanguageSelection()\n                            navController.navigate(Screen.HearAboutUs) {\n                                popUpTo(Screen.LanguageSelection) { inclusive = true }\n                            }'
)

new_route = '''
                composable<Screen.HearAboutUs> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Hear About Us"))
                    }
                    HearAboutUsScreen(
                        onContinue = { source ->
                            viewModel.completeHearAboutUs(source)
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.HearAboutUs) { inclusive = true }
                            }
                        }
                    )
                }
'''

content = content.replace(
    '                composable<Screen.Onboarding> {',
    new_route + '\n                composable<Screen.Onboarding> {'
)


with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerApp.kt', 'w') as f:
    f.write(content)
