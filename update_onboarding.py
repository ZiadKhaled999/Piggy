import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
        
    replacements = [
        ('append("Welcome to\\n")', 'append(stringResource(R.string.onboarding_welcome_to) + "\\n")'),
        ('append("Piggy Ledger")', 'append(stringResource(R.string.app_name))'),
        ('subtitle = "Your modern way to save and pool resources\\nwith friends and family."', 'subtitle = stringResource(R.string.onboarding_subtitle_1)'),
        ('append("Pool Your ")', 'append(stringResource(R.string.onboarding_pool_your) + " ")'),
        ('append("Savings")', 'append(stringResource(R.string.onboarding_savings))'),
        ('subtitle = "Create shared goals and invite others to\\ncontribute. Whether it\'s for a trip, a gift, or a\\ngroup project."', 'subtitle = stringResource(R.string.onboarding_subtitle_2)'),
        ('append("Track ")', 'append(stringResource(R.string.onboarding_track) + " ")'),
        ('append("Progress")', 'append(stringResource(R.string.onboarding_progress))'),
        ('append("\\nTogether")', 'append("\\n" + stringResource(R.string.onboarding_together))'),
        ('subtitle = "Real-time updates on contributions and goal\\ncompletion. Stay motivated as you see the\\nprogress bar fill up."', 'subtitle = stringResource(R.string.onboarding_subtitle_3)'),
        ('append("Ready to ")', 'append(stringResource(R.string.onboarding_ready_to) + " ")'),
        ('append("Start")', 'append(stringResource(R.string.onboarding_start))'),
        ('append("?")', 'append("?")'),
        ('subtitle = "Let\'s set up your first goal and begin your\\njourney towards smarter collective savings."', 'subtitle = stringResource(R.string.onboarding_subtitle_4)'),
        ('text = "Continue",', 'text = stringResource(R.string.continue_btn),'),
        ('text = "BACK",', 'text = stringResource(R.string.back_btn),'),
        ('text = if (currentPage == pages.size - 1) "Get Started" else "Continue",', 'text = if (currentPage == pages.size - 1) stringResource(R.string.get_started) else stringResource(R.string.continue_btn),')
    ]
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt')

en_strings = """
    <string name="onboarding_welcome_to">Welcome to</string>
    <string name="onboarding_subtitle_1">Your modern way to save and pool resources\\nwith friends and family.</string>
    <string name="onboarding_pool_your">Pool Your</string>
    <string name="onboarding_savings">Savings</string>
    <string name="onboarding_subtitle_2">Create shared goals and invite others to\\ncontribute. Whether it\\'s for a trip, a gift, or a\\ngroup project.</string>
    <string name="onboarding_track">Track</string>
    <string name="onboarding_progress">Progress</string>
    <string name="onboarding_together">Together</string>
    <string name="onboarding_subtitle_3">Real-time updates on contributions and goal\\ncompletion. Stay motivated as you see the\\nprogress bar fill up.</string>
    <string name="onboarding_ready_to">Ready to</string>
    <string name="onboarding_start">Start</string>
    <string name="onboarding_subtitle_4">Let\\'s set up your first goal and begin your\\njourney towards smarter collective savings.</string>
    <string name="back_btn">BACK</string>
"""

ar_strings = """
    <string name="onboarding_welcome_to">مرحباً بك في</string>
    <string name="onboarding_subtitle_1">طريقتك الحديثة للادخار وتجميع الموارد\\nمع الأصدقاء والعائلة.</string>
    <string name="onboarding_pool_your">اجمع</string>
    <string name="onboarding_savings">مدخراتك</string>
    <string name="onboarding_subtitle_2">أنشئ أهدافاً مشتركة وادعُ الآخرين للمساهمة.\\nسواء كان ذلك لرحلة، أو هدية، أو\\nمشروع جماعي.</string>
    <string name="onboarding_track">تتبع</string>
    <string name="onboarding_progress">التقدم</string>
    <string name="onboarding_together">معاً</string>
    <string name="onboarding_subtitle_3">تحديثات فورية للمساهمات واكتمال الأهداف.\\nابق متحمساً وأنت ترى شريط\\nالتقدم يمتلئ.</string>
    <string name="onboarding_ready_to">مستعد</string>
    <string name="onboarding_start">للبدء</string>
    <string name="onboarding_subtitle_4">دعنا نعد هدفك الأول ونبدأ\\nرحلتك نحو مدخرات جماعية أذكى.</string>
    <string name="back_btn">رجوع</string>
"""

def append_strings(filepath, content):
    with open(filepath, 'r') as f:
        file_content = f.read()
    file_content = file_content.replace('</resources>', content + '\n</resources>')
    with open(filepath, 'w') as f:
        f.write(file_content)

append_strings('app/src/main/res/values/strings.xml', en_strings)
append_strings('app/src/main/res/values-ar/strings.xml', ar_strings)
