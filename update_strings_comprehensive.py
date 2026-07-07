import os

def update_strings(filepath, new_strings):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Remove duplicates if they exist in new_strings
    existing_keys = []
    import re
    matches = re.findall(r'name="([^"]+)"', content)
    existing_keys.extend(matches)
    
    filtered_new_strings = []
    for line in new_strings.strip().split('\n'):
        if 'name="' in line:
            key = re.search(r'name="([^"]+)"', line).group(1)
            if key not in existing_keys:
                filtered_new_strings.append(line)
    
    if filtered_new_strings:
        content = content.replace('</resources>', '\n' + '\n'.join(filtered_new_strings) + '\n</resources>')
        with open(filepath, 'w') as f:
            f.write(content)

en_strings = """
    <string name="loans_payoffs_title">Loans &amp; Payoffs</string>
    <string name="keep_tabs_subtitle">Keep tabs on who owes who</string>
    <string name="owed_to_me">OWED TO ME</string>
    <string name="i_owe">I OWE</string>
    <string name="net_ledger">NET LEDGER</string>
    <string name="search_person_notes">Search person or notes...</string>
    <string name="add_record">Add Record</string>
    <string name="active_tab">Active</string>
    <string name="paid_off_tab">Paid Off</string>
    <string name="show_all_tab">Show All</string>
    <string name="no_records_found">No transaction records found</string>
    <string name="log_lent_borrowed_desc">Log who you lent to or who you borrowed from to populate your ledger.</string>
    <string name="due_date">Due %1$s</string>
    <string name="open_ended">Open-Ended</string>
    <string name="details_badge">DETAILS</string>
    <string name="new_ledger_entry">NEW LEDGER ENTRY</string>
    <string name="fill_all_details">Fill all details</string>
    <string name="transaction_amount_label">TRANSACTION AMOUNT</string>
    <string name="i_lent">I Lent</string>
    <string name="i_borrowed">I Borrowed</string>
    <string name="contact_details_label">CONTACT DETAILS</string>
    <string name="contact_name_label">Contact Name</string>
    <string name="mike_smith_placeholder">e.g. Mike Smith</string>
    <string name="pick_contact_desc">Pick Contact</string>
    <string name="phone_optional">Phone (Optional)</string>
    <string name="phone_placeholder">e.g. +1 555...</string>
    <string name="social_optional">Social (Optional)</string>
    <string name="social_placeholder">e.g. email / handle</string>
    <string name="flashback_note_required">Flashback Note (Required)</string>
    <string name="flashback_placeholder">Why did the money change hands? Recall details easily later...</string>
    <string name="repayment_deadline">Repayment Deadline?</string>
    <string name="confirm_btn">Confirm</string>
    <string name="cancel_btn">Cancel</string>
    <string name="record_to_ledger_btn">RECORD TO LEDGER</string>
    <string name="contact_name_header">CONTACT NAME</string>
    <string name="repayment_deadline_header">REPAYMENT DEADLINE</string>
    <string name="no_strict_deadline">Open-Ended (No strict deadline)</string>
    <string name="flashback_recall_note">FLASHBACK RECALL NOTE</string>
    <string name="mark_as_paid_off">MARK AS PAID OFF &amp; SETTLE</string>
    <string name="delete_confirm_msg">Are you absolutely sure you want to delete?</string>
    <string name="yes_btn">Yes</string>
    <string name="no_btn">No</string>
    <string name="delete_record">Delete Record</string>
    <string name="budgeting_title">BUDGETING</string>
    <string name="total_balance_label">TOTAL BALANCE</string>
    <string name="extra_amount">+$$%1$s EXTRA</string>
    <string name="completed_status">COMPLETED</string>
    <string name="in_progress_status">IN PROGRESS</string>
    <string name="your_budget_title">YOUR BUDGET</string>
    <string name="remaining_left">$$%1$s left</string>
    <string name="goal_reached_status">Goal reached</string>
    <string name="add_deposit">Add Deposit</string>
    <string name="goal_completed_msg">Goal Completed</string>
    <string name="overview_tab">Overview</string>
    <string name="history_tab">History</string>
    <string name="growth_savings_subtitle">GROW YOUR SAVINGS BALANCE</string>
    <string name="deposit_amount_label">DEPOSIT AMOUNT</string>
    <string name="zero_amount_placeholder">$$ 0.00</string>
    <string name="note_required_label">NOTE (REQUIRED)</string>
    <string name="monthly_contribution_placeholder">e.g. Monthly contribution</string>
    <string name="confirm_deposit_btn">Confirm Deposit</string>
    <string name="established_date_label">ESTABLISHED DATE</string>
    <string name="days_since_start_label">DAYS SINCE START</string>
    <string name="days_count">%1$d days</string>
    <string name="avg_daily_saving_label">AVG. DAILY SAVING</string>
    <string name="est_completion_date_label">EST. COMPLETION DATE</string>
    <string name="goal_reached_success">Goal Reached!</string>
    <string name="savings_challenge_title">Savings Challenge</string>
    <string name="milestones_title">Milestones</string>
    <string name="contributions_count_msg">You\'ve made %1$d contributions so far. Keep it up!</string>
    <string name="deposit_tx_note">Deposit</string>
    <string name="target_goal_title">Target Goal</string>
    <string name="target_goal_desc">Save for a specific target amount.</string>
    <string name="open_savings_desc">Deposit just for saving (No target limit).</string>
    <string name="open_savings_hint">💡 Hint: Opened balance is for general/indefinite savings. You can deposit money here anytime for savings without setting any specific limit.</string>
    <string name="egyptian">Egyptian Arabic</string>
"""

