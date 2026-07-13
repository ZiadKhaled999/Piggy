import re

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AccountsScreen.kt", "r") as f:
    content = f.read()

# Replace Dialog with BottomSheet
dialog_pattern = r"// EDIT BUDGET DIALOG.*?(?=// TRANSACTION DETAILS MODAL)"
dialog_replacement = """// EDIT BUDGET BOTTOM SHEET
    if (showEditBudgetDialog) {
        ModalBottomSheet(
            onDismissRequest = { showEditBudgetDialog = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.3f)) }
        ) {
            var newBudgetStr by remember { mutableStateOf(monthlyBudget.toString()) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.update_budget),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = newBudgetStr,
                    onValueChange = { newBudgetStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("${stringResource(R.string.budget_amount)} ($currencySymbol)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary,
                        focusedLabelColor = PinkPrimary,
                        cursorColor = PinkPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val parsed = newBudgetStr.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            viewModel.setMonthlyBudget(parsed)
                        }
                        showEditBudgetDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
"""

content = re.sub(dialog_pattern, dialog_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AccountsScreen.kt", "w") as f:
    f.write(content)
