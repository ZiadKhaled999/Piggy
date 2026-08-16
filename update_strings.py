import sys
import xml.etree.ElementTree as ET

strings = {
    'pro_member_active': {'en': 'PRO MEMBER ACTIVE', 'ar': 'عضو برو نشط', 'eg': 'بريميم شغال'},
    'pro_title': {'en': 'Piggy Ledger Pro', 'ar': 'بيجي ليدجر برو', 'eg': 'بيجي ليدجر برو'},
    'pro_desc': {'en': 'All premium features are unlocked and active on your device.', 'ar': 'تم تفعيل جميع الميزات الاحترافية وهي جاهزة للاستخدام على جهازك.', 'eg': 'كل مميزات البرو متفعلة وشغالة معاك على الموبايل.'},
    'pro_feature_1': {'en': 'Unlimited Accounts & Savings Goals', 'ar': 'حسابات وأهداف ادخار غير محدودة', 'eg': 'حسابات وحصالات مفتوحة من غير ليميت'},
    'pro_feature_2': {'en': 'Unlimited Budgets & Loan Ledgers', 'ar': 'ميزانيات وسجلات قروض غير محدودة', 'eg': 'ميزانيات وسجلات ديون مفتوحة براحتك'},
    'pro_feature_3': {'en': 'Advanced Financial Analytics & Charts', 'ar': 'تحليلات ورسوم بيانية مالية متقدمة', 'eg': 'رسومات وتحليلات مالية متقدمة'},
    'pro_feature_4': {'en': 'Data Export (CSV/PDF) & Cloud Sync', 'ar': 'تصدير البيانات ومزامنة سحابية', 'eg': 'طلع بياناتك واعملها سينك على الكلاود'},
    'pro_feature_5': {'en': 'Screenshot Protection & Custom Categories', 'ar': 'حماية لقطة الشاشة وفئات مخصصة', 'eg': 'حماية سكرين شوت وتصنيفات بمزاجك'},
    'plan_monthly': {'en': 'Monthly', 'ar': 'شهري', 'eg': 'كل شهر'},
    'plan_monthly_desc': {'en': 'Keep tracking with expanded access & unlimited control', 'ar': 'استمر في التتبع مع وصول موسع وتحكم غير محدود', 'eg': 'كمل متابعة بمميزات اكتر وتحكم براحتك خالص'},
    'plan_monthly_renew': {'en': 'Renews for %s/month. Cancel anytime.', 'ar': 'يُجدد بـ %s/شهر. الإلغاء في أي وقت.', 'eg': 'هيتجدد بـ %s في الشهر. وتقدر تلغي في أي وقت.'},
    'upgrade_monthly': {'en': 'Upgrade Monthly', 'ar': 'ترقية شهرية', 'eg': 'رقي اشتراكك الشهري'},
    'plan_yearly': {'en': 'Yearly', 'ar': 'سنوي', 'eg': 'كل سنة'},
    'plan_yearly_desc': {'en': 'Save 40% with annual billing', 'ar': 'وفر 40% مع الدفع السنوي', 'eg': 'وفر 40% مع الدفع السنوي'},
    'plan_yearly_renew': {'en': 'Renews for %s/year. Cancel anytime.', 'ar': 'يُجدد بـ %s/سنة. الإلغاء في أي وقت.', 'eg': 'هيتجدد بـ %s في السنة. وتقدر تلغي في أي وقت.'},
    'upgrade_yearly': {'en': 'Upgrade Yearly', 'ar': 'ترقية سنوية', 'eg': 'رقي اشتراكك السنوي'},
    'plan_lifetime': {'en': 'Lifetime', 'ar': 'مدى الحياة', 'eg': 'مدى الحياة'},
    'plan_lifetime_desc': {'en': 'One-time payment for forever access', 'ar': 'دفعة لمرة واحدة لوصول دائم', 'eg': 'ادفع مرة واحدة واستمتع على طول'},
    'plan_lifetime_renew': {'en': 'Pay once, yours forever.', 'ar': 'ادفع مرة واحدة، ملكك للأبد.', 'eg': 'ادفع مرة واحدة، ويفضل بتاعك العمر كله.'},
    'upgrade_lifetime': {'en': 'Get Lifetime Access', 'ar': 'احصل على وصول مدى الحياة', 'eg': 'اشترك مدى الحياة'},
    'tag_popular': {'en': 'Most Popular', 'ar': 'الأكثر شعبية', 'eg': 'الأكثر طلباً'},
    'tag_best_value': {'en': 'Best Value', 'ar': 'أفضل قيمة', 'eg': 'اللقطة'},
    'start_free_trial': {'en': 'Start Free Trial', 'ar': 'ابدأ فترة تجريبية مجانية', 'eg': 'جرب ببلاش الأول'},
    'unlock_premium_now': {'en': 'Unlock Premium Now', 'ar': 'افتح النسخة الاحترافية الآن', 'eg': 'افتح البرو دلوقتي'}
}

files = {
    'en': 'app/src/main/res/values/strings.xml',
    'ar': 'app/src/main/res/values-ar/strings.xml',
    'eg': 'app/src/main/res/values-ar-rEG/strings.xml'
}

for lang, path in files.items():
    try:
        tree = ET.parse(path)
        root = tree.getroot()
        
        # Add strings
        for key, vals in strings.items():
            # Check if exists
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
                
        # Format XML string slightly
        tree.write(path, encoding='utf-8', xml_declaration=True)
        print(f"Updated {path}")
    except Exception as e:
        print(f"Failed to update {path}: {e}")

