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

en = {"completed_badge": "Completed", "cancel_btn": "Cancel"}
ar = {"completed_badge": "مكتمل", "cancel_btn": "إلغاء"}
eg = {"completed_badge": "تم بنجاح", "cancel_btn": "إلغاء"}

update_strings('app/src/main/res/values/strings.xml', en)
update_strings('app/src/main/res/values-ar/strings.xml', ar)
update_strings('app/src/main/res/values-ar-rEG/strings.xml', eg)
