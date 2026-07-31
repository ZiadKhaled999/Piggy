import re

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                        com.revenuecat.purchases.Purchases.sharedInstance.purchase(
                            activity,
                            packageToBuy,
                            object : com.revenuecat.purchases.interfaces.PurchaseCallback {"""
                            
replacement = """                        com.revenuecat.purchases.Purchases.sharedInstance.purchase(
                            com.revenuecat.purchases.PurchaseParams.Builder(activity, packageToBuy).build(),
                            object : com.revenuecat.purchases.interfaces.PurchaseCallback {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
