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

ar_strings = {
    "streak_days_streak": "عامل ستريك %1$s يوم!",
    "streak_week_wins": "أسبوع كله مكاسب! خلينا نحافظ على الستريك ده مولع 🔥",
    "streak_great_consistency": "عاش جداً! خليك مكمل وسجل مصاريفك كل يوم",
    "streak_start_today": "ابدأ الستريك بتاعك النهاردة وسجل أي معاملة!",
    "streak_legend_streak": "الستريك",
    "streak_legend_frozen": "متجمد",
    "streak_legend_missed": "راح عليك",
    "longest_streak": "🏆 %1$s يوم",
    "share_my_streak": "شير الستريك بتاعي",
    "share_your_streak": "شير الستريك بتاعك",
    "share_messages": "رسائل",
    "save_image": "حفظ الصورة",
    "share_more": "المزيد",
    "saved_to_gallery": "اتحفظت في المعرض! 📸",
    "failed_to_save_image": "فشل حفظ الصورة"
}

update_xml("app/src/main/res/values-ar/strings.xml", ar_strings)
update_xml("app/src/main/res/values-ar-rEG/strings.xml", ar_strings)

print("Updated Egyptian slang.")
