import xml.etree.ElementTree as ET

strings = {
    'comp_acc_goals': {'en': 'Accounts & Goals', 'ar': 'الحسابات والأهداف', 'eg': 'الحسابات والحصالات'},
    'comp_budgets_loans': {'en': 'Budgets & Loans', 'ar': 'الميزانيات والقروض', 'eg': 'الميزانيات والسلفيات'},
    'comp_adv_analytics': {'en': 'Advanced Analytics', 'ar': 'تحليلات متقدمة', 'eg': 'تحليلات متقدمة'},
    'comp_export': {'en': 'Data Export (CSV/PDF)', 'ar': 'تصدير البيانات (CSV/PDF)', 'eg': 'تصدير البيانات (CSV/PDF)'},
    'comp_custom_categories': {'en': 'Custom Categories', 'ar': 'فئات مخصصة', 'eg': 'تصنيفات بمزاجك'},
    'comp_screenshot_protect': {'en': 'Screenshot Protection', 'ar': 'حماية لقطة الشاشة', 'eg': 'حماية سكرين شوت'},
    'comp_cloud_sync': {'en': 'Cloud Backup & Sync', 'ar': 'نسخ احتياطي ومزامنة سحابية', 'eg': 'نسخة احتياطية وسينك'},
    'comp_priority_support': {'en': 'Priority Support', 'ar': 'أولوية الدعم', 'eg': 'أولوية الدعم'},
    'two_max': {'en': '2 Max', 'ar': '2 كحد أقصى', 'eg': 'أخرك ٢'},
    
    'plan_lifetime_desc_2': {'en': 'Unlock lifetime unlimited access & all future features', 'ar': 'وصول دائم لجميع الميزات الحالية والمستقبلية', 'eg': 'وصول دائم ومفتوح لكل المميزات الحالية والمستقبلية'},
    'plan_lifetime_renew_2': {'en': 'One-time payment of %s. No renewal or hidden fees.', 'ar': 'دفعة واحدة بقيمة %s. بدون تجديد أو رسوم خفية.', 'eg': 'دفع مرة واحدة %s. مفيش تجديد أو مصاريف مخفية.'},
    'upgrade_lifetime_2': {'en': 'Upgrade Lifetime', 'ar': 'ترقية مدى الحياة', 'eg': 'رقي لمدى الحياة'},
    'best_value_caps': {'en': 'BEST VALUE', 'ar': 'أفضل قيمة', 'eg': 'اللقطة'}
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

