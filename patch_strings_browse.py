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

add_strings("app/src/main/res/values/strings.xml", {"profile_browse": "Browse"})
add_strings("app/src/main/res/values-ar/strings.xml", {"profile_browse": "تصفح"})
add_strings("app/src/main/res/values-ar-rEG/strings.xml", {"profile_browse": "تصفح"})
