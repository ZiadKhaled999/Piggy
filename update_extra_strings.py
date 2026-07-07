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
    "transaction_memo_header": "TRANSACTION MEMO",
    "due_prefix": "Due: %1$s",
    "no_deadline_set": "No deadline set"
}

ar = {
    "transaction_memo_header": "مذكرة المعاملة",
    "due_prefix": "تاريخ الاستحقاق: %1$s",
    "no_deadline_set": "لم يتم تحديد موعد نهائي"
}

eg = {
    "transaction_memo_header": "ملحوظة عن العملية",
    "due_prefix": "ميعادها: %1$s",
    "no_deadline_set": "مفيش ميعاد محدد"
}

update_strings('app/src/main/res/values/strings.xml', en)
update_strings('app/src/main/res/values-ar/strings.xml', ar)
update_strings('app/src/main/res/values-ar-rEG/strings.xml', eg)
