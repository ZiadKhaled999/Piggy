import xml.etree.ElementTree as ET

# English strings
en_additions = """
    <string name="onboarding_step_debt_title">Debt Payoff Strategy</string>
    <string name="onboarding_step_debt_desc">We will use the Debt Snowball method to build psychological momentum and quick wins.</string>
    <string name="onboarding_step_accounts_title">Link Accounts</string>
    <string name="onboarding_step_accounts_desc">We will prompt you to securely link all your scattered accounts first.</string>
    <string name="onboarding_step_emergency_title">Emergency Buffer</string>
    <string name="onboarding_step_emergency_desc">Your first savings goal will be an emergency fund to stop the relapse cycle.</string>
"""

with open('./app/src/main/res/values/strings.xml', 'r') as f:
    content = f.read()

if 'onboarding_step_debt_title' not in content:
    content = content.replace('</resources>', en_additions + '\n</resources>')
    with open('./app/src/main/res/values/strings.xml', 'w') as f:
        f.write(content)

# Arabic (Standard) strings
ar_additions = """
    <string name="onboarding_step_debt_title">خطة تسديد الديون</string>
    <string name="onboarding_step_debt_desc">سنعتمد طريقة كرة الثلج لبناء دافع معنوي وتحقيق إنجازات سريعة.</string>
    <string name="onboarding_step_accounts_title">ربط الحسابات</string>
    <string name="onboarding_step_accounts_desc">سنوجهك لربط جميع حساباتك المتفرقة بأمان أولاً.</string>
    <string name="onboarding_step_emergency_title">صندوق الطوارئ</string>
    <string name="onboarding_step_emergency_desc">هدفك الادخاري الأول سيكون بناء صندوق طوارئ لمنع الانتكاسات.</string>
"""

with open('./app/src/main/res/values-ar/strings.xml', 'r') as f:
    content_ar = f.read()

if 'onboarding_step_debt_title' not in content_ar:
    content_ar = content_ar.replace('</resources>', ar_additions + '\n</resources>')
    with open('./app/src/main/res/values-ar/strings.xml', 'w') as f:
        f.write(content_ar)

# Arabic (Egyptian) strings
eg_additions = """
    <string name="onboarding_step_debt_title">خطة سداد الديون</string>
    <string name="onboarding_step_debt_desc">هنستخدم طريقة كرة الثلج عشان تبدأ بإنجازات سريعة وتشجع نفسك.</string>
    <string name="onboarding_step_accounts_title">ربط الحسابات</string>
    <string name="onboarding_step_accounts_desc">هنساعدك تربط كل حساباتك ومحافظك المتطورة في مكان واحد بالأول.</string>
    <string name="onboarding_step_emergency_title">صندوق الطوارئ</string>
    <string name="onboarding_step_emergency_desc">أول هدف تحويش ليك هيكون صندوق طوارئ عشان المصاريف المفاجئة متأثرش عليك.</string>
"""

with open('./app/src/main/res/values-ar-rEG/strings.xml', 'r') as f:
    content_eg = f.read()

if 'onboarding_step_debt_title' not in content_eg:
    content_eg = content_eg.replace('</resources>', eg_additions + '\n</resources>')
    with open('./app/src/main/res/values-ar-rEG/strings.xml', 'w') as f:
        f.write(content_eg)

print("Updated all strings.xml files successfully!")
