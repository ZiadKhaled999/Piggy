import os
import re

def update_strings(filepath, new_entries):
    if not os.path.exists(filepath):
        # Create file if it doesn't exist (basic template)
        with open(filepath, 'w') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>')
    
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Simple regex to remove existing tags to avoid duplicates
    for key in new_entries.keys():
        content = re.sub(f'    <string name="{key}">.*</string>\n', '', content)
    
    new_xml_entries = ""
    for key, val in new_entries.items():
        # Escape single quotes and ampersands
        val = val.replace("'", "\\'").replace("&", "&amp;")
        new_xml_entries += f'    <string name="{key}">{val}</string>\n'
    
    if '</resources>' in content:
        content = content.replace('</resources>', new_xml_entries + '</resources>')
        with open(filepath, 'w') as f:
            f.write(content)

en_entries = {
    "piggy_ledger_logo": "Piggy Ledger Logo",
    "settings_icon": "Settings",
    "back_icon": "Back",
    "feedback_illustration": "Feedback Illustration",
    "rate_illustration": "Rate Illustration",
    "backup_illustration": "Backup Illustration",
    "restore_illustration": "Restore Illustration",
    "export_success": "Data exported successfully",
    "export_failed": "Export failed: %1$s",
    "restore_success": "Data restored successfully",
    "restore_failed": "Restore failed: %1$s",
    "read_file_failed": "Failed to read file: %1$s",
    "browser_error": "Could not open browser",
    "email_error": "No email client found",
    "selected_badge": "Selected",
    "amount_extra_simple": "%1$s EXTRA",
    "amount_saved_simple": "%1$s saved",
    "amount_left_simple": "%1$s left",
    "bar_chart_desc": "Bar Chart",
    "polar_chart_desc": "Polar Area Chart",
    "max_label": "Max",
    "start_saving_msg": "Start saving to see your progress chart!",
    "no_contributions_msg": "No contributions yet.",
    "repayment_deadline_over": "The repayment deadline for %1$s is over. Outstanding amount: %2$s.",
    "zero_amount": "0.00",
    "i_owe_this": "I OWE THIS",
    "i_owe": "I OWE",
    "owed_to_me": "OWED TO ME",
    "open_feedback_board": "Open Feedback Board",
    "send_rating": "Send Rating",
    "create_backup_file": "Create Backup File",
    "select_backup_file": "Select Backup File"
}

ar_entries = {
    "piggy_ledger_logo": "شعار حصالة",
    "settings_icon": "الإعدادات",
    "back_icon": "رجوع",
    "feedback_illustration": "توضيح الملاحظات",
    "rate_illustration": "توضيح التقييم",
    "backup_illustration": "توضيح النسخ الاحتياطي",
    "restore_illustration": "توضيح استعادة البيانات",
    "export_success": "تم تصدير البيانات بنجاح",
    "export_failed": "فشل التصدير: %1$s",
    "restore_success": "تم استعادة البيانات بنجاح",
    "restore_failed": "فشلت الاستعادة: %1$s",
    "read_file_failed": "فشل قراءة الملف: %1$s",
    "browser_error": "تعذر فتح المتصفح",
    "email_error": "لم يتم العثور على تطبيق بريد إلكتروني",
    "selected_badge": "محدد",
    "amount_extra_simple": "%1$s إضافي",
    "amount_saved_simple": "تم ادخار %1$s",
    "amount_left_simple": "متبقي %1$s",
    "bar_chart_desc": "رسم بياني شريطي",
    "polar_chart_desc": "رسم بياني قطبي",
    "max_label": "الأقصى",
    "start_saving_msg": "ابدأ في الادخار لرؤية مخطط التقدم الخاص بك!",
    "no_contributions_msg": "لا توجد مساهمات بعد.",
    "repayment_deadline_over": "انتهى موعد السداد لـ %1$s. المبلغ المستحق: %2$s.",
    "zero_amount": "0.00",
    "i_owe_this": "أنا مدين بهذا",
    "i_owe": "أنا مدين",
    "owed_to_me": "ليا فلوس",
    "open_feedback_board": "فتح لوحة الملاحظات",
    "send_rating": "إرسال التقييم",
    "create_backup_file": "إنشاء ملف نسخة احتياطية",
    "select_backup_file": "اختر ملف النسخة الاحتياطية"
}

eg_entries = {
    "piggy_ledger_logo": "لوجو الحصالة",
    "settings_icon": "الضبط",
    "back_icon": "رجوع",
    "feedback_illustration": "رسمة التعليقات",
    "rate_illustration": "رسمة التقييم",
    "backup_illustration": "رسمة النسخة الاحتياطية",
    "restore_illustration": "رسمة ترجيع البيانات",
    "export_success": "البيانات خرجت تمام",
    "export_failed": "التصدير باظ عشان: %1$s",
    "restore_success": "البيانات رجعت زي الفل",
    "restore_failed": "الترجيع فشل عشان: %1$s",
    "read_file_failed": "مش عارف أقرأ الملف: %1$s",
    "browser_error": "مش عارف أفتح المتصفح",
    "email_error": "مفيش برنامج إيميلات هنا",
    "selected_badge": "مختار",
    "amount_extra_simple": "%1$s زيادة",
    "amount_saved_simple": "محوش %1$s",
    "amount_left_simple": "فاضل %1$s",
    "bar_chart_desc": "رسم بياني عواميد",
    "polar_chart_desc": "رسم بياني دواير",
    "max_label": "أعلى حاجة",
    "start_saving_msg": "ابدأ حوش عشان تشوف الرسم البياني بتاعك!",
    "no_contributions_msg": "لسه مفيش مشاركات.",
    "repayment_deadline_over": "ميعاد سداد %1$s خلص. المبلغ اللي فاضل: %2$s.",
    "zero_amount": "0.00",
    "i_owe_this": "عليا الفلوس دي",
    "i_owe": "عليا فلوس للناس",
    "owed_to_me": "ليا فلوس بره",
    "open_feedback_board": "افتح لوحة الآراء",
    "send_rating": "ابعت التقييم",
    "create_backup_file": "اعمل ملف نسخة احتياطية",
    "select_backup_file": "اختار ملف النسخة الاحتياطية"
}

update_strings('app/src/main/res/values/strings.xml', en_entries)
update_strings('app/src/main/res/values-ar/strings.xml', ar_entries)
update_strings('app/src/main/res/values-ar-rEG/strings.xml', eg_entries)
