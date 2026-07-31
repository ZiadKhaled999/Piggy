import re

# 1. Update SettingsScreen.kt
with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

# Update getCustomerInfo check in SettingsScreen line 1355
content = content.replace(
    'val active = info.entitlements["Piggy Ledger Pro"]?.isActive == true',
    'val active = info.entitlements.all.values.any { it.isActive } || info.entitlements["Piggy Ledger Pro"]?.isActive == true'
)

# Update PiggyLedgerPaywall
paywall_code = """
fun PiggyLedgerPaywall(
    viewModel: PiggyLedgerViewModel,
    onPurchaseSuccess: (com.revenuecat.purchases.CustomerInfo?) -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var offerings: com.revenuecat.purchases.Offerings? by remember { mutableStateOf(null) }
    var isLoadingOfferings by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var selectedPlan by remember { mutableStateOf(PaywallPlan.YEARLY) }
    var isPurchasing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.revenuecat.purchases.Purchases.sharedInstance.getOfferings(
            object : com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                override fun onReceived(offeringsResult: com.revenuecat.purchases.Offerings) {
                    isLoadingOfferings = false
                    offerings = offeringsResult
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    isLoadingOfferings = false
                    fetchError = error.message
                    android.util.Log.e("Paywall", "Error fetching offerings: ${error.message}")
                }
            }
        )
    }

    val packagesList = remember(offerings) {
        val current = offerings?.current?.availablePackages
        if (!current.isNullOrEmpty()) {
            current
        } else {
            offerings?.all?.values?.flatMap { it.availablePackages } ?: emptyList()
        }
    }

    val monthlyPackage = packagesList.find { 
        it.packageType == com.revenuecat.purchases.PackageType.MONTHLY ||
        it.identifier.contains("month", ignoreCase = true) ||
        it.product.id.contains("month", ignoreCase = true)
    } ?: packagesList.getOrNull(0)

    val yearlyPackage = packagesList.find { 
        it.packageType == com.revenuecat.purchases.PackageType.ANNUAL ||
        it.identifier.contains("year", ignoreCase = true) ||
        it.identifier.contains("annual", ignoreCase = true) ||
        it.product.id.contains("year", ignoreCase = true) ||
        it.product.id.contains("annual", ignoreCase = true)
    } ?: packagesList.getOrNull(1)

    val lifetimePackage = packagesList.find { 
        it.packageType == com.revenuecat.purchases.PackageType.LIFETIME ||
        it.identifier.contains("life", ignoreCase = true) ||
        it.identifier.contains("lt", ignoreCase = true) ||
        it.product.id.contains("life", ignoreCase = true) ||
        it.product.id.contains("lt", ignoreCase = true)
    } ?: packagesList.getOrNull(2)

    val planMeta = when (selectedPlan) {
        PaywallPlan.MONTHLY -> PlanMetadata(
            tabLabel = "Monthly",
            badgeName = "Monthly",
            headerSubtitle = "Keep tracking with expanded access & unlimited control",
            priceText = monthlyPackage?.product?.price?.formatted ?: "$9.99 / mo",
            renewalCaption = "Renews for ${monthlyPackage?.product?.price?.formatted ?: "$9.99"}/month. Cancel anytime.",
            ctaText = "Upgrade Monthly",
            accentColor = Color(0xFF2563EB)
        )
        PaywallPlan.YEARLY -> PlanMetadata(
            tabLabel = "Yearly",
            badgeName = "Yearly",
            headerSubtitle = "Get full access with advanced intelligence & complete analytics",
            priceText = yearlyPackage?.product?.price?.formatted ?: "$99.99 / yr",
            renewalCaption = "Renews for ${yearlyPackage?.product?.price?.formatted ?: "$99.99"}/year. Cancel anytime.",
            ctaText = "Upgrade Yearly",
            tag = "SAVE 17%",
            accentColor = Color(0xFF7C3AED)
        )
        PaywallPlan.LIFETIME -> PlanMetadata(
            tabLabel = "Lifetime",
            badgeName = "Lifetime",
            headerSubtitle = "Unlock lifetime unlimited access & all future features",
            priceText = lifetimePackage?.product?.price?.formatted ?: "$299.99",
            renewalCaption = "One-time payment of ${lifetimePackage?.product?.price?.formatted ?: "$299.99"}. No renewal or hidden fees.",
            ctaText = "Upgrade Lifetime",
            tag = "BEST VALUE",
            accentColor = PinkPrimary
        )
    }
"""

