import os

def update_strings(filepath, new_strings):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if '</resources>' in content:
        content = content.replace('</resources>', '\n' + new_strings.strip() + '\n</resources>')
        with open(filepath, 'w') as f:
            f.write(content)

en = """
    <string name="open_badge">OPEN</string>
    <string name="amount_saved">$$%1$s saved</string>
    <string name="amount_total_extra">$$%1$s total ($$%2$s extra)</string>
"""

ar = """
    <string name="open_badge">مفتوح</string>
    <string name="amount_saved">تم ادخار $$%1$s</string>
    <string name="amount_total_extra">إجمالي $$%1$s (إضافي $$%2$s)</string>
"""

eg = """
    <string name="open_badge">مفتوح</string>
    <string name="amount_saved">محوش $$%1$s</string>
    <string name="amount_total_extra">كله $$%1$s (زيادة $$%2$s)</string>
"""

update_strings('app/src/main/res/values/strings.xml', en)
update_strings('app/src/main/res/values-ar/strings.xml', ar)
update_strings('app/src/main/res/values-ar-rEG/strings.xml', eg)
