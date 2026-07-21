import re

en_strings = """
    <string name="release_to_cancel">Release to cancel</string>
    <string name="slide_up_to_pause">Slide up to pause/cancel</string>
    <string name="recording_state">Recording...</string>
    <string name="hold_anywhere_to_record">Hold anywhere here to record</string>
"""

ar_strings = """
    <string name="release_to_cancel">سيب عشان تلغي</string>
    <string name="slide_up_to_pause">اسحب لفوق عشان توقف/تلغي</string>
    <string name="recording_state">بيسجل...</string>
    <string name="hold_anywhere_to_record">دوس في أي حتة هنا عشان تسجل</string>
"""

def append_strings(file_path, strings):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace("</resources>", strings + "</resources>")
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

append_strings('app/src/main/res/values/strings.xml', en_strings)
append_strings('app/src/main/res/values-ar/strings.xml', ar_strings)

print("Appended strings.")
