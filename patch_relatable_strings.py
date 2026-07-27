import re

# 1. values/strings.xml
with open('./app/src/main/res/values/strings.xml', 'r') as f:
    en_content = f.read()

en_additions = """
    <string name="onboarding_relatable_header">Do you relate to the statement below?</string>
    <string name="onboarding_relatable_statement_1">"Whenever I get paid, I feel like none of the money is actually mine because of what I owe."</string>
    <string name="onboarding_relatable_statement_2">"My money is split across so many accounts that I never actually know how much I\'m safe to spend."</string>
    <string name="onboarding_relatable_statement_3">"Every time I finally build up some savings, one surprise expense resets my progress back to zero."</string>
    <string name="yes_label">Yes</string>
    <string name="no_label">No</string>
"""
en_content = en_content.replace('</resources>', en_additions + '</resources>')
with open('./app/src/main/res/values/strings.xml', 'w') as f: f.write(en_content)

# 2. values-ar/strings.xml
with open('./app/src/main/res/values-ar/strings.xml', 'r') as f:
    ar_content = f.read()

ar_additions = """
    <string name="onboarding_relatable_header">هل تتفق مع العبارة التالية؟</string>
    <string name="onboarding_relatable_statement_1">"كل ما أقبض، بحس إن الفلوس دي مش بتاعتي أصلاً بسبب الديون اللي عليا."</string>
    <string name="onboarding_relatable_statement_2">"فلوسي متوزعة على حسابات كتير لدرجة إني ماببقاش عارف أنا معايا كام بالظبط وأقدر أصرف إيه."</string>
    <string name="onboarding_relatable_statement_3">"كل ما أبدأ أحوش مبلغ، تطلعلي مصيبة فجأة ترجعني للصفر تاني."</string>
    <string name="yes_label">نعم</string>
    <string name="no_label">لا</string>
"""
ar_content = ar_content.replace('</resources>', ar_additions + '</resources>')
with open('./app/src/main/res/values-ar/strings.xml', 'w') as f: f.write(ar_content)

# 3. values-ar-rEG/strings.xml
with open('./app/src/main/res/values-ar-rEG/strings.xml', 'r') as f:
    eg_content = f.read()

eg_additions = """
    <string name="onboarding_relatable_header">بتحس بالكلام ده؟</string>
    <string name="onboarding_relatable_statement_1">"أول ما بقبض، بحس إن الفلوس دي مش بتاعتي أساساً بسبب الديون والأقساط اللي عليا."</string>
    <string name="onboarding_relatable_statement_2">"فلوسي متطورة في ميت حساب ومحفظة، لدرجة إني ماببقاش عارف أنا معايا كام بجد ولا أقدر أصرف إيه بأمان."</string>
    <string name="onboarding_relatable_statement_3">"كل ما أحوش قرشين على جنب، تطلعلي حوار فجأة يطيرهم كلهم ويرجعني عالحديدة تاني."</string>
    <string name="yes_label">أيوه</string>
    <string name="no_label">لأ</string>
"""
eg_content = eg_content.replace('</resources>', eg_additions + '</resources>')
with open('./app/src/main/res/values-ar-rEG/strings.xml', 'w') as f: f.write(eg_content)

print("Strings patched!")
