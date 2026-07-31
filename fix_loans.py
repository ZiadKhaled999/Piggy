with open('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt', 'r') as f:
    content = f.read()
with open('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt', 'w') as f:
    f.write("import androidx.compose.material.icons.filled.AttachMoney\nimport androidx.compose.material.icons.filled.ReceiptLong\nimport androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight\n" + content)