content = re.sub(
    r'fun PiggyLedgerPaywall\(.*?val planMeta = when \(selectedPlan\) \{.*?\n    }', 
    paywall_code.strip(), 
    content, 
    flags=re.DOTALL
)

# Replace Restore subscription callback
restore_target = """                                    if (info.entitlements["Piggy Ledger Pro"]?.isActive == true) {"""
restore_replacement = """                                    val active = info.entitlements.all.values.any { it.isActive } || info.entitlements["Piggy Ledger Pro"]?.isActive == true
                                    if (active) {"""

content = content.replace(restore_target, restore_replacement)

# Replace SwipeToUpgrade button logic
swipe_target_pattern = r'SwipeToUpgradeButton\(.*?onSwipeComplete = \{.*?\n                \},'
swipe_replacement = """SwipeToUpgradeButton(
                planText = planMeta.badgeName,
                priceText = planMeta.priceText,
                accentColor = planMeta.accentColor,
                isPurchasing = isPurchasing,
                onSwipeComplete = {
                    if (isPurchasing) return@SwipeToUpgradeButton
                    val packageToBuy = when (selectedPlan) {
                        PaywallPlan.MONTHLY -> monthlyPackage
                        PaywallPlan.YEARLY -> yearlyPackage
                        PaywallPlan.LIFETIME -> lifetimePackage
                    }
                    val activity = context.findActivity()
                    if (packageToBuy != null && activity != null) {
                        isPurchasing = true
                        com.revenuecat.purchases.Purchases.sharedInstance.purchase(
                            com.revenuecat.purchases.PurchaseParams.Builder(activity, packageToBuy).build(),
                            object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                                override fun onCompleted(storeTransaction: com.revenuecat.purchases.models.StoreTransaction, customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                    isPurchasing = false
                                    val active = customerInfo.entitlements.all.values.any { it.isActive } || customerInfo.entitlements["Piggy Ledger Pro"]?.isActive == true
                                    if (active || customerInfo.entitlements.all.isNotEmpty()) {
                                        viewModel.setPremiumStatus(true)
                                        onPurchaseSuccess(customerInfo)
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Welcome to Pro! Pro features unlocked.", android.widget.Toast.LENGTH_SHORT)
                                    } else {
                                        viewModel.setPremiumStatus(true)
                                        onPurchaseSuccess(customerInfo)
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Purchase complete! Unlocking Pro...", android.widget.Toast.LENGTH_SHORT)
                                    }
                                }
                                override fun onError(error: com.revenuecat.purchases.PurchasesError, userCancelled: Boolean) {
                                    isPurchasing = false
                                    if (!userCancelled) {
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Purchase error: ${error.message}", android.widget.Toast.LENGTH_LONG)
                                    }
                                }
                            }
                        )
                    } else {
                        val msg = when {
                            isLoadingOfferings -> "Plans are loading from RevenueCat. Please wait a moment..."
                            fetchError != null -> "RevenueCat error: $fetchError"
                            packagesList.isEmpty() -> "No active billing products found. Ensure Play Console products are Active & test account added."
                            else -> "Selected plan is currently unavailable."
                        }
                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, msg, android.widget.Toast.LENGTH_LONG)
                    }
                },"""

content = re.sub(swipe_target_pattern, swipe_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "w") as f:
    f.write(content)

# 2. Update DashboardScreen.kt
with open("app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt", "r") as f:
    dash_content = f.read()

dash_content = dash_content.replace(
    'val active = info.entitlements["Piggy Ledger Pro"]?.isActive == true',
    'val active = info.entitlements.all.values.any { it.isActive } || info.entitlements["Piggy Ledger Pro"]?.isActive == true'
)

dash_content = dash_content.replace(
    'customerInfo?.entitlements?.get("Piggy Ledger Pro")',
    'customerInfo?.entitlements?.active?.values?.firstOrNull() ?: customerInfo?.entitlements?.get("Piggy Ledger Pro")'
)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt", "w") as f:
    f.write(dash_content)

print("Patch applied successfully.")
