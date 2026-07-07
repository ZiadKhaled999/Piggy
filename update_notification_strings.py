import os
import re

def update_strings(filepath, new_entries):
    with open(filepath, 'r') as f:
        content = f.read()
    
    for key in new_entries.keys():
        content = re.sub(f'    <string name="{key}">.*</string>\n', '', content)
    
    new_xml_entries = ""
    for key, val in new_entries.items():
        val = val.replace("'", "\\'").replace("&", "&amp;")
        new_xml_entries += f'    <string name="{key}">{val}</string>\n'
    
    content = content.replace('</resources>', new_xml_entries + '</resources>')
    with open(filepath, 'w') as f:
        f.write(content)

en = {
    "deadline_channel_name": "Deadline Reminders",
    "deadline_channel_desc": "Notifications for loan repayment deadlines",
    "deadline_title": "Deadline Over!",
    "cancel_action": "Cancel",
    "snooze_action": "Snooze"
}

ar = {
    "deadline_channel_name": "تذكيرات الموعد النهائي",
    "deadline_channel_desc": "إشعارات للمواعيد النهائية لسداد القروض",
    "deadline_title": "انتهى الموعد النهائي!",
    "cancel_action": "إلغاء",
    "snooze_action": "غفوة"
}

eg = {
    "deadline_channel_name": "تنبيهات المواعيد",
    "deadline_channel_desc": "إشعارات عشان تفكرك بمواعيد سداد الفلوس",
    "deadline_title": "الميعاد خلص!",
    "cancel_action": "إلغاء",
    "snooze_action": "فكرني بعدين"
}

update_strings('app/src/main/res/values/strings.xml', en)
update_strings('app/src/main/res/values-ar/strings.xml', ar)
update_strings('app/src/main/res/values-ar-rEG/strings.xml', eg)
