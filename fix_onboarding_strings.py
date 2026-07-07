def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    replacement = """
    val welcomeTo = stringResource(R.string.onboarding_welcome_to)
    val appName = stringResource(R.string.app_name)
    val subtitle1 = stringResource(R.string.onboarding_subtitle_1)
    
    val poolYour = stringResource(R.string.onboarding_pool_your)
    val savings = stringResource(R.string.onboarding_savings)
    val subtitle2 = stringResource(R.string.onboarding_subtitle_2)
    
    val trackStr = stringResource(R.string.onboarding_track)
    val progress = stringResource(R.string.onboarding_progress)
    val together = stringResource(R.string.onboarding_together)
    val subtitle3 = stringResource(R.string.onboarding_subtitle_3)
    
    val readyTo = stringResource(R.string.onboarding_ready_to)
    val startStr = stringResource(R.string.onboarding_start)
    val subtitle4 = stringResource(R.string.onboarding_subtitle_4)
    
    val pages = listOf(
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString {
                append(welcomeTo + "\\n")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(appName)
                }
            },
            subtitle = subtitle1
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_pool,
            title = buildAnnotatedString {
                append(poolYour + " ")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(savings)
                }
            },
            subtitle = subtitle2
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_track,
            title = buildAnnotatedString {
                append(trackStr + " ")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(progress)
                }
                append("\\n" + together)
            },
            subtitle = subtitle3
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_app_logo,
            title = buildAnnotatedString {
                append(readyTo + " ")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(startStr)
                }
                append("?")
            },
            subtitle = subtitle4
        )
    )
"""
    # We replace the whole `val pages = remember { listOf(...) }` block
    import re
    # Find start of `val pages = remember {`
    # Find end of it
    
    start_idx = content.find('val pages = remember {')
    end_idx = content.find('    }\n\n    Column(', start_idx)
    
    if start_idx != -1 and end_idx != -1:
        content = content[:start_idx] + replacement + content[end_idx + 6:]
        with open(filepath, 'w') as f:
            f.write(content)
            
update_file('app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt')
