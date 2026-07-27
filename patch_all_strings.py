import re

# 1. values/strings.xml
with open('./app/src/main/res/values/strings.xml', 'r') as f:
    en_content = f.read()

en_additions = """
    <string name="piggy_ledger_brand">Piggy Ledger</string>
    <string name="onboarding_ai_thinking">Thinking...</string>
    <string name="onboarding_ai_sketching">Sketching...</string>
    <string name="onboarding_ai_making_plan">Making Plan...</string>
    <string name="onboarding_ai_syncing">syncing</string>
    <string name="onboarding_saving_rate_label">Saving Rate: %1$s</string>
    <string name="onboarding_step_workspace_title">Workspace Configured: %1$s</string>
    <string name="onboarding_step_workspace_desc_personal">Private vault configured to keep your core balance safe.</string>
    <string name="onboarding_step_workspace_desc_loans">Tailored to track lent/borrowed cash and deadlines.</string>
    <string name="onboarding_step_workspace_desc_auto">Prepared to automatically organize incoming receipts.</string>
    <string name="onboarding_step_intensity_title">Savings Rate Configured: %1$s (%2$s)</string>
    <string name="onboarding_step_intensity_desc_casual">A light, steady habit to build savings without strain.</string>
    <string name="onboarding_step_intensity_desc_balanced">The perfect pace for hitting your major milestones.</string>
    <string name="onboarding_step_intensity_desc_aggressive">High-velocity savings mode to smash buffers in record time.</string>
    <string name="onboarding_step_sync_title">Automation Assistant</string>
    <string name="onboarding_step_sync_desc_granted">Vodafone Cash, Orange Cash, and bank alert SMS parsed automatically!</string>
    <string name="onboarding_step_sync_desc_manual">Log transactions manually. Enable SMS auto-sync anytime in Settings.</string>
    <string name="onboarding_step_milestone_title">Your First Major Goal</string>
    <string name="onboarding_step_milestone_desc_personal">Create a vault and set a deposit to start your streak!</string>
    <string name="onboarding_step_milestone_desc_loans">Log your first lent/borrowed deal to see net dues.</string>
    <string name="onboarding_step_milestone_desc_auto">Complete a transfer and let our automation handle it.</string>
    <string name="onboarding_choose_pace_title">Choose Your Pace</string>
    <string name="onboarding_choose_pace_subtitle">Decide how you want to reach your financial milestones.</string>
"""

# Update how_did_you_hear_about_us in EN
en_content = re.sub(
    r'<string name="how_did_you_hear_about_us">.*?</string>',
    '<string name="how_did_you_hear_about_us">How did you hear about %1$s?</string>',
    en_content
)
en_content = en_content.replace('</resources>', en_additions + '</resources>')

with open('./app/src/main/res/values/strings.xml', 'w') as f:
    f.write(en_content)


# 2. values-ar/strings.xml
with open('./app/src/main/res/values-ar/strings.xml', 'r') as f:
    ar_content = f.read()

ar_additions = """
    <string name="piggy_ledger_brand">دفتر الحصالة</string>
    <string name="onboarding_ai_thinking">جاري التفكير...</string>
    <string name="onboarding_ai_sketching">جاري الرسم والتخطيط...</string>
    <string name="onboarding_ai_making_plan">جاري إعداد الخطة...</string>
    <string name="onboarding_ai_syncing">جاري المزامنة</string>
    <string name="onboarding_saving_rate_label">معدل الادخار: %1$s</string>
    <string name="onboarding_step_workspace_title">تم تهيئة مساحة العمل: %1$s</string>
    <string name="onboarding_step_workspace_desc_personal">خزينة خاصة لحفظ رصيدك الأساسي بأمان.</string>
    <string name="onboarding_step_workspace_desc_loans">مخصصة لمتابعة الأموال المعارة والمستدانة ومواعيد تسديدها.</string>
    <string name="onboarding_step_workspace_desc_auto">جاهزة لتنظيم الإيصالات الواردة والمعاملات تلقائياً.</string>
    <string name="onboarding_step_intensity_title">تم ضبط معدل الادخار: %1$s (%2$s)</string>
    <string name="onboarding_step_intensity_desc_casual">عادة خفيفة ومستمرة لبناء مدخراتك دون إجهاد.</string>
    <string name="onboarding_step_intensity_desc_balanced">الوتيرة المثالية لتحقيق أهدافك المالية الكبرى.</string>
    <string name="onboarding_step_intensity_desc_aggressive">وضع ادخار سريع للغاية لبناء مدخراتك في وقت قياسي.</string>
    <string name="onboarding_step_sync_title">مساعد الأتمتة</string>
    <string name="onboarding_step_sync_desc_granted">قراءة ورصد رسائل فودافون كاش، أورنج كاش، وإشعارات البنوك تلقائياً!</string>
    <string name="onboarding_step_sync_desc_manual">تسجيل المعاملات يدوياً. يمكنك تفعيل المزامنة التلقائية عبر الرسائل لاحقاً من الإعدادات.</string>
    <string name="onboarding_step_milestone_title">هدفك المالي الأول</string>
    <string name="onboarding_step_milestone_desc_personal">أنشئ خزينة وحدد مبلغ إيداع للبدء في سلسلة الادخار!</string>
    <string name="onboarding_step_milestone_desc_loans">سجل أول معاملة إقراض أو استدانة لمتابعة صافي المبالغ المستحقة.</string>
    <string name="onboarding_step_milestone_desc_auto">أكمل أول تحويل ودع أتمتة التطبيق تتكفل بالباقي.</string>
    <string name="onboarding_choose_pace_title">اختر سرعتك</string>
    <string name="onboarding_choose_pace_subtitle">حدد الكيفية والسرعة التي تريد بها تحقيق أهدافك المالية.</string>
"""

