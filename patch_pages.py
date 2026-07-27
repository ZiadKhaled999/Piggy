with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

new_pages = """
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_intensity_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_intensity_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append("") },
            subtitle = ""
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append("") },
            subtitle = ""
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append("") },
            subtitle = ""
        ),
"""

content = content.replace(
"""        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_intensity_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_intensity_subtitle)
        ),""", new_pages)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
