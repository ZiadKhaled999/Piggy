package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

data class ProviderDefinition(
    val canonicalKey: String,
    val standardName: String,
    val aliases: List<String>
)

object BankProviderRegistry {

    private val providers = listOf(
        // E-Wallets
        ProviderDefinition(
            canonicalKey = "VODAFONE_CASH",
            standardName = "Vodafone Cash",
            aliases = listOf("vodafone", "vf-cash", "vfcash", "vodafone cash", "vodafonecash", "فودافون", "فودافون كاش", "فودافونكاش")
        ),
        ProviderDefinition(
            canonicalKey = "ORANGE_CASH",
            standardName = "Orange Cash",
            aliases = listOf("orange", "orangecash", "orange cash", "أورنج", "اورنج", "أورنج كاش", "اورنج كاش")
        ),
        ProviderDefinition(
            canonicalKey = "ETISALAT_CASH",
            standardName = "e& Cash",
            aliases = listOf("etisalat", "etisalat cash", "etisalatcash", "e&", "e& cash", "e&cash", "اتصالات", "اتصالات كاش", "اي اند", "إي آند كاش")
        ),
        ProviderDefinition(
            canonicalKey = "WE_PAY",
            standardName = "WE Pay",
            aliases = listOf("we pay", "wepay", "we", "telecom egypt", "وي باي", "المصرية للاتصالات", "وي")
        ),

        // Instant Payment Network / Switch
        ProviderDefinition(
            canonicalKey = "INSTAPAY",
            standardName = "InstaPay",
            aliases = listOf("instapay", "انستاباي", "انستا باي", "المدفوعات اللحظية", "شبكة المدفوعات اللحظية", "ipn", "smartwallet", "telda", "nexta")
        ),

        // 40 Egyptian Banks
        ProviderDefinition(
            canonicalKey = "CIB",
            standardName = "Commercial International Bank (CIB)",
            aliases = listOf("cib", "cibegypt", "cib-alert", "cib alert", "commercial international bank", "التجاري الدولي", "البنك التجاري الدولي", "البنك التجارى الدولى")
        ),
        ProviderDefinition(
            canonicalKey = "NBE",
            standardName = "National Bank of Egypt (NBE)",
            aliases = listOf("nbe", "nbeg", "nbe-alert", "national bank of egypt", "nationalbankofegypt", "ahlybank", "ahly", "alahly", "الأهلي", "الاهلي", "البنك الأهلي", "البنك الاهلي", "البنك الأهلي المصري", "البنك الاهلي المصري")
        ),
        ProviderDefinition(
            canonicalKey = "BANQUE_MISR",
            standardName = "Banque Misr",
            aliases = listOf("bm", "bm-alert", "banque misr", "banquemisr", "بنك مصر", "مصر")
        ),
        ProviderDefinition(
            canonicalKey = "BANQUE_DU_CAIRE",
            standardName = "Banque Du Caire",
            aliases = listOf("bdc", "bdc-alert", "banque du caire", "banqueducaire", "بنك القاهرة", "القاهرة")
        ),
        ProviderDefinition(
            canonicalKey = "QNB",
            standardName = "QNB Alahli",
            aliases = listOf("qnb", "qnb-alert", "qnbalahli", "qnb alahli", "قطر الوطني", "كيو ان بي")
        ),
        ProviderDefinition(
            canonicalKey = "ALEXBANK",
            standardName = "Bank of Alexandria (AlexBank)",
            aliases = listOf("alexbank", "alex bank", "alexbank-alert", "bank of alexandria", "بنك الإسكندرية", "بنك الاسكندرية", "إسكندرية", "اسكندرية")
        ),
        ProviderDefinition(
            canonicalKey = "HSBC",
            standardName = "HSBC Egypt",
            aliases = listOf("hsbc", "hsbcegypt", "hsbc egypt", "hsbc-alert", "اتش اس بي سي")
        ),
        ProviderDefinition(
            canonicalKey = "FAISAL",
            standardName = "Faisal Islamic Bank",
            aliases = listOf("faisal", "faisalbank", "faisal islamic bank", "بنك فيصل", "فيصل الإسلامي", "فيصل الاسلامي", "فيصل")
        ),
        ProviderDefinition(
            canonicalKey = "AAIB",
            standardName = "Arab African International Bank (AAIB)",
            aliases = listOf("aaib", "aaib-alert", "arab african", "arab african international bank", "العربي الأفريقي", "العربي الافريقي", "البنك العربي الافريقي")
        ),
        ProviderDefinition(
            canonicalKey = "ADIB",
            standardName = "Abu Dhabi Islamic Bank (ADIB)",
            aliases = listOf("adib", "adib-alert", "adibegypt", "abu dhabi islamic", "أبوظبي الإسلامي", "ابوظبي الاسلامي", "أبو ظبي الإسلامي", "مصرف أبو ظبي الإسلامي")
        ),
        ProviderDefinition(
            canonicalKey = "CAE",
            standardName = "Credit Agricole Egypt",
            aliases = listOf("cae", "credit agricole", "creditagricole", "كريدي أجريكول", "كريدي اجريكول", "كريدي")
        ),
        ProviderDefinition(
            canonicalKey = "ENBD",
            standardName = "Emirates NBD Egypt",
            aliases = listOf("enbd", "emirates nbd", "emiratesnbd", "الإمارات دبي", "الامارات دبي", "بنك الإمارات دبي الوطني")
        ),
        ProviderDefinition(
            canonicalKey = "HDB",
            standardName = "Housing and Development Bank (HDB)",
            aliases = listOf("hdb", "housing and development", "housingdevelopmentbank", "التعمير والإسكان", "التعمير والاسكان", "بنك التعمير والإسكان")
        ),
        ProviderDefinition(
            canonicalKey = "EGB",
            standardName = "Egyptian Gulf Bank (EG Bank)",
            aliases = listOf("egb", "eg bank", "egbank", "egyptian gulf bank", "المصري الخليجي", "البنك المصري الخليجي")
        ),
        ProviderDefinition(
            canonicalKey = "SAIB",
            standardName = "SAIB Bank",
            aliases = listOf("saib", "saib bank", "saibbank", "بنك سايب", "الشركة المصرفية العربية الدولية", "الشركة المصرفية")
        ),
        ProviderDefinition(
            canonicalKey = "ALBARAKA",
            standardName = "Al Baraka Bank",
            aliases = listOf("albaraka", "baraka", "al baraka", "abg", "بنك البركة", "البركة")
        ),
        ProviderDefinition(
            canonicalKey = "ATTIJARIWAFA",
            standardName = "Attijariwafa Bank Egypt",
            aliases = listOf("attijariwafa", "awb", "التجاري وفا", "التجاري وفا بنك", "وفا بنك")
        ),
        ProviderDefinition(
            canonicalKey = "ARAB_BANK",
            standardName = "Arab Bank",
            aliases = listOf("arab bank", "arabbank", "البنك العربي", "العربي")
        ),
        ProviderDefinition(
            canonicalKey = "ADCB",
            standardName = "Abu Dhabi Commercial Bank (ADCB)",
            aliases = listOf("adcb", "adcbegypt", "abu dhabi commercial", "أبوظبي التجاري", "ابوظبي التجاري", "بنك أبوظبي التجاري")
        ),
        ProviderDefinition(
            canonicalKey = "EBANK",
            standardName = "EBank (Export Development Bank of Egypt)",
            aliases = listOf("ebank", "edbe", "export development", "تنمية الصادرات", "البنك المصري لتنمية الصادرات")
        ),
        ProviderDefinition(
            canonicalKey = "UNITED_BANK",
            standardName = "The United Bank of Egypt",
            aliases = listOf("united bank", "unitedbank", "ub", "المصرف المتحد")
        ),
        ProviderDefinition(
            canonicalKey = "SUEZ_CANAL",
            standardName = "Suez Canal Bank",
            aliases = listOf("suez canal", "suezcanal", "scb", "قناة السويس", "بنك قناة السويس")
        ),
        ProviderDefinition(
            canonicalKey = "MASHREQ",
            standardName = "Mashreq Bank",
            aliases = listOf("mashreq", "mashreq bank", "بنك المشرق", "المشرق")
        ),
        ProviderDefinition(
            canonicalKey = "CITIBANK",
            standardName = "Citibank Egypt",
            aliases = listOf("citibank", "citi", "سيتي بنك")
        ),
        ProviderDefinition(
            canonicalKey = "FAB",
            standardName = "First Abu Dhabi Bank (FAB / Audi)",
            aliases = listOf("fab", "fabegypt", "bank audi", "bankaudi", "بنك عوده", "أبوظبي الأول", "ابوظبي الاول")
        ),
        ProviderDefinition(
            canonicalKey = "ABK",
            standardName = "Al Ahli Bank of Kuwait (ABK)",
            aliases = listOf("abk", "abkegypt", "الأهلي الكويتي", "الاهلي الكويتي")
        ),
        ProviderDefinition(
            canonicalKey = "NBK",
            standardName = "National Bank of Kuwait (NBK)",
            aliases = listOf("nbk", "nbkegypt", "الكويت الوطني", "بنك الكويت الوطني")
        ),
        ProviderDefinition(
            canonicalKey = "BANK_ABC",
            standardName = "Bank ABC Egypt",
            aliases = listOf("bank abc", "bankabc", "abc bank", "المؤسسة العربية المصرفية")
        ),
        ProviderDefinition(
            canonicalKey = "AIBANK",
            standardName = "aiBank (Arab Investment Bank)",
            aliases = listOf("aibank", "ai bank", "الاستثمار العربي", "بنك الاستثمار العربي")
        ),
        ProviderDefinition(
            canonicalKey = "MIDBANK",
            standardName = "MIDBANK",
            aliases = listOf("midbank", "mid bank", "ميد بنك")
        )
    )

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
            .trim()
    }

    /**
     * Resolves the canonical key for a provider name string.
     */
    fun getCanonicalKey(providerOrName: String?): String? {
        if (providerOrName.isNullOrBlank()) return null
        val norm = normalize(providerOrName)
        for (def in providers) {
            if (normalize(def.standardName) == norm || normalize(def.canonicalKey) == norm) {
                return def.canonicalKey
            }
            if (def.aliases.any { normalize(it) == norm || norm.contains(normalize(it)) || normalize(it).contains(norm) }) {
                return def.canonicalKey
            }
        }
        return null
    }

    /**
     * Checks if sender or SMS body matches the provider or account.
     */
    fun matchesProviderOrAccount(
        account: Account,
        sender: String,
        body: String,
        customKeywords: List<String>
    ): Boolean {
        val normSender = normalize(sender)
        val normBody = normalize(body)

        // 1. Check account's custom keywords from UserPreferences
        for (kw in customKeywords) {
            val normKw = normalize(kw)
            if (normKw.isNotBlank() && (normSender.contains(normKw) || normBody.contains(normKw) || normKw.contains(normSender))) {
                return true
            }
        }

        // 2. Check account's direct name and label
        val normName = normalize(account.name)
        if (normName.length >= 3 && (normSender.contains(normName) || normBody.contains(normName) || (normName.length <= 10 && normName.contains(normSender)))) {
            return true
        }
        account.label?.let { label ->
            val normLabel = normalize(label)
            if (normLabel.length >= 3 && (normSender.contains(normLabel) || normBody.contains(normLabel))) {
                return true
            }
        }

        // 3. Check canonical provider definition
        val canonicalKey = getCanonicalKey(account.provider) ?: getCanonicalKey(account.name)
        if (canonicalKey != null) {
            val def = providers.firstOrNull { it.canonicalKey == canonicalKey }
            if (def != null) {
                for (alias in def.aliases) {
                    val normAlias = normalize(alias)
                    if (normSender.contains(normAlias) || normAlias.contains(normSender) || normBody.contains(normAlias)) {
                        return true
                    }
                }
            }
        }

        // 4. Fallback: check raw provider string containment
        account.provider?.takeIf { it.isNotBlank() }?.let { prov ->
            val normProv = normalize(prov)
            if (normSender.contains(normProv) || normProv.contains(normSender) || normBody.contains(normProv)) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts candidate card/account 4-digit numbers from SMS body.
     */
    fun extractCardOrAccountTokens(body: String): List<String> {
        val tokens = mutableSetOf<String>()

        // 1. Explicit card/account keywords followed by digits
        val patterns = listOf(
            Regex("""(?i)(?:ending\s*(?:in|with)?|بطاق[ة|تك]|card|حساب(?:ك)?|account)\s*(?:المنتهي[ة|ة]?\s*بـ?|رقم|no\.?|#)?\s*[*xX\.\-]*\s*(\d{4})"""),
            Regex("""(?i)[*xX]{2,4}(\d{4})"""),
            Regex("""(?i)\.{2,4}(\d{4})"""),
            Regex("""(?i)card\s*(\d{4})"""),
            Regex("""(?i)بطاقة\s*(\d{4})""")
        )

        for (pattern in patterns) {
            pattern.findAll(body).forEach { match ->
                match.groups[1]?.value?.let { tokens.add(it) }
            }
        }

        return tokens.toList()
    }

    /**
     * Checks if an account's card_numbers or bank_account_no matches candidate SMS tokens or body.
     */
    fun matchesCardOrAccount(account: Account, smsTokens: List<String>, body: String): Boolean {
        val accountCardTokens = mutableListOf<String>()

        // Split account.card_numbers by common delimiters
        account.card_numbers?.let { raw ->
            raw.split(',', ';', ' ', '\n', '-', '/').forEach { piece ->
                val clean = piece.replace(" ", "").replace("-", "")
                if (clean.length >= 4) {
                    accountCardTokens.add(clean.takeLast(4))
                    accountCardTokens.add(clean)
                }
            }
        }

        // Split account.bank_account_no
        account.bank_account_no?.let { raw ->
            raw.split(',', ';', ' ', '\n', '-', '/').forEach { piece ->
                val clean = piece.replace(" ", "").replace("-", "")
                if (clean.length >= 4) {
                    accountCardTokens.add(clean.takeLast(4))
                    accountCardTokens.add(clean)
                }
            }
        }

        if (accountCardTokens.isEmpty()) return false

        // Match against explicit extracted SMS card tokens
        for (token in smsTokens) {
            if (accountCardTokens.contains(token)) return true
        }

        // Match against body directly
        for (token in accountCardTokens) {
            if (token.length == 4) {
                // Must be 4 digits in body
                if (body.contains(token)) {
                    val tokenRegex = Regex("""(?i)(?:[*xX\.\-]|ending|بطاق[ة|تك]|حساب|card|account)\s*[*xX\.\-]*$token|\b$token\b""")
                    if (tokenRegex.containsMatchIn(body)) return true
                }
            } else if (body.contains(token)) {
                return true
            }
        }

        return false
    }

    /**
     * High-precision resolver to find the exact matching Account for an incoming SMS.
     */
    suspend fun resolveAccount(
        context: Context,
        sender: String,
        rawBody: String,
        actionType: SmsActionType
    ): Account? {
        val db = PiggyLedgerDatabase.getInstance(context)
        val dao = db.piggyLedgerDao()
        val allAccounts = dao.getAllAccountsSync().filter { !it.is_deleted }

        if (allAccounts.isEmpty()) {
            Log.d("BankProviderRegistry", "No accounts exist in app; routing to pending")
            return null
        }

        // RULE 1: If user has ONLY ONE active account, every valid transaction SMS belongs to it!
        if (allAccounts.size == 1) {
            Log.d("BankProviderRegistry", "User has exactly one account (${allAccounts.first().name}); auto-matched")
            return allAccounts.first()
        }

        val body = SmsParser.convertArabicDigitsAndSymbols(rawBody)

        // Read user preferences
        val userPrefs = UserPreferences(context)
        val preferredAccountId = userPrefs.preferredAccountId.firstOrNull()
        val customJsonStr = userPrefs.customIdentifiersJson.firstOrNull() ?: "{}"
        val customMap: Map<String, List<String>> = try {
            Json { ignoreUnknownKeys = true }.decodeFromString(customJsonStr)
        } catch (e: Exception) {
            emptyMap()
        }

        // RULE 2: Exact Card / Account Number Match (Highest Priority)
        val smsCardTokens = extractCardOrAccountTokens(body)
        val exactCardMatches = allAccounts.filter { account ->
            matchesCardOrAccount(account, smsCardTokens, body)
        }
        if (exactCardMatches.size == 1) {
            Log.d("BankProviderRegistry", "Card/account number uniquely matched ${exactCardMatches.first().name}")
            return exactCardMatches.first()
        } else if (exactCardMatches.size > 1) {
            // If multiple accounts share digits, prioritize preferred or provider
            preferredAccountId?.let { prefId ->
                exactCardMatches.firstOrNull { it.id == prefId }?.let { return it }
            }
            return exactCardMatches.first()
        }

        // RULE 3: Provider & Keyword Matching
        val providerMatches = allAccounts.filter { account ->
            val accountKeywords = mutableListOf<String>()
            account.provider?.let { customMap[it]?.let { list -> accountKeywords.addAll(list) } }
            customMap[account.name]?.let { list -> accountKeywords.addAll(list) }

            matchesProviderOrAccount(account, sender, body, accountKeywords)
        }

        // Special handling for InstaPay
        val isInstaPay = sender.contains("instapay", ignoreCase = true) ||
                         rawBody.contains("instapay", ignoreCase = true) ||
                         rawBody.contains("انستاباي") ||
                         rawBody.contains("المدفوعات اللحظية")

        var candidateAccounts = providerMatches

        if (isInstaPay && candidateAccounts.isEmpty()) {
            // In an InstaPay SMS, the body often mentions the target bank or wallet
            // E.g. "إلى حسابك في CIB" or "لدى البنك الأهلي المصري" or "محفظة فودافون كاش"
            val bankMentionedMatches = allAccounts.filter { account ->
                val canonicalKey = getCanonicalKey(account.provider) ?: getCanonicalKey(account.name)
                if (canonicalKey != null && canonicalKey != "INSTAPAY") {
                    val def = providers.firstOrNull { it.canonicalKey == canonicalKey }
                    def?.aliases?.any { alias -> normalize(body).contains(normalize(alias)) } == true
                } else false
            }

            if (bankMentionedMatches.isNotEmpty()) {
                candidateAccounts = bankMentionedMatches
            } else {
                // Check if user has an account with instaPayFee enabled
                val instaFeeAccounts = allAccounts.filter { it.insta_pay_fee }
                if (instaFeeAccounts.size == 1) {
                    return instaFeeAccounts.first()
                } else if (instaFeeAccounts.isNotEmpty()) {
                    candidateAccounts = instaFeeAccounts
                }
            }
        }

        // RULE 4: Disambiguate Candidate Accounts
        if (candidateAccounts.size == 1) {
            Log.d("BankProviderRegistry", "Provider uniquely matched ${candidateAccounts.first().name}")
            return candidateAccounts.first()
        }

        if (candidateAccounts.size > 1) {
            // A. Check if one of them is the user's preferred account
            if (preferredAccountId != null) {
                candidateAccounts.firstOrNull { it.id == preferredAccountId }?.let {
                    Log.d("BankProviderRegistry", "Disambiguated to preferred account: ${it.name}")
                    return it
                }
            }

            // B. Check Account Type based on transaction action type
            val isCardTransaction = actionType == SmsActionType.PURCHASE ||
                body.contains("بطاقة") || body.contains("card") ||
                body.contains("شراء") || body.contains("purchase") ||
                body.contains("ائتمان") || body.contains("credit") ||
                body.contains("pos", ignoreCase = true)

            val isBankTransaction = actionType == SmsActionType.TRANSFER_OUT ||
                actionType == SmsActionType.DEPOSIT ||
                actionType == SmsActionType.WITHDRAWAL ||
                body.contains("تحويل") || body.contains("transfer") ||
                body.contains("ايداع") || body.contains("حساب") ||
                body.contains("مرتب") || body.contains("salary")

            if (isCardTransaction) {
                val cardAccounts = candidateAccounts.filter { it.type == AccountType.CARD }
                if (cardAccounts.size == 1) return cardAccounts.first()
            }

            if (isBankTransaction) {
                val bankAccounts = candidateAccounts.filter { it.type == AccountType.BANK }
                if (bankAccounts.size == 1) return bankAccounts.first()
            }

            // C. Check if specific account name or label is found in body
            val nameMatches = candidateAccounts.filter { account ->
                account.name.isNotBlank() && body.contains(account.name, ignoreCase = true)
            }
            if (nameMatches.size == 1) return nameMatches.first()

            // If genuine ambiguity between multiple identical-type accounts of the same bank:
            Log.d("BankProviderRegistry", "Genuine ambiguity between ${candidateAccounts.size} accounts; sending to pending")
            return null
        }

        // RULE 5: Fallback to Preferred Account if sender is a verified financial message
        if (preferredAccountId != null) {
            allAccounts.firstOrNull { it.id == preferredAccountId }?.let {
                Log.d("BankProviderRegistry", "Fallback to user's preferred account: ${it.name}")
                return it
            }
        }

        return null
    }
}
