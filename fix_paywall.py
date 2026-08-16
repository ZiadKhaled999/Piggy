import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

comp_old = """    val comparisonFeatures = listOf(
        FeatureComparisonRow("Accounts & Goals", FeatureStatus.TextValue("2 Max"), FeatureStatus.Check),
        FeatureComparisonRow("Budgets & Loans", FeatureStatus.TextValue("2 Max"), FeatureStatus.Check),
        FeatureComparisonRow("Advanced Analytics", FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow("Data Export (CSV/PDF)", FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow("Custom Categories", FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow("Screenshot Protection", FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow("Cloud Backup & Sync", FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow("Priority Support", FeatureStatus.Dash, FeatureStatus.Check)
    )"""

comp_new = """    val comparisonFeatures = listOf(
        FeatureComparisonRow(stringResource(R.string.comp_acc_goals), FeatureStatus.TextValue(stringResource(R.string.two_max)), FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_budgets_loans), FeatureStatus.TextValue(stringResource(R.string.two_max)), FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_adv_analytics), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_export), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_custom_categories), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_screenshot_protect), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_cloud_sync), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_priority_support), FeatureStatus.Dash, FeatureStatus.Check)
    )"""

lt_old = """        PaywallPlan.LIFETIME -> PlanMetadata(
            tabLabel = "Lifetime",
            badgeName = "Lifetime",
            headerSubtitle = "Unlock lifetime unlimited access & all future features",
            priceText = lifetimePackage?.product?.price?.formatted ?: "$299.99",
            renewalCaption = "One-time payment of ${lifetimePackage?.product?.price?.formatted ?: "$299.99"}. No renewal or hidden fees.",
            ctaText = "Upgrade Lifetime",
            tag = "BEST VALUE","""

lt_new = """        PaywallPlan.LIFETIME -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_lifetime),
            badgeName = stringResource(R.string.plan_lifetime),
            headerSubtitle = stringResource(R.string.plan_lifetime_desc_2),
            priceText = lifetimePackage?.product?.price?.formatted ?: "$299.99",
            renewalCaption = stringResource(R.string.plan_lifetime_renew_2, lifetimePackage?.product?.price?.formatted ?: "$299.99"),
            ctaText = stringResource(R.string.upgrade_lifetime_2),
            tag = stringResource(R.string.best_value_caps),"""

content = content.replace(comp_old, comp_new)
content = content.replace(lt_old, lt_new)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)

