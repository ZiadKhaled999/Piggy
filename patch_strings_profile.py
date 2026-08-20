import xml.etree.ElementTree as ET

def add_strings(file_path, strings_dict):
    tree = ET.parse(file_path)
    root = tree.getroot()
    for name, value in strings_dict.items():
        existing = root.find(f".//string[@name='{name}']")
        if existing is not None:
            existing.text = value
        else:
            elem = ET.SubElement(root, "string", name=name)
            elem.text = value
    tree.write(file_path, encoding="utf-8", xml_declaration=True)

en_strings = {
    "profile_title": "Edit Profile",
    "profile_edit_details": "Edit Details",
    "profile_user": "User",
    "profile_change_photo": "Change Photo"
}

ar_strings = {
    "profile_title": "تعديل الملف الشخصي",
    "profile_edit_details": "تعديل البيانات",
    "profile_user": "مستخدم",
    "profile_change_photo": "تغيير الصورة"
}

add_strings("app/src/main/res/values/strings.xml", en_strings)
add_strings("app/src/main/res/values-ar/strings.xml", ar_strings)
add_strings("app/src/main/res/values-ar-rEG/strings.xml", ar_strings)
