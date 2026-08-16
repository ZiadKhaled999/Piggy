import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Replace hardcoded strings in PiggyLedgerProView
content = content.replace('text = "PRO MEMBER ACTIVE",', 'text = stringResource(R.string.pro_member_active),')
content = content.replace('text = "Piggy Ledger Pro",\n                        fontSize = 22.sp', 'text = stringResource(R.string.pro_title),\n                        fontSize = 22.sp')
content = content.replace('text = "All premium features are unlocked and active on your device.",', 'text = stringResource(R.string.pro_desc),')

features_old = """                        listOf(
                            "Unlimited Accounts & Savings Goals",
                            "Unlimited Budgets & Loan Ledgers",
                            "Advanced Financial Analytics & Charts",
                            "Data Export (CSV/PDF) & Cloud Sync",
                            "Screenshot Protection & Custom Categories"
                        ).forEach { feature ->"""

features_new = """                        listOf(
                            stringResource(R.string.pro_feature_1),
                            stringResource(R.string.pro_feature_2),
                            stringResource(R.string.pro_feature_3),
                            stringResource(R.string.pro_feature_4),
                            stringResource(R.string.pro_feature_5)
                        ).forEach { feature ->"""

content = content.replace(features_old, features_new)

# Replace in PaywallPlan setup
meta_monthly_old = """        PaywallPlan.MONTHLY -> PlanMetadata(
            tabLabel = "Monthly",
            badgeName = "Monthly",
            headerSubtitle = "Keep tracking with expanded access & unlimited control",
            priceText = monthlyPackage?.product?.price?.formatted ?: "$9.99 / mo",
            renewalCaption = "Renews for ${monthlyPackage?.product?.price?.formatted ?: "$9.99"}/month. Cancel anytime.",
            ctaText = "Upgrade Monthly","""

meta_monthly_new = """        PaywallPlan.MONTHLY -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_monthly),
            badgeName = stringResource(R.string.plan_monthly),
            headerSubtitle = stringResource(R.string.plan_monthly_desc),
            priceText = monthlyPackage?.product?.price?.formatted ?: "$9.99 / mo",
            renewalCaption = stringResource(R.string.plan_monthly_renew, monthlyPackage?.product?.price?.formatted ?: "$9.99"),
            ctaText = stringResource(R.string.upgrade_monthly),"""
            
meta_yearly_old = """        PaywallPlan.YEARLY -> PlanMetadata(
            tabLabel = "Yearly",
            badgeName = "Yearly",
            headerSubtitle = "Save 40% with annual billing",
            priceText = yearlyPackage?.product?.price?.formatted ?: "$59.99 / yr",
            renewalCaption = "Renews for ${yearlyPackage?.product?.price?.formatted ?: "$59.99"}/year. Cancel anytime.",
            ctaText = "Upgrade Yearly",
            tag = "Most Popular","""

meta_yearly_new = """        PaywallPlan.YEARLY -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_yearly),
            badgeName = stringResource(R.string.plan_yearly),
            headerSubtitle = stringResource(R.string.plan_yearly_desc),
            priceText = yearlyPackage?.product?.price?.formatted ?: "$59.99 / yr",
            renewalCaption = stringResource(R.string.plan_yearly_renew, yearlyPackage?.product?.price?.formatted ?: "$59.99"),
            ctaText = stringResource(R.string.upgrade_yearly),
            tag = stringResource(R.string.tag_popular),"""

meta_lifetime_old = """        PaywallPlan.LIFETIME -> PlanMetadata(
            tabLabel = "Lifetime",
            badgeName = "Lifetime",
            headerSubtitle = "One-time payment for forever access",
            priceText = lifetimePackage?.product?.price?.formatted ?: "$149.99",
            renewalCaption = "Pay once, yours forever.",
            ctaText = "Get Lifetime Access",
            tag = "Best Value","""

meta_lifetime_new = """        PaywallPlan.LIFETIME -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_lifetime),
            badgeName = stringResource(R.string.plan_lifetime),
            headerSubtitle = stringResource(R.string.plan_lifetime_desc),
            priceText = lifetimePackage?.product?.price?.formatted ?: "$149.99",
            renewalCaption = stringResource(R.string.plan_lifetime_renew),
            ctaText = stringResource(R.string.upgrade_lifetime),
            tag = stringResource(R.string.tag_best_value),"""

content = content.replace(meta_monthly_old, meta_monthly_new)
content = content.replace(meta_yearly_old, meta_yearly_new)
content = content.replace(meta_lifetime_old, meta_lifetime_new)

# Find "Unlock Premium Now" and replace
content = content.replace('"Unlock Premium Now"', 'stringResource(R.string.unlock_premium_now)')

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
