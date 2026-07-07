import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add import if missing
    if 'import androidx.compose.ui.res.stringResource' not in content:
        content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport androidx.compose.ui.res.stringResource\nimport com.oryno.piggy_ledger.R')

    replacements = [
        ('Text("Loans & Payoffs",', 'Text(stringResource(R.string.loans_payoffs_title),'),
        ('Text("Keep tabs on who owes who",', 'Text(stringResource(R.string.keep_tabs_subtitle),'),
        ('Text("OWED TO ME",', 'Text(stringResource(R.string.owed_to_me),'),
        ('Text("I OWE",', 'Text(stringResource(R.string.i_owe),'),
        ('Text("NET LEDGER",', 'Text(stringResource(R.string.net_ledger),'),
        ('placeholder = { Text("Search person or notes...",', 'placeholder = { Text(stringResource(R.string.search_person_notes),'),
        ('Text("Add Record",', 'Text(stringResource(R.string.add_record),'),
        ('TabButton(text = "Active",', 'TabButton(text = stringResource(R.string.active_tab),'),
        ('TabButton(text = "Paid Off",', 'TabButton(text = stringResource(R.string.paid_off_tab),'),
        ('TabButton(text = "Show All",', 'TabButton(text = stringResource(R.string.show_all_tab),'),
        ('Text("No transaction records found",', 'Text(stringResource(R.string.no_records_found),'),
        ('Text("Log who you lent to or who you borrowed from to populate your ledger.",', 'Text(stringResource(R.string.log_lent_borrowed_desc),'),
        ('"Due ${sdf.format(java.util.Date(loan.deadline))}"', 'stringResource(R.string.due_date, sdf.format(java.util.Date(loan.deadline)))'),
        ('"Open-Ended"', 'stringResource(R.string.open_ended)'),
        ('text = "DETAILS",', 'text = stringResource(R.string.details_badge),'),
        ('text = "NEW LEDGER ENTRY",', 'text = stringResource(R.string.new_ledger_entry),'),
        ('text = "Fill all details",', 'text = stringResource(R.string.fill_all_details),'),
        ('text = "TRANSACTION AMOUNT",', 'text = stringResource(R.string.transaction_amount_label),'),
        ('text = "I Lent",', 'text = stringResource(R.string.i_lent),'),
        ('text = "I Borrowed",', 'text = stringResource(R.string.i_borrowed),'),
        ('text = "CONTACT DETAILS",', 'text = stringResource(R.string.contact_details_label),'),
        ('label = { Text("Contact Name") },', 'label = { Text(stringResource(R.string.contact_name_label)) },'),
        ('placeholder = { Text("e.g. Mike Smith") },', 'placeholder = { Text(stringResource(R.string.mike_smith_placeholder)) },'),
        ('contentDescription = "Pick Contact",', 'contentDescription = stringResource(R.string.pick_contact_desc),'),
        ('label = { Text("Phone (Optional)") },', 'label = { Text(stringResource(R.string.phone_optional)) },'),
        ('placeholder = { Text("e.g. +1 555...") },', 'placeholder = { Text(stringResource(R.string.phone_placeholder)) },'),
        ('label = { Text("Social (Optional)") },', 'label = { Text(stringResource(R.string.social_optional)) },'),
        ('placeholder = { Text("e.g. email / handle") },', 'placeholder = { Text(stringResource(R.string.social_placeholder)) },'),
        ('label = { Text("Flashback Note (Required)") },', 'label = { Text(stringResource(R.string.flashback_note_required)) },'),
        ('placeholder = { Text("Why did the money change hands? Recall details easily later...") },', 'placeholder = { Text(stringResource(R.string.flashback_placeholder)) },'),
        ('Text("Repayment Deadline?",', 'Text(stringResource(R.string.repayment_deadline),'),
        ('Text("Confirm",', 'Text(stringResource(R.string.confirm_btn),'),
        ('Text("Cancel",', 'Text(stringResource(R.string.cancel_btn),'),
        ('Text("RECORD TO LEDGER",', 'Text(stringResource(R.string.record_to_ledger_btn),'),
        ('Text("CONTACT NAME",', 'Text(stringResource(R.string.contact_name_header),'),
        ('Text("REPAYMENT DEADLINE",', 'Text(stringResource(R.string.repayment_deadline_header),'),
        ('Text("Open-Ended (No strict deadline)",', 'Text(stringResource(R.string.no_strict_deadline),'),
        ('Text("FLASHBACK RECALL NOTE",', 'Text(stringResource(R.string.flashback_recall_note),'),
        ('Text("MARK AS PAID OFF & SETTLE",', 'Text(stringResource(R.string.mark_as_paid_off),'),
        ('Text("Are you absolutely sure you want to delete?",', 'Text(stringResource(R.string.delete_confirm_msg),'),
        ('Text("Yes",', 'Text(stringResource(R.string.yes_btn),'),
        ('Text("No",', 'Text(stringResource(R.string.no_btn),'),
        ('Text("Delete Record",', 'Text(stringResource(R.string.delete_record),')
    ]

    for old, new in replacements:
        content = content.replace(old, new)

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
