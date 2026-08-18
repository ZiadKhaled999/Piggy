import xml.etree.ElementTree as ET
import os

def update_xml(filepath, strings_dict):
    if not os.path.exists(filepath):
        print(f"{filepath} not found.")
        return
    tree = ET.parse(filepath)
    root = tree.getroot()
    
    existing = {elem.get('name'): elem for elem in root.findall('string')}
    for k, v in strings_dict.items():
        if k in existing:
            existing[k].text = v
        else:
            elem = ET.Element('string')
            elem.set('name', k)
            elem.text = v
            root.append(elem)
            
    ET.indent(tree, space="    ", level=0)
    tree.write(filepath, encoding="utf-8", xml_declaration=True)

en_strings = {
    "export_data_title": "Export data",
    "export_data_subtitle": "Export your data for external spreadsheets.",
    "export_include_pending": "Include Pending Transactions",
    "export_include_balances": "Include Account Balances",
    "export_include_goals": "Include Goal History",
    "export_action": "EXPORT",
    "export_found_transactions": "FOUND %1$d TRANSACTIONS",
    "export_upgrade_pro": "Upgrade to Pro to export your data",
    "export_success": "Data exported successfully to %1$s",
    "export_failed": "Export failed: %1$s"
}

ar_strings = {
    "export_data_title": "تصدير البيانات",
    "export_data_subtitle": "صدر بياناتك لملفات الإكسيل والجداول الخارجية.",
    "export_include_pending": "ضِيف المعاملات المعلقة",
    "export_include_balances": "ضِيف أرصدة الحسابات",
    "export_include_goals": "ضِيف سجل الأهداف",
    "export_action": "تصدير",
    "export_found_transactions": "لقينا %1$d معاملة",
    "export_upgrade_pro": "اشترك في برو عشان تصدر بياناتك",
    "export_success": "تم التصدير لـ %1$s بنجاح",
    "export_failed": "فشل التصدير: %1$s"
}

update_xml("app/src/main/res/values/strings.xml", en_strings)
update_xml("app/src/main/res/values-ar/strings.xml", ar_strings)
update_xml("app/src/main/res/values-ar-rEG/strings.xml", ar_strings)

print("Updated export strings.")
