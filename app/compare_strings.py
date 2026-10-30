import xml.etree.ElementTree as ET
import os

def get_keys(file_path):
    if not os.path.exists(file_path):
        return set()
    tree = ET.parse(file_path)
    root = tree.getroot()
    return {elem.get('name') for elem in root.findall('string')}

def get_array_keys(file_path):
    if not os.path.exists(file_path):
        return set()
    tree = ET.parse(file_path)
    root = tree.getroot()
    return {elem.get('name') for elem in root.findall('string-array')}

en_strings = 'app/src/main/res/values/strings.xml'
ar_strings = 'app/src/main/res/values-ar/strings.xml'

en_keys = get_keys(en_strings)
ar_keys = get_keys(ar_strings)

en_arrays = get_array_keys(en_strings)
ar_arrays = get_array_keys(ar_strings)

missing_strings = en_keys - ar_keys
missing_arrays = en_arrays - ar_arrays

print(f"Missing Strings: {missing_strings}")
print(f"Missing Arrays: {missing_arrays}")
