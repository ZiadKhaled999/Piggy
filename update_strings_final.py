import os
import re

def update_strings(filepath, new_entries):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Remove existing versions of the new entries if they exist
    import re
    for key in new_entries.keys():
        content = re.sub(f'    <string name="{key}">.*</string>\n', '', content)
    
    new_xml_entries = ""
    for key, val in new_entries.items():
        # Escape single quotes
        val = val.replace("'", "\\'")
        new_xml_entries += f'    <string name="{key}">{val}</string>\n'
    
    if '</resources>' in content:
        content = content.replace('</resources>', new_xml_entries + '</resources>')
        with open(filepath, 'w') as f:
            f.write(content)

en_entries = {
    "i_owe_this": "I OWE THIS",
    "growth_savings_subtitle": "GROW YOUR SAVINGS BALANCE"
}

ar_entries = {
    "i_owe_this": "أنا مدين بهذا",
    "growth_savings_subtitle": "نمِّ رصيد مدخراتك"
}

eg_entries = {
    "i_owe_this": "عليا الفلوس دي",
    "growth_savings_subtitle": "كبر تحويشتك قوام",
    "owed_to_me": "ليا فلوس بره",
    "i_owe": "عليا فلوس للناس",
    "loans_payoffs_title": "السلف والديون",
    "add_record": "ضيف سلفة جديدة",
    "new_ledger_entry": "سجل جديد في الدفتر",
    "go_straight_dashboard": "خش على الحساب قوام ←",
    "target_goal_title": "خطة تحويش",
    "open_savings": "حصالة مفتوحة",
    "milestones_title": "وصلت لفين؟",
    "deposit_amount_label": "هتحط كام؟"
}

update_strings('app/src/main/res/values/strings.xml', en_entries)
update_strings('app/src/main/res/values-ar/strings.xml', ar_entries)
update_strings('app/src/main/res/values-ar-rEG/strings.xml', eg_entries)