ar_content = re.sub(
    r'<string name="how_did_you_hear_about_us">.*?</string>',
    '<string name="how_did_you_hear_about_us">كيف سمعت عن %1$s؟</string>',
    ar_content
)
ar_content = ar_content.replace('</resources>', ar_additions + '</resources>')

with open('./app/src/main/res/values-ar/strings.xml', 'w') as f:
    f.write(ar_content)


# 3. values-ar-rEG/strings.xml
with open('./app/src/main/res/values-ar-rEG/strings.xml', 'r') as f:
    eg_content = f.read()

eg_additions = """
    <string name="piggy_ledger_brand">دفتر الحصالة</string>
    <string name="onboarding_ai_thinking">بفكر...</string>
    <string name="onboarding_ai_sketching">برسم الخطة...</string>
    <string name="onboarding_ai_making_plan">بجهز لك الخطة...</string>
    <string name="onboarding_ai_syncing">بيتزامن</string>
    <string name="onboarding_saving_rate_label">معدل التوفير: %1$s</string>
    <string name="onboarding_step_workspace_title">جهّزنا لك مساحة العمل: %1$s</string>
    <string name="onboarding_step_workspace_desc_personal">خزنة خاصة عشان تحافظ على فلوسك وأمان حسابك.</string>
    <string name="onboarding_step_workspace_desc_loans">متظبطة عشان تتابع الفلوس اللي ليك واللي عليك ومواعيد السداد.</string>
    <string name="onboarding_step_workspace_desc_auto">جاهزة عشان تتسجل المعاملات والإيصالات أول بأول تلقائياً.</string>
    <string name="onboarding_step_intensity_title">ظبطنا معدل التوفير: %1$s (%2$s)</string>
    <string name="onboarding_step_intensity_desc_casual">عادة خفيفة وسهلة عشان تحوش من غير ما تحس بضغط.</string>
    <string name="onboarding_step_intensity_desc_balanced">أحسن معدل يخليك توصل لأهدافك الكبيرة بسرعة.</string>
    <string name="onboarding_step_intensity_desc_aggressive">نظام توفير سريع جداً عشان تعمل قرشين محترمين في وقت قياسي.</string>
    <string name="onboarding_step_sync_title">مساعد المزامنة التلقائية</string>
    <string name="onboarding_step_sync_desc_granted">رسائل فودافون كاش، أورنج كاش، وإشعارات البنك بتتقري وتتسجل لوحدها!</string>
    <string name="onboarding_step_sync_desc_manual">سجل المعاملات بإيدك، وتقدر تفعل المزامنة التلقائية للرسائل في أي وقت من الإعدادات.</string>
    <string name="onboarding_step_milestone_title">أول هدف مالي ليك</string>
    <string name="onboarding_step_milestone_desc_personal">عملنا لك حصالة، ابتدي حوش فيها عشان تبدأ السلسلة!</string>
    <string name="onboarding_step_milestone_desc_loans">سجل أول ديون ليك أو عليك عشان تشوف صافي الحساب.</string>
    <string name="onboarding_step_milestone_desc_auto">اعمل أول تحويل وسيب السيستم يظبط كل حاجة لوحده.</string>
    <string name="onboarding_choose_pace_title">اختار السرعة اللي تناسبك</string>
    <string name="onboarding_choose_pace_subtitle">حدد الطريق والسرعة اللي تحب توصل بيها لأهدافك المالية.</string>
"""

eg_content = re.sub(
    r'<string name="how_did_you_hear_about_us">.*?</string>',
    '<string name="how_did_you_hear_about_us">عرفت %1$s منين؟</string>',
    eg_content
)
eg_content = eg_content.replace('</resources>', eg_additions + '</resources>')

with open('./app/src/main/res/values-ar-rEG/strings.xml', 'w') as f:
    f.write(eg_content)

print("Strings updated successfully!")