ar_strings = """
    <string name="loans_payoffs_title">القروض والمدفوعات</string>
    <string name="keep_tabs_subtitle">تتبع من يدين لمن</string>
    <string name="owed_to_me">مستحق لي</string>
    <string name="i_owe">أنا مدين</string>
    <string name="net_ledger">صافي الدفتر</string>
    <string name="search_person_notes">البحث عن شخص أو ملاحظات...</string>
    <string name="add_record">إضافة سجل</string>
    <string name="active_tab">نشط</string>
    <string name="paid_off_tab">تم السداد</string>
    <string name="show_all_tab">عرض الكل</string>
    <string name="no_records_found">لم يتم العثور على سجلات معاملات</string>
    <string name="log_lent_borrowed_desc">سجل من أقرضته أو من اقترضت منه لتعبئة دفترك.</string>
    <string name="due_date">يستحق في %1$s</string>
    <string name="open_ended">مفتوح</string>
    <string name="details_badge">التفاصيل</string>
    <string name="new_ledger_entry">إدخال دفتر جديد</string>
    <string name="fill_all_details">املأ جميع التفاصيل</string>
    <string name="transaction_amount_label">مبلغ المعاملة</string>
    <string name="i_lent">أنا أقرضت</string>
    <string name="i_borrowed">أنا اقترضت</string>
    <string name="contact_details_label">تفاصيل الاتصال</string>
    <string name="contact_name_label">اسم جهة الاتصال</string>
    <string name="mike_smith_placeholder">مثال: محمد علي</string>
    <string name="pick_contact_desc">اختر جهة اتصال</string>
    <string name="phone_optional">الهاتف (اختياري)</string>
    <string name="phone_placeholder">مثال: +20 10...</string>
    <string name="social_optional">التواصل الاجتماعي (اختياري)</string>
    <string name="social_placeholder">مثال: البريد الإلكتروني / الحساب</string>
    <string name="flashback_note_required">ملاحظة الاستذكار (مطلوب)</string>
    <string name="flashback_placeholder">لماذا تغير المال؟ تذكر التفاصيل بسهولة لاحقاً...</string>
    <string name="repayment_deadline">موعد السداد؟</string>
    <string name="confirm_btn">تأكيد</string>
    <string name="cancel_btn">إلغاء</string>
    <string name="record_to_ledger_btn">تسجيل في الدفتر</string>
    <string name="contact_name_header">اسم جهة الاتصال</string>
    <string name="repayment_deadline_header">موعد السداد</string>
    <string name="no_strict_deadline">مفتوح (لا يوجد موعد محدد)</string>
    <string name="flashback_recall_note">ملاحظة الاستذكار</string>
    <string name="mark_as_paid_off">وضع علامة كمسدد وتسوية</string>
    <string name="delete_confirm_msg">هل أنت متأكد تماماً من رغبتك في الحذف؟</string>
    <string name="yes_btn">نعم</string>
    <string name="no_btn">لا</string>
    <string name="delete_record">حذف السجل</string>
    <string name="budgeting_title">الميزانية</string>
    <string name="total_balance_label">إجمالي الرصيد</string>
    <string name="extra_amount">+$$%1$s إضافي</string>
    <string name="completed_status">اكتمل</string>
    <string name="in_progress_status">قيد التنفيذ</string>
    <string name="your_budget_title">ميزانيتك</string>
    <string name="remaining_left">متبقي $$%1$s</string>
    <string name="goal_reached_status">تم الوصول للهدف</string>
    <string name="add_deposit">إضافة إيداع</string>
    <string name="goal_completed_msg">اكتمل الهدف</string>
    <string name="overview_tab">نظرة عامة</string>
    <string name="history_tab">السجل</string>
    <string name="growth_savings_subtitle">نمِّ رصيد مدخراتك</string>
    <string name="deposit_amount_label">مبلغ الإيداع</string>
    <string name="zero_amount_placeholder">$$ 0.00</string>
    <string name="note_required_label">ملاحظة (مطلوب)</string>
    <string name="monthly_contribution_placeholder">مثال: المساهمة الشهرية</string>
    <string name="confirm_deposit_btn">تأكيد الإيداع</string>
    <string name="established_date_label">تاريخ التأسيس</string>
    <string name="days_since_start_label">أيام منذ البداية</string>
    <string name="days_count">%1$d يوم</string>
    <string name="avg_daily_saving_label">متوسط الادخار اليومي</string>
    <string name="est_completion_date_label">تاريخ الاكتمال المتوقع</string>
    <string name="goal_reached_success">تم تحقيق الهدف!</string>
    <string name="savings_challenge_title">تحدي الادخار</string>
    <string name="milestones_title">الإنجازات</string>
    <string name="contributions_count_msg">لقد قمت بإجراء %1$d مساهمة حتى الآن. استمر!</string>
    <string name="deposit_tx_note">إيداع</string>
    <string name="target_goal_title">هدف محدد</string>
    <string name="target_goal_desc">ادخر لمبلغ محدد.</string>
    <string name="open_savings_desc">إيداع لمجرد الادخار (لا يوجد حد).</string>
    <string name="open_savings_hint">💡 تلميح: الرصيد المفتوح هو للمدخرات العامة. يمكنك إيداع المال هنا في أي وقت دون تحديد حد معين.</string>
    <string name="egyptian">العربية المصرية</string>
"""

