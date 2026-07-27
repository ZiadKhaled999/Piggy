import re

# 1. values/strings.xml
with open('./app/src/main/res/values/strings.xml', 'r') as f:
    en_content = f.read()

en_supercharge = """
    <string name="onboarding_supercharge_title">Supercharge Savings</string>
    <string name="onboarding_supercharge_subtitle">Automated guidance helps you reach milestones up to 7 times faster than manual tracking.</string>
    <string name="onboarding_supercharge_solo">Solo</string>
    <string name="onboarding_supercharge_faster">FASTER</string>
    <string name="onboarding_supercharge_with_piggy">With %1$s</string>
"""
en_content = en_content.replace('</resources>', en_supercharge + '</resources>')

with open('./app/src/main/res/values/strings.xml', 'w') as f:
    f.write(en_content)


# 2. values-ar/strings.xml
with open('./app/src/main/res/values-ar/strings.xml', 'r') as f:
    ar_content = f.read()

# Replace any occurrence of "بيجي ليدجر" or "Piggy Ledger" with "دفتر الحصالة" in values-ar
ar_content = ar_content.replace("بيجي ليدجر", "دفتر الحصالة")
ar_content = ar_content.replace("Piggy Ledger", "دفتر الحصالة")

ar_supercharge = """
    <string name="onboarding_supercharge_title">ضاعف سرعة ادخارك</string>
    <string name="onboarding_supercharge_subtitle">التوجيه الذكي والتلقائي يساعدك على تحقيق أهدافك أسرع بـ 7 أضعاف مقارنة بالتتبع اليدوي.</string>
    <string name="onboarding_supercharge_solo">بمفردك</string>
    <string name="onboarding_supercharge_faster">أسرع</string>
    <string name="onboarding_supercharge_with_piggy">مع %1$s</string>
"""
ar_content = ar_content.replace('</resources>', ar_supercharge + '</resources>')

with open('./app/src/main/res/values-ar/strings.xml', 'w') as f:
    f.write(ar_content)


# 3. values-ar-rEG/strings.xml
with open('./app/src/main/res/values-ar-rEG/strings.xml', 'r') as f:
    eg_content = f.read()

eg_content = eg_content.replace("بيجي ليدجر", "دفتر الحصالة")
eg_content = eg_content.replace("Piggy Ledger", "دفتر الحصالة")

eg_supercharge = """
    <string name="onboarding_supercharge_title">سرّع توفيرك لأقصى درجة</string>
    <string name="onboarding_supercharge_subtitle">التوجيه التلقائي بيساعدك توصل لأهدافك أسرع 7 مرات من الحساب اليدوي.</string>
    <string name="onboarding_supercharge_solo">لوحدك</string>
    <string name="onboarding_supercharge_faster">أسرع</string>
    <string name="onboarding_supercharge_with_piggy">مع %1$s</string>
"""
eg_content = eg_content.replace('</resources>', eg_supercharge + '</resources>')

with open('./app/src/main/res/values-ar-rEG/strings.xml', 'w') as f:
    f.write(eg_content)

print("Strings patched for supercharge and brand name!")
