import xml.etree.ElementTree as ET

strings = {
    'restore_subscription': {'en': 'Restore subscription', 'ar': 'استعادة الاشتراك', 'eg': 'رجع اشتراكك'}
}

files = {
    'en': 'app/src/main/res/values/strings.xml',
    'ar': 'app/src/main/res/values-ar/strings.xml',
    'eg': 'app/src/main/res/values-ar-rEG/strings.xml'
}

for lang, path in files.items():
    tree = ET.parse(path)
    root = tree.getroot()
    for key, vals in strings.items():
        exists = False
        for child in root:
            if child.get('name') == key:
                exists = True
                child.text = vals[lang]
                break
        if not exists:
            elem = ET.Element('string', name=key)
            elem.text = vals[lang]
            root.append(elem)
    tree.write(path, encoding='utf-8', xml_declaration=True)