eg_strings = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">دفتر الحصالة</string>
    <string name="language">اللغة</string>
    <string name="select_language">اختار اللغة</string>
    <string name="choose_language_desc">اختار لغتك المفضلة عشان تكمل</string>
    <string name="english">English</string>
    <string name="arabic">العربية (الفصحى)</string>
    <string name="egyptian">العامية المصرية</string>
    <string name="continue_btn">كمل</string>
    <string name="get_started">ابدأ دلوقتي</string>
    <string name="settings">الإعدادات والتفضيلات</string>
    <string name="backup_data">نسخة احتياطية</string>
    <string name="restore_data">ترجيع البيانات</string>
    <string name="rate_app">قيم التطبيق</string>
    <string name="give_feedback">قول رأيك</string>
    <!-- Onboarding -->
    <string name="onboarding_welcome_to">أهلاً بيك في</string>
    <string name="onboarding_subtitle_1">طريقتك الحديثة عشان تحوش وتجمع مواردك\\nمع أصحابك وعيلتك.</string>
    <string name="onboarding_pool_your">جمع</string>
    <string name="onboarding_savings">تحويشتك</string>
    <string name="onboarding_subtitle_2">اعمل أهداف مشتركة واعزم غيرك يشاركوا.\\nسواء لرحلة، أو هدية، أو\\nمشروع جماعي.</string>
    <string name="onboarding_track">تابع</string>
    <string name="onboarding_progress">التقدم</string>
    <string name="onboarding_together">مع بعض</string>
    <string name="onboarding_subtitle_3">تحديثات لحظية للمشاركات واكتمال الأهداف.\\nخليك متحمس وأنت شايف شريط\\nالتقدم بيملا.</string>
    <string name="onboarding_ready_to">مستعد</string>
    <string name="onboarding_start">تبدأ</string>
    <string name="onboarding_subtitle_4">يلا نجهز أول هدف ليك ونبدأ\\nرحلتك لادخار جماعي أذكى.</string>
    <string name="back_btn">رجوع</string>
    
    <!-- Dashboard & UI -->
    <string name="welcome_back">نورت تاني!</string>
    <string name="dashboard_title">لوحة التحكم</string>
    <string name="your_balance">رصيدك</string>
    <string name="recent_activity">آخر العمليات</string>
    <string name="no_recent_activity">مفيش عمليات قريبة</string>
    <string name="view_all">شوف الكل</string>
    <string name="go_straight_dashboard">روح للوحة التحكم علطول ←</string>
    
    <!-- Bottom Nav -->
    <string name="nav_home">الرئيسية</string>
    <string name="nav_goals">الأهداف</string>
    <string name="nav_loans">القروض</string>
    <string name="nav_settings">الإعدادات</string>
    
    <string name="welcome_to_circle">أهلاً بيك في دايرتك</string>
    <string name="choose_saving_journey">اختار عايز تبدأ رحلة التحويش إزاي.</string>
    <string name="start_new_goal">ابدأ هدف جديد</string>
    <string name="set_target">حدد رقم هدف.</string>
    <string name="payoffs_loans">المديونات والقروض</string>
    <string name="manage_loans">ظبط مين ليه فلوس ومين عليه.</string>
    <string name="community_feedback">رأي المجتمع</string>
    <string name="join_community_board">شارك في لوحة المجتمع</string>
    <string name="help_improve">ساعدنا نحسن دفتر الحصالة! دوس تحت عشان تطلب مميزات جديدة.</string>
    <string name="rate_app_title">قيم التطبيق</string>
    <string name="enjoying_piggy_ledger">مبسوط بالحصالة؟ دوس على النجوم عشان تقيم تجربتك.</string>
    <string name="backup_data_title">نسخة احتياطية للبيانات</string>
    <string name="secure_local_export">تصدير محلي آمن</string>
    <string name="save_goals_desc">احفظ أهدافك وسجلاتك في ملف JSON.</string>
    <string name="restore_data_title">ترجيع البيانات</string>
    <string name="import_json_backup">استيراد نسخة JSON</string>
    <string name="restoring_data_replace">ترجيع البيانات هيمسح الأهداف والسجلات اللي موجودة دلوقتي.</string>
    <string name="start_first_goal">ابدأ أول هدف ليك النهاردة!</string>
    <string name="open_savings">تحويش مفتوح (عام)</string>
    <string name="target">الهدف:</string>
    
    <string name="loans_payoffs_title">القروض والمديونات</string>
    <string name="keep_tabs_subtitle">خليك عارف مين ليه ومين عليه</string>
    <string name="owed_to_me">ليا فلوس</string>
    <string name="i_owe">عليا فلوس</string>
    <string name="net_ledger">صافي الدفتر</string>
    <string name="search_person_notes">دور على حد أو ملاحظة...</string>
    <string name="add_record">إضافة سجل</string>
    <string name="active_tab">شغال</string>
    <string name="paid_off_tab">خلصان</string>
    <string name="show_all_tab">كله</string>
    <string name="no_records_found">مفيش سجلات معاملات</string>
    <string name="log_lent_borrowed_desc">سجل اللي سلفته أو اللي استلفته عشان تملى دفترك.</string>
    <string name="due_date">ميعاده في %1$s</string>
    <string name="open_ended">مفتوح</string>
    <string name="details_badge">تفاصيل</string>
    <string name="new_ledger_entry">إضافة في الدفتر</string>
    <string name="fill_all_details">املأ كل البيانات</string>
    <string name="transaction_amount_label">مبلغ العملية</string>
    <string name="i_lent">أنا سلفت</string>
    <string name="i_borrowed">أنا استلفت</string>
    <string name="contact_details_label">بيانات التواصل</string>
    <string name="contact_name_label">اسم الشخص</string>
    <string name="mike_smith_placeholder">مثلاً: أحمد محمد</string>
    <string name="pick_contact_desc">اختار حد من الأسماء</string>
    <string name="phone_optional">الموبايل (اختياري)</string>
    <string name="phone_placeholder">مثلاً: 010...</string>
    <string name="social_optional">تواصل اجتماعي (اختياري)</string>
    <string name="social_placeholder">مثلاً: إيميل أو يوزر نيم</string>
    <string name="flashback_note_required">ملاحظة عشان تفتكر (مهم)</string>
    <string name="flashback_placeholder">الفلوس دي بتاعة إيه؟ عشان تفتكرها بعدين بسهولة...</string>
    <string name="repayment_deadline">ميعاد السداد؟</string>
    <string name="confirm_btn">تأكيد</string>
    <string name="cancel_btn">إلغاء</string>
    <string name="record_to_ledger_btn">تسجيل في الدفتر</string>
    <string name="contact_name_header">اسم الشخص</string>
    <string name="repayment_deadline_header">ميعاد السداد</string>
    <string name="no_strict_deadline">مفتوح (مفيش ميعاد محدد)</string>
    <string name="flashback_recall_note">ملاحظة عشان تفتكر</string>
    <string name="mark_as_paid_off">سدد وسوي الحساب</string>
    <string name="delete_confirm_msg">متأكد إنك عايز تمسحه؟</string>
    <string name="yes_btn">أيوة</string>
    <string name="no_btn">لأ</string>
    <string name="delete_record">مسح السجل</string>
    <string name="budgeting_title">الميزانية</string>
    <string name="total_balance_label">إجمالي الرصيد</string>
    <string name="extra_amount">+$$%1$s زيادة</string>
    <string name="completed_status">خلص</string>
    <string name="in_progress_status">شغال</string>
    <string name="your_budget_title">ميزانيتك</string>
    <string name="remaining_left">فاضل $$%1$s</string>
    <string name="goal_reached_status">وصلت للهدف</string>
    <string name="add_deposit">زود فلوس</string>
    <string name="goal_completed_msg">الهدف خلص</string>
    <string name="overview_tab">نظرة عامة</string>
    <string name="history_tab">السجل</string>
    <string name="growth_savings_subtitle">كبر رصيد تحويشتك</string>
    <string name="deposit_amount_label">مبلغ الإيداع</string>
    <string name="zero_amount_placeholder">$$ 0.00</string>
    <string name="note_required_label">ملاحظة (مهمة)</string>
    <string name="monthly_contribution_placeholder">مثلاً: تحويشة الشهر</string>
    <string name="confirm_deposit_btn">أكد الإيداع</string>
    <string name="established_date_label">تاريخ البداية</string>
    <string name="days_since_start_label">أيام من البداية</string>
    <string name="days_count">%1$d يوم</string>
    <string name="avg_daily_saving_label">متوسط التحويش في اليوم</string>
    <string name="est_completion_date_label">ميعاد الخلاص المتوقع</string>
    <string name="goal_reached_success">حققت الهدف!</string>
    <string name="savings_challenge_title">تحدي التحويش</string>
    <string name="milestones_title">الإنجازات</string>
    <string name="contributions_count_msg">أنت عملت %1$d مشاركة لحد دلوقتي. عاش!</string>
    <string name="deposit_tx_note">إيداع</string>
    <string name="target_goal_title">هدف محدد</string>
    <string name="target_goal_desc">حوش لمبلغ معين.</string>
    <string name="open_savings_desc">إيداع للتحويش بس (من غير حد).</string>
    <string name="open_savings_hint">💡 ملحوظة: الرصيد المفتوح للتحويش العام. تقدر تحط فلوس هنا في أي وقت من غير ما تحدد سقف معين.</string>
    
    <string name="my_goals">أهدافي</string>
    <string name="no_goals_yet">مفيش أهداف لسه. ضيف واحد!</string>
    <string name="add_goal">ضيف هدف</string>
    <string name="new_goal">هدف جديد</string>
    <string name="new_goal_subtitle">إيه خطتك الكبيرة؟ ظبطها هنا.</string>
    <string name="what_are_you_saving_for">بتحوش عشان إيه؟</string>
    <string name="goal_name_placeholder">مثلاً: رحلة أحلامك، لابتوب جديد</string>
    <string name="goal_type">نوع الهدف</string>
    <string name="how_much_do_you_need">محتاج كام؟ ($)</string>
    <string name="lets_get_saving">يلا نحوش!</string>
</resources>
"""

os.makedirs('app/src/main/res/values-ar-rEG', exist_ok=True)
with open('app/src/main/res/values-ar-rEG/strings.xml', 'w') as f:
    f.write(eg_strings)

update_strings('app/src/main/res/values/strings.xml', en_strings)
update_strings('app/src/main/res/values-ar/strings.xml', ar_strings)

# Update locales_config.xml
locales_config_path = 'app/src/main/res/xml/locales_config.xml'
if os.path.exists(locales_config_path):
    with open(locales_config_path, 'r') as f:
        config = f.read()
    if 'ar-EG' not in config:
        config = config.replace('</locale-config>', '   <locale android:name="ar-EG"/>\n</locale-config>')
        with open(locales_config_path, 'w') as f:
            f.write(config)

