import re

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

# Append Context.findActivity() at the end
if "fun android.content.Context.findActivity()" not in content:
    content += "\nfun android.content.Context.findActivity(): android.app.Activity? {\n"
    content += "    var context = this\n"
    content += "    while (context is android.content.ContextWrapper) {\n"
    content += "        if (context is android.app.Activity) return context\n"
    content += "        context = context.baseContext\n"
    content += "    }\n"
    content += "    return null\n"
    content += "}\n"

# Replace PiggyLedgerPaywall signature to fetch offerings and handle RevenueCat purchases
paywall_code = """
fun PiggyLedgerPaywall(
    viewModel: PiggyLedgerViewModel,
    onPurchaseSuccess: (com.revenuecat.purchases.CustomerInfo?) -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var offerings: com.revenuecat.purchases.Offerings? by remember { mutableStateOf(null) }
    var selectedPlan by remember { mutableStateOf(PaywallPlan.YEARLY) }
    var isPurchasing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.revenuecat.purchases.Purchases.sharedInstance.getOfferings(
            object : com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                override fun onReceived(offeringsResult: com.revenuecat.purchases.Offerings) {
                    offerings = offeringsResult
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    android.util.Log.e("Paywall", "Error fetching offerings: ${error.message}")
                }
            }
        )
    }

    val monthlyPackage = offerings?.current?.availablePackages?.find { it.packageType == com.revenuecat.purchases.PackageType.MONTHLY }
    val yearlyPackage = offerings?.current?.availablePackages?.find { it.packageType == com.revenuecat.purchases.PackageType.ANNUAL }
    val lifetimePackage = offerings?.current?.availablePackages?.find { it.packageType == com.revenuecat.purchases.PackageType.LIFETIME }

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

swipe_to_upgrade_target = """
            SwipeToUpgradeButton(
                planText = planMeta.badgeName,
                priceText = planMeta.priceText,
                accentColor = planMeta.accentColor,
                isPurchasing = isPurchasing,
                onSwipeComplete = {
                    if (isPurchasing) return@SwipeToUpgradeButton
                    isPurchasing = true
                    viewModel.setPremiumStatus(true)
                    onPurchaseSuccess(null)
                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Welcome to Pro! Pro features unlocked.", Toast.LENGTH_SHORT)
                    isPurchasing = false
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
"""

swipe_to_upgrade_replacement = """
            SwipeToUpgradeButton(
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
                            activity,
                            packageToBuy,
                            object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                                override fun onCompleted(storeTransaction: com.revenuecat.purchases.models.StoreTransaction, customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                    isPurchasing = false
                                    if (customerInfo.entitlements["Piggy Ledger Pro"]?.isActive == true) {
                                        viewModel.setPremiumStatus(true)
                                        onPurchaseSuccess(customerInfo)
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Welcome to Pro! Pro features unlocked.", android.widget.Toast.LENGTH_SHORT)
                                    }
                                }
                                override fun onError(error: com.revenuecat.purchases.PurchasesError, userCancelled: Boolean) {
                                    isPurchasing = false
                                    if (!userCancelled) {
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Purchase failed: ${error.message}", android.widget.Toast.LENGTH_LONG)
                                    }
                                }
                            }
                        )
                    } else {
                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Plans are currently loading or unavailable.", android.widget.Toast.LENGTH_SHORT)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
"""

if "val packageToBuy" not in content:
    content = content.replace(swipe_to_upgrade_target, swipe_to_upgrade_replacement)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
