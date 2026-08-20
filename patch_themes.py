import xml.etree.ElementTree as ET

tree = ET.parse("app/src/main/res/values/themes.xml")
root = tree.getroot()

cropper_theme = ET.SubElement(root, "style", name="Theme.MyApplication.Cropper", parent="Theme.AppCompat.DayNight.DarkActionBar")
ET.SubElement(cropper_theme, "item", name="colorPrimary").text = "#F65096"
ET.SubElement(cropper_theme, "item", name="colorPrimaryDark").text = "#C63A72"
ET.SubElement(cropper_theme, "item", name="colorAccent").text = "#F65096"

tree.write("app/src/main/res/values/themes.xml", encoding="utf-8", xml_declaration=True)
