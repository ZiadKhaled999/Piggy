package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.NotificationParserPrefs
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

data class ProviderDefinition(
    val canonicalKey: String,
    val standardName: String,
    val aliases: List<String>,
    val packageNames: List<String> = emptyList()
)

object BankProviderRegistry {

    private val providers = listOf(
        // Egyptian E-Wallets
        ProviderDefinition(
            canonicalKey = "VODAFONE_CASH",
            standardName = "Vodafone Cash",
            aliases = listOf("vodafone", "vf-cash", "vfcash", "vodafone cash", "vodafonecash", "ana vodafone", "فودافون", "فودافون كاش", "فودافونكاش", "انا فودافون"),
            packageNames = listOf("com.emeint.android.myservices", "com.vodafone.vodafoneapp", "com.vodafone.cash")
        ),
        ProviderDefinition(
            canonicalKey = "ORANGE_CASH",
            standardName = "Orange Cash",
            aliases = listOf("orange", "orangecash", "orange cash", "my orange", "أورنج", "اورنج", "أورنج كاش", "اورنج كاش"),
            packageNames = listOf("com.orange.orangecash", "com.orange.myorange", "com.orange.egypt.myorange")
        ),
        ProviderDefinition(
            canonicalKey = "ETISALAT_CASH",
            standardName = "e& Cash",
            aliases = listOf("etisalat", "etisalat cash", "etisalatcash", "e&", "e& cash", "e&cash", "my e&", "اتصالات", "اتصالات كاش", "اي اند", "إي آند كاش"),
            packageNames = listOf("com.etisalat.etisalatcash", "com.etisalat", "ae.etisalat.myetisalat")
        ),
        ProviderDefinition(
            canonicalKey = "WE_PAY",
            standardName = "WE Pay",
            aliases = listOf("we pay", "wepay", "we", "telecom egypt", "my we", "وي باي", "المصرية للاتصالات", "وي"),
            packageNames = listOf("com.te.wepay", "eg.com.te.mywe", "com.te.mywe")
        ),

        // Instant Payment Network / Fintechs (Egypt & MENA)
        ProviderDefinition(
            canonicalKey = "INSTAPAY",
            standardName = "InstaPay",
            aliases = listOf("instapay", "انستاباي", "انستا باي", "المدفوعات اللحظية", "شبكة المدفوعات اللحظية", "ipn", "smartwallet"),
            packageNames = listOf("com.egyptianbanks.instapay")
        ),
        ProviderDefinition(
            canonicalKey = "TELDA",
            standardName = "Telda",
            aliases = listOf("telda", "تيلدا"),
            packageNames = listOf("app.telda", "com.telda.app")
        ),
        ProviderDefinition(
            canonicalKey = "NEXTA",
            standardName = "Nexta",
            aliases = listOf("nexta", "نيكستا"),
            packageNames = listOf("com.nexta.app", "app.nexta")
        ),
        ProviderDefinition(
            canonicalKey = "KLIVVR",
            standardName = "Klivvr",
            aliases = listOf("klivvr", "كليفر"),
            packageNames = listOf("com.klivvr.app", "app.klivvr")
        ),
        ProviderDefinition(
            canonicalKey = "KHAZNA",
            standardName = "Khazna",
            aliases = listOf("khazna", "خزنة"),
            packageNames = listOf("app.khazna", "com.khazna.app")
        ),
        ProviderDefinition(
            canonicalKey = "FAWRY",
            standardName = "Fawry",
            aliases = listOf("fawry", "myfawry", "فوري", "ماي فوري"),
            packageNames = listOf("com.fawry.myfawry")
        ),
        ProviderDefinition(
            canonicalKey = "VALU",
            standardName = "valU",
            aliases = listOf("valu", "فاليو"),
            packageNames = listOf("com.valu.app", "com.valu.customer")
        ),

        // Egyptian Banks
        ProviderDefinition(
            canonicalKey = "CIB",
            standardName = "Commercial International Bank (CIB)",
            aliases = listOf("cib", "cibegypt", "cib-alert", "cib alert", "cib smart", "commercial international bank", "التجاري الدولي", "البنك التجاري الدولي", "البنك التجارى الدولى"),
            packageNames = listOf("com.cib.cibmobile", "com.cibeg.mobile", "com.cibeg.smartwallet")
        ),
        ProviderDefinition(
            canonicalKey = "NBE",
            standardName = "National Bank of Egypt (NBE)",
            aliases = listOf("nbe", "nbeg", "nbe-alert", "nbe mobile", "national bank of egypt", "nationalbankofegypt", "ahlybank", "ahly", "alahly", "الأهلي", "الاهلي", "البنك الأهلي", "البنك الاهلي", "البنك الأهلي المصري", "البنك الاهلي المصري"),
            packageNames = listOf("com.nbe.nbemobile", "com.nbe.ahlynet", "com.nbe.phonecash")
        ),
        ProviderDefinition(
            canonicalKey = "BANQUE_MISR",
            standardName = "Banque Misr",
            aliases = listOf("bm", "bm-alert", "bm online", "banque misr", "banquemisr", "بنك مصر", "مصر"),
            packageNames = listOf("com.banquemisr.bm", "com.bm.online", "com.banquemisr.wallet")
        ),
        ProviderDefinition(
            canonicalKey = "BANQUE_DU_CAIRE",
            standardName = "Banque Du Caire",
            aliases = listOf("bdc", "bdc-alert", "banque du caire", "banqueducaire", "بنك القاهرة", "القاهرة"),
            packageNames = listOf("com.bdc.mobile", "com.banqueducaire.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "QNB",
            standardName = "QNB Alahli",
            aliases = listOf("qnb", "qnb-alert", "qnbalahli", "qnb alahli", "قطر الوطني", "كيو ان بي"),
            packageNames = listOf("com.qnb.qnbalahli", "com.qnb.mobile", "com.qnbalahli.ebanking")
        ),
        ProviderDefinition(
            canonicalKey = "ALEXBANK",
            standardName = "Bank of Alexandria (AlexBank)",
            aliases = listOf("alexbank", "alex bank", "alexbank-alert", "bank of alexandria", "بنك الإسكندرية", "بنك الاسكندرية", "إسكندرية", "اسكندرية"),
            packageNames = listOf("com.alexbank.mobile", "com.alexbank.mahez")
        ),
        ProviderDefinition(
            canonicalKey = "HSBC",
            standardName = "HSBC Egypt",
            aliases = listOf("hsbc", "hsbcegypt", "hsbc egypt", "hsbc-alert", "اتش اس بي سي"),
            packageNames = listOf("com.hsbc.hsbcegypt", "uk.co.hsbc.hsbcukmobilebanking", "com.htsu.hsbcpersonalbanking")
        ),
        ProviderDefinition(
            canonicalKey = "FAISAL",
            standardName = "Faisal Islamic Bank",
            aliases = listOf("faisal", "faisalbank", "faisal islamic bank", "بنك فيصل", "فيصل الإسلامي", "فيصل الاسلامي", "فيصل"),
            packageNames = listOf("com.faisalbank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "AAIB",
            standardName = "Arab African International Bank (AAIB)",
            aliases = listOf("aaib", "aaib-alert", "arab african", "arab african international bank", "العربي الأفريقي", "العربي الافريقي", "البنك العربي الافريقي"),
            packageNames = listOf("com.aaib.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "ADIB",
            standardName = "Abu Dhabi Islamic Bank (ADIB)",
            aliases = listOf("adib", "adib-alert", "adibegypt", "abu dhabi islamic", "أبوظبي الإسلامي", "ابوظبي الاسلامي", "أبو ظبي الإسلامي", "مصرف أبو ظبي الإسلامي"),
            packageNames = listOf("com.adib.mobile", "com.adib.egypt")
        ),
        ProviderDefinition(
            canonicalKey = "CAE",
            standardName = "Credit Agricole Egypt",
            aliases = listOf("cae", "banki", "credit agricole", "creditagricole", "كريدي أجريكول", "كريدي اجريكول", "كريدي"),
            packageNames = listOf("com.creditagricole.egypt", "com.cae.banki")
        ),
        ProviderDefinition(
            canonicalKey = "ENBD",
            standardName = "Emirates NBD Egypt",
            aliases = listOf("enbd", "emirates nbd", "emiratesnbd", "الإمارات دبي", "الامارات دبي", "بنك الإمارات دبي الوطني"),
            packageNames = listOf("com.emiratesnbd.enbd", "com.emiratesnbd.egypt")
        ),
        ProviderDefinition(
            canonicalKey = "HDB",
            standardName = "Housing and Development Bank (HDB)",
            aliases = listOf("hdb", "housing and development", "housingdevelopmentbank", "التعمير والإسكان", "التعمير والاسكان", "بنك التعمير والإسكان"),
            packageNames = listOf("com.hdb.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "EGB",
            standardName = "Egyptian Gulf Bank (EG Bank)",
            aliases = listOf("egb", "eg bank", "egbank", "egyptian gulf bank", "المصري الخليجي", "البنك المصري الخليجي"),
            packageNames = listOf("com.egbank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "SAIB",
            standardName = "SAIB Bank",
            aliases = listOf("saib", "saib bank", "saibbank", "بنك سايب", "الشركة المصرفية العربية الدولية", "الشركة المصرفية"),
            packageNames = listOf("com.saib.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "ALBARAKA",
            standardName = "Al Baraka Bank",
            aliases = listOf("albaraka", "baraka", "al baraka", "abg", "بنك البركة", "البركة"),
            packageNames = listOf("com.albaraka.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "ATTIJARIWAFA",
            standardName = "Attijariwafa Bank Egypt",
            aliases = listOf("attijariwafa", "awb", "التجاري وفا", "التجاري وفا بنك", "وفا بنك"),
            packageNames = listOf("com.attijariwafa.egypt")
        ),
        ProviderDefinition(
            canonicalKey = "ARAB_BANK",
            standardName = "Arab Bank",
            aliases = listOf("arab bank", "arabbank", "arabi mobile", "البنك العربي", "العربي"),
            packageNames = listOf("com.arabbank.arabimobile")
        ),
        ProviderDefinition(
            canonicalKey = "ADCB",
            standardName = "Abu Dhabi Commercial Bank (ADCB)",
            aliases = listOf("adcb", "adcbegypt", "abu dhabi commercial", "أبوظبي التجاري", "ابوظبي التجاري", "بنك أبوظبي التجاري"),
            packageNames = listOf("com.adcb.mobile", "com.adcb.egypt")
        ),
        ProviderDefinition(
            canonicalKey = "EBANK",
            standardName = "EBank (Export Development Bank of Egypt)",
            aliases = listOf("ebank", "edbe", "export development", "تنمية الصادرات", "البنك المصري لتنمية الصادرات"),
            packageNames = listOf("com.ebank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "UNITED_BANK",
            standardName = "The United Bank of Egypt",
            aliases = listOf("united bank", "unitedbank", "ub", "المصرف المتحد"),
            packageNames = listOf("com.theub.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "SUEZ_CANAL",
            standardName = "Suez Canal Bank",
            aliases = listOf("suez canal", "suezcanal", "scb", "قناة السويس", "بنك قناة السويس"),
            packageNames = listOf("com.scbank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "MASHREQ",
            standardName = "Mashreq Bank",
            aliases = listOf("mashreq", "mashreq bank", "mashreq neo", "بنك المشرق", "المشرق"),
            packageNames = listOf("com.mashreq.ae", "com.mashreq.egypt", "com.mashreq.neo")
        ),
        ProviderDefinition(
            canonicalKey = "CITIBANK",
            standardName = "Citibank Egypt",
            aliases = listOf("citibank", "citi", "سيتي بنك"),
            packageNames = listOf("com.citi.citimobile")
        ),
        ProviderDefinition(
            canonicalKey = "FAB",
            standardName = "First Abu Dhabi Bank (FAB / Audi)",
            aliases = listOf("fab", "fabegypt", "fab misr", "bank audi", "bankaudi", "بنك عوده", "أبوظبي الأول", "ابوظبي الاول"),
            packageNames = listOf("com.fab.mobile", "com.fabmisr.mobile", "com.bankaudi.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "ABK",
            standardName = "Al Ahli Bank of Kuwait (ABK)",
            aliases = listOf("abk", "abkegypt", "الأهلي الكويتي", "الاهلي الكويتي"),
            packageNames = listOf("com.abk.egypt")
        ),
        ProviderDefinition(
            canonicalKey = "NBK",
            standardName = "National Bank of Kuwait (NBK)",
            aliases = listOf("nbk", "nbkegypt", "الكويت الوطني", "بنك الكويت الوطني"),
            packageNames = listOf("com.nbk.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "AUB",
            standardName = "Ahli United Bank (AUB)",
            aliases = listOf("aub", "ahli united", "الأهلي المتحد", "الاهلي المتحد"),
            packageNames = listOf("com.aub.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "BANK_ABC",
            standardName = "Bank ABC Egypt",
            aliases = listOf("bank abc", "bankabc", "abc bank", "المؤسسة العربية المصرفية"),
            packageNames = listOf("com.bankabc.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "AIBANK",
            standardName = "aiBank (Arab Investment Bank)",
            aliases = listOf("aibank", "ai bank", "الاستثمار العربي", "بنك الاستثمار العربي"),
            packageNames = listOf("com.aibank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "MIDBANK",
            standardName = "MIDBANK",
            aliases = listOf("midbank", "mid bank", "ميد بنك"),
            packageNames = listOf("com.midbank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "AIB",
            standardName = "Arab International Bank (AIB)",
            aliases = listOf("arab international bank", "aib", "العربي الدولي"),
            packageNames = listOf("com.aib.mobile")
        ),

        // Global & Regional Fintechs / Wallets / Banks (from ProviderCatalog)
        ProviderDefinition(
            canonicalKey = "GOOGLE_WALLET",
            standardName = "Google Wallet",
            aliases = listOf("google wallet", "google pay", "gpay", "محفظة جوجل"),
            packageNames = listOf("com.google.android.apps.walletnfcrel", "com.google.android.apps.nbu.paisa.user")
        ),
        ProviderDefinition(
            canonicalKey = "APPLE_WALLET",
            standardName = "Apple Wallet",
            aliases = listOf("apple wallet", "apple pay")
        ),
        ProviderDefinition(
            canonicalKey = "PAYPAL",
            standardName = "PayPal",
            aliases = listOf("paypal", "باي بال"),
            packageNames = listOf("com.paypal.android.p2pmobile")
        ),
        ProviderDefinition(
            canonicalKey = "REVOLUT",
            standardName = "Revolut",
            aliases = listOf("revolut", "ريفولوت"),
            packageNames = listOf("com.revolut.revolut")
        ),
        ProviderDefinition(
            canonicalKey = "WISE",
            standardName = "Wise",
            aliases = listOf("wise", "transferwise"),
            packageNames = listOf("com.transferwise.android")
        ),
        ProviderDefinition(
            canonicalKey = "MONZO",
            standardName = "Monzo",
            aliases = listOf("monzo"),
            packageNames = listOf("co.uk.getmondo")
        ),
        ProviderDefinition(
            canonicalKey = "N26",
            standardName = "N26",
            aliases = listOf("n26"),
            packageNames = listOf("de.number26.android")
        ),
        ProviderDefinition(
            canonicalKey = "STARLING",
            standardName = "Starling Bank",
            aliases = listOf("starling", "starling bank"),
            packageNames = listOf("com.starlingbank.android")
        ),
        ProviderDefinition(
            canonicalKey = "CHASE",
            standardName = "Chase UK",
            aliases = listOf("chase", "chase uk"),
            packageNames = listOf("com.chase.sig.android", "com.chase.intl")
        ),
        ProviderDefinition(
            canonicalKey = "CASH_APP",
            standardName = "Cash App",
            aliases = listOf("cash app", "cashapp"),
            packageNames = listOf("com.squareup.cash")
        ),
        ProviderDefinition(
            canonicalKey = "VENMO",
            standardName = "Venmo",
            aliases = listOf("venmo"),
            packageNames = listOf("com.venmo")
        ),
        ProviderDefinition(
            canonicalKey = "CHIME",
            standardName = "Chime",
            aliases = listOf("chime"),
            packageNames = listOf("com.onedebit.chime")
        ),
        ProviderDefinition(
            canonicalKey = "SOFI",
            standardName = "SoFi",
            aliases = listOf("sofi"),
            packageNames = listOf("com.sofi.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "WELLS_FARGO",
            standardName = "Wells Fargo",
            aliases = listOf("wells fargo", "wellsfargo"),
            packageNames = listOf("com.wf.wellsfargomobile")
        ),
        ProviderDefinition(
            canonicalKey = "ALLY",
            standardName = "Ally Bank",
            aliases = listOf("ally", "ally bank"),
            packageNames = listOf("com.ally.MobileBanking")
        ),
        ProviderDefinition(
            canonicalKey = "SNB",
            standardName = "SNB",
            aliases = listOf("snb", "alahli", "saudi national bank", "الأهلي السعودي"),
            packageNames = listOf("com.alahli.snbmobile")
        ),
        ProviderDefinition(
            canonicalKey = "STC_BANK",
            standardName = "STC Bank",
            aliases = listOf("stc bank", "stcpay", "stc pay", "اس تي سي"),
            packageNames = listOf("sa.com.stcpay", "com.stc.stcbank")
        ),
        ProviderDefinition(
            canonicalKey = "URPAY",
            standardName = "urpay",
            aliases = listOf("urpay", "يور باي"),
            packageNames = listOf("com.urpay.app")
        ),
        ProviderDefinition(
            canonicalKey = "TIQMO",
            standardName = "tiqmo",
            aliases = listOf("tiqmo", "تيكمو"),
            packageNames = listOf("com.tiqmo.app")
        ),
        ProviderDefinition(
            canonicalKey = "TABBY",
            standardName = "Tabby",
            aliases = listOf("tabby", "تابي"),
            packageNames = listOf("com.tabby.app", "ai.tabby")
        ),
        ProviderDefinition(
            canonicalKey = "TAMARA",
            standardName = "Tamara",
            aliases = listOf("tamara", "تمارا"),
            packageNames = listOf("co.tamara.android")
        ),
        ProviderDefinition(
            canonicalKey = "WIO",
            standardName = "Wio",
            aliases = listOf("wio", "wio bank"),
            packageNames = listOf("io.wio.bank")
        ),
        ProviderDefinition(
            canonicalKey = "LIV",
            standardName = "Liv",
            aliases = listOf("liv", "liv."),
            packageNames = listOf("com.liv.me", "com.emiratesnbd.liv")
        ),
        ProviderDefinition(
            canonicalKey = "RIYAD_BANK",
            standardName = "Riyad Bank",
            aliases = listOf("riyad bank", "riyadbank", "بنك الرياض"),
            packageNames = listOf("com.riyadbank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "RAKBANK",
            standardName = "RAKBANK",
            aliases = listOf("rakbank", "rak bank"),
            packageNames = listOf("com.rakbank")
        ),
        ProviderDefinition(
            canonicalKey = "SAB",
            standardName = "SAB",
            aliases = listOf("sab", "sabb", "ساب"),
            packageNames = listOf("com.sabb")
        ),
        ProviderDefinition(
            canonicalKey = "QIB",
            standardName = "QIB",
            aliases = listOf("qib", "qatar islamic bank"),
            packageNames = listOf("qa.com.qib.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "OPAY",
            standardName = "OPay",
            aliases = listOf("opay"),
            packageNames = listOf("team.opay.pay")
        ),
        ProviderDefinition(
            canonicalKey = "PALMPAY",
            standardName = "PalmPay",
            aliases = listOf("palmpay"),
            packageNames = listOf("com.transsnet.palmpay")
        ),
        ProviderDefinition(
            canonicalKey = "KUDA",
            standardName = "Kuda Bank",
            aliases = listOf("kuda", "kuda bank"),
            packageNames = listOf("com.kudabank.app")
        ),
        ProviderDefinition(
            canonicalKey = "CHIPPER",
            standardName = "Chipper Cash",
            aliases = listOf("chipper", "chipper cash"),
            packageNames = listOf("com.chippercash.app")
        ),
        ProviderDefinition(
            canonicalKey = "MONIEPOINT",
            standardName = "Moniepoint",
            aliases = listOf("moniepoint"),
            packageNames = listOf("com.moniepoint.app")
        ),
        ProviderDefinition(
            canonicalKey = "WAVE",
            standardName = "Wave Mobile Money",
            aliases = listOf("wave", "wave mobile money"),
            packageNames = listOf("com.wave.personal")
        ),
        ProviderDefinition(
            canonicalKey = "PAYTM",
            standardName = "Paytm",
            aliases = listOf("paytm"),
            packageNames = listOf("net.one97.paytm")
        ),
        ProviderDefinition(
            canonicalKey = "PHONEPE",
            standardName = "PhonePe",
            aliases = listOf("phonepe"),
            packageNames = listOf("com.phonepe.app")
        ),
        ProviderDefinition(
            canonicalKey = "HDFC",
            standardName = "HDFC Bank",
            aliases = listOf("hdfc", "hdfc bank"),
            packageNames = listOf("com.snapwork.hdfc")
        ),
        ProviderDefinition(
            canonicalKey = "ICICI",
            standardName = "ICICI Bank",
            aliases = listOf("icici", "icici bank", "imobile"),
            packageNames = listOf("com.csam.icici.bank.imobile")
        ),
        ProviderDefinition(
            canonicalKey = "SBI",
            standardName = "SBI / Yono SBI",
            aliases = listOf("sbi", "yono", "yono sbi"),
            packageNames = listOf("com.sbi.lotusintouch")
        ),
        ProviderDefinition(
            canonicalKey = "KOTAK",
            standardName = "Kotak",
            aliases = listOf("kotak", "kotak bank"),
            packageNames = listOf("com.msf.kbank.mobile")
        ),
        ProviderDefinition(
            canonicalKey = "NUBANK",
            standardName = "Nubank",
            aliases = listOf("nubank", "nu"),
            packageNames = listOf("com.nu.production")
        ),
        ProviderDefinition(
            canonicalKey = "PICPAY",
            standardName = "PicPay",
            aliases = listOf("picpay"),
            packageNames = listOf("com.picpay")
        ),
        ProviderDefinition(
            canonicalKey = "UALA",
            standardName = "Ualá",
            aliases = listOf("uala", "ualá"),
            packageNames = listOf("ar.com.uala")
        ),
        ProviderDefinition(
            canonicalKey = "PAPARA",
            standardName = "Papara",
            aliases = listOf("papara"),
            packageNames = listOf("com.mobillium.papara")
        ),
        ProviderDefinition(
            canonicalKey = "KLARNA",
            standardName = "Klarna",
            aliases = listOf("klarna"),
            packageNames = listOf("com.myklarnamobile")
        ),
        ProviderDefinition(
            canonicalKey = "BUNQ",
            standardName = "bunq",
            aliases = listOf("bunq"),
            packageNames = listOf("com.bunq.android")
        ),
        ProviderDefinition(
            canonicalKey = "PAYONEER",
            standardName = "Payoneer",
            aliases = listOf("payoneer"),
            packageNames = listOf("com.payoneer.android")
        ),
        ProviderDefinition(
            canonicalKey = "REMITLY",
            standardName = "Remitly",
            aliases = listOf("remitly"),
            packageNames = listOf("com.remitly.androidapp")
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
            .replace(".", "")
            .replace(" ", "")
            .trim()
    }

    /**
     * Returns all known aliases across registered providers for SMS/notification sender matching.
     */
    fun getAllKnownSmsTokens(): List<String> {
        return providers.flatMap { it.aliases + it.standardName }.distinct()
    }

    /**
     * Returns true if the installed app looks like a banking, e-wallet, or payment app
     * so Settings and Account Creation pickers can surface relevant apps first.
     */
    fun isLikelyFinancialApp(packageName: String, appLabel: String): Boolean {
        val pkgLower = packageName.lowercase()
        if (providers.any { def -> def.packageNames.any { it.equals(pkgLower, ignoreCase = true) } }) {
            return true
        }
        val combined = "$pkgLower ${appLabel.lowercase()}"
        val financialKeywords = listOf(
            "bank", "wallet", "pay", "cash", "card", "money", "fintech", "finance",
            "credit", "debit", "instapay", "telda", "nexta", "klivvr", "fawry", "valu",
            "khazna", "revolut", "wise", "paypal", "monzo", "venmo", "chime", "tabby", "tamara",
            "بنك", "محفظة", "كاش", "دفع", "باي", "فلوس", "مصرف", "تيلدا", "انستاباي", "فوري"
        )
        return financialKeywords.any { combined.contains(it) }
    }

    /**
     * Scores how well an installed app ([packageName], [appLabel]) matches a chosen [providerOrName].
     * Higher score = stronger match; 0 = no match.
     */
    fun scorePackageMatchForProvider(providerOrName: String?, packageName: String, appLabel: String): Int {
        if (providerOrName.isNullOrBlank()) return 0
        val pkgLower = packageName.lowercase()
        val normPkg = normalize(pkgLower)
        val normLabel = normalize(appLabel)
        val canonicalKey = getCanonicalKey(providerOrName)
        val def = canonicalKey?.let { key -> providers.firstOrNull { it.canonicalKey == key } }

        if (def != null) {
            if (def.packageNames.any { it.equals(pkgLower, ignoreCase = true) }) {
                return 100
            }
            for (alias in def.aliases) {
                val normAlias = normalize(alias)
                if (normAlias.length >= 3) {
                    if (normLabel == normAlias) return 90
                    if (normLabel.contains(normAlias) || normPkg.contains(normAlias)) return 75
                }
            }
        }

        val normProv = normalize(providerOrName)
        if (normProv.length >= 3) {
            if (normLabel == normProv) return 85
            if (normLabel.contains(normProv) || normProv.contains(normLabel)) return 65
            if (normPkg.contains(normProv)) return 60
        }

        val words = providerOrName.lowercase()
            .replace("(", " ").replace(")", " ")
            .split(" ", "-", "_")
            .map { normalize(it) }
            .filter { it.length >= 3 && it !in setOf("bank", "egypt", "cash", "the", "and", "of", "mobile", "wallet", "بنك", "مصر") }
        val wordMatches = words.count { w -> normLabel.contains(w) || normPkg.contains(w) }
        return if (wordMatches > 0) 40 + (wordMatches * 10).coerceAtMost(20) else 0
    }

    /**
     * Resolves the canonical key for a provider name string or package name.
     */
    fun getCanonicalKey(providerOrName: String?): String? {
        if (providerOrName.isNullOrBlank()) return null
        val norm = normalize(providerOrName)
        for (def in providers) {
            if (def.packageNames.any { it.equals(providerOrName, ignoreCase = true) }) {
                return def.canonicalKey
            }
            if (normalize(def.standardName) == norm || normalize(def.canonicalKey) == norm) {
                return def.canonicalKey
            }
            if (def.aliases.any {
                    val normAlias = normalize(it)
                    normAlias.length >= 2 && (normAlias == norm || norm.contains(normAlias) || (norm.length >= 3 && normAlias.contains(norm)))
                }
            ) {
                return def.canonicalKey
            }
        }
        return null
    }

    /**
     * Checks if sender, notification package, or message body matches the provider or account.
     */
    fun matchesProviderOrAccount(
        account: Account,
        sender: String,
        body: String,
        customKeywords: List<String>,
        notificationPackage: String? = null
    ): Boolean {
        val normSender = normalize(sender)
        val normBody = normalize(body)
        val normPkg = notificationPackage?.let { normalize(it) } ?: ""

        // 1. Check account's custom keywords from UserPreferences
        for (kw in customKeywords) {
            val normKw = normalize(kw)
            if (normKw.isNotBlank() && (
                    normSender.contains(normKw) ||
                    normBody.contains(normKw) ||
                    (normSender.length >= 2 && normKw.contains(normSender)) ||
                    (normPkg.isNotBlank() && normPkg.contains(normKw))
                )
            ) {
                return true
            }
        }

        // 2. Check account's direct name and label
        val normName = normalize(account.name)
        if (normName.length >= 3 && (
                normSender.contains(normName) ||
                normBody.contains(normName) ||
                (normPkg.isNotBlank() && normPkg.contains(normName)) ||
                (normName.length <= 10 && normSender.length >= 2 && normName.contains(normSender))
            )
        ) {
            return true
        }
        account.label?.let { label ->
            val normLabel = normalize(label)
            if (normLabel.length >= 3 && (
                    normSender.contains(normLabel) ||
                    normBody.contains(normLabel) ||
                    (normPkg.isNotBlank() && normPkg.contains(normLabel))
                )
            ) {
                return true
            }
        }

        // 3. Check canonical provider definition (aliases + known package names)
        val canonicalKey = getCanonicalKey(account.provider) ?: getCanonicalKey(account.name)
        if (canonicalKey != null) {
            val def = providers.firstOrNull { it.canonicalKey == canonicalKey }
            if (def != null) {
                if (notificationPackage != null &&
                    def.packageNames.any { it.equals(notificationPackage, ignoreCase = true) }
                ) {
                    return true
                }
                for (alias in def.aliases) {
                    val normAlias = normalize(alias)
                    if (normAlias.length >= 2 && (
                            normSender.contains(normAlias) ||
                            (normSender.length >= 3 && normAlias.contains(normSender)) ||
                            normBody.contains(normAlias) ||
                            (normPkg.isNotBlank() && normAlias.length >= 3 && normPkg.contains(normAlias))
                        )
                    ) {
                        return true
                    }
                }
            }
        }

        // 4. Fallback: check raw provider string containment
        account.provider?.takeIf { it.isNotBlank() }?.let { prov ->
            val normProv = normalize(prov)
            if (normProv.length >= 3 && (
                    normSender.contains(normProv) ||
                    (normSender.length >= 3 && normProv.contains(normSender)) ||
                    normBody.contains(normProv) ||
                    (normPkg.isNotBlank() && normPkg.contains(normProv))
                )
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts candidate card/account 4-digit numbers from SMS or notification body.
     */
    fun extractCardOrAccountTokens(body: String): List<String> {
        val tokens = mutableSetOf<String>()

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
     * Checks if an account's card_numbers or bank_account_no matches candidate tokens or body.
     */
    fun matchesCardOrAccount(account: Account, smsTokens: List<String>, body: String): Boolean {
        val accountCardTokens = mutableListOf<String>()

        account.card_numbers?.let { raw ->
            raw.split(',', ';', ' ', '\n', '-', '/').forEach { piece ->
                val clean = piece.replace(" ", "").replace("-", "")
                if (clean.length >= 4) {
                    accountCardTokens.add(clean.takeLast(4))
                    accountCardTokens.add(clean)
                }
            }
        }

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

        for (token in smsTokens) {
            if (accountCardTokens.contains(token)) return true
        }

        for (token in accountCardTokens) {
            if (token.length == 4) {
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
     * High-precision resolver to find the exact matching Account for an incoming SMS or app notification.
     */
    suspend fun resolveAccount(
        context: Context,
        sender: String,
        rawBody: String,
        actionType: SmsActionType,
        notificationPackage: String? = null
    ): Account? {
        val db = PiggyLedgerDatabase.getInstance(context)
        val dao = db.piggyLedgerDao()
        val allAccounts = dao.getAllAccountsSync().filter { !it.is_deleted }

        val userPrefs = UserPreferences(context)
        val preferredAccountId = userPrefs.preferredAccountId.firstOrNull()
        val linkedAccountIds = if (!notificationPackage.isNullOrBlank()) {
            NotificationParserPrefs(context).getAccountsForPackage(notificationPackage)
        } else {
            emptyList()
        }
        val customJsonStr = userPrefs.customIdentifiersJson.firstOrNull() ?: "{}"
        val customMap: Map<String, List<String>> = try {
            Json { ignoreUnknownKeys = true }.decodeFromString(customJsonStr)
        } catch (e: Exception) {
            emptyMap()
        }

        return resolveAccountPure(
            allAccounts = allAccounts,
            sender = sender,
            rawBody = rawBody,
            actionType = actionType,
            notificationPackage = notificationPackage,
            linkedAccountIds = linkedAccountIds,
            preferredAccountId = preferredAccountId,
            customKeywordsMap = customMap
        )
    }

    internal fun resolveAccountPure(
        allAccounts: List<Account>,
        sender: String,
        rawBody: String,
        actionType: SmsActionType,
        notificationPackage: String? = null,
        linkedAccountIds: List<String> = emptyList(),
        preferredAccountId: String? = null,
        customKeywordsMap: Map<String, List<String>> = emptyMap()
    ): Account? {
        if (allAccounts.isEmpty()) {
            logDebug("No accounts exist in app; routing to pending")
            return null
        }

        val body = SmsParser.convertArabicDigitsAndSymbols(rawBody)
        val smsCardTokens = extractCardOrAccountTokens(body)

        // RULE 0: Explicit Notification Package -> Account Mapping (highest specificity for notifications)
        if (!notificationPackage.isNullOrBlank() && linkedAccountIds.isNotEmpty()) {
            val linkedAccounts = allAccounts.filter { it.id in linkedAccountIds }
            if (linkedAccounts.size == 1) {
                logDebug("Notification package $notificationPackage explicitly linked to ${linkedAccounts.first().name}")
                return linkedAccounts.first()
            } else if (linkedAccounts.size > 1) {
                val cardMatch = linkedAccounts.filter { matchesCardOrAccount(it, smsCardTokens, body) }
                if (cardMatch.size == 1) return cardMatch.first()

                disambiguateCandidates(linkedAccounts, body, actionType, preferredAccountId)?.let { return it }
                return null
            }
        }

        // RULE 1: If user has ONLY ONE active account, every valid transaction alert belongs to it
        if (allAccounts.size == 1) {
            logDebug("User has exactly one account (${allAccounts.first().name}); auto-matched")
            return allAccounts.first()
        }

        // RULE 2: Exact Card / Account Number Match
        val exactCardMatches = allAccounts.filter { account ->
            matchesCardOrAccount(account, smsCardTokens, body)
        }
        if (exactCardMatches.size == 1) {
            logDebug("Card/account number uniquely matched ${exactCardMatches.first().name}")
            return exactCardMatches.first()
        } else if (exactCardMatches.size > 1) {
            preferredAccountId?.let { prefId ->
                exactCardMatches.firstOrNull { it.id == prefId }?.let { return it }
            }
            return null
        }

        // RULE 3: Provider, Package & Keyword Matching
        val providerMatches = allAccounts.filter { account ->
            val accountKeywords = mutableListOf<String>()
            account.provider?.let { customKeywordsMap[it]?.let { list -> accountKeywords.addAll(list) } }
            customKeywordsMap[account.name]?.let { list -> accountKeywords.addAll(list) }
            customKeywordsMap[account.id]?.let { list -> accountKeywords.addAll(list) }

            matchesProviderOrAccount(
                account = account,
                sender = sender,
                body = body,
                customKeywords = accountKeywords,
                notificationPackage = notificationPackage
            )
        }

        val isInstaPay = sender.contains("instapay", ignoreCase = true) ||
            (notificationPackage?.contains("instapay", ignoreCase = true) == true) ||
            rawBody.contains("instapay", ignoreCase = true) ||
            rawBody.contains("انستاباي") ||
            rawBody.contains("المدفوعات اللحظية")

        var candidateAccounts = providerMatches

        if (isInstaPay && candidateAccounts.isEmpty()) {
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
            logDebug("Provider uniquely matched ${candidateAccounts.first().name}")
            return candidateAccounts.first()
        }

        if (candidateAccounts.size > 1) {
            return disambiguateCandidates(candidateAccounts, body, actionType, preferredAccountId)
        }

        return null
    }

    private fun disambiguateCandidates(
        candidateAccounts: List<Account>,
        body: String,
        actionType: SmsActionType,
        preferredAccountId: String?
    ): Account? {
        if (preferredAccountId != null) {
            candidateAccounts.firstOrNull { it.id == preferredAccountId }?.let {
                logDebug("Disambiguated to preferred account: ${it.name}")
                return it
            }
        }

        val isCardTransaction = actionType == SmsActionType.PURCHASE ||
            body.contains("بطاقة") || body.contains("card", ignoreCase = true) ||
            body.contains("شراء") || body.contains("purchase", ignoreCase = true) ||
            body.contains("ائتمان") || body.contains("credit", ignoreCase = true) ||
            body.contains("pos", ignoreCase = true)

        val isBankTransaction = actionType == SmsActionType.TRANSFER_OUT ||
            actionType == SmsActionType.DEPOSIT ||
            actionType == SmsActionType.WITHDRAWAL ||
            body.contains("تحويل") || body.contains("transfer", ignoreCase = true) ||
            body.contains("ايداع") || body.contains("حساب") ||
            body.contains("مرتب") || body.contains("salary", ignoreCase = true)

        if (isCardTransaction) {
            val cardAccounts = candidateAccounts.filter { it.type == AccountType.CARD }
            if (cardAccounts.size == 1) return cardAccounts.first()
        }

        if (isBankTransaction) {
            val bankAccounts = candidateAccounts.filter { it.type == AccountType.BANK }
            if (bankAccounts.size == 1) return bankAccounts.first()
        }

        val nameMatches = candidateAccounts.filter { account ->
            account.name.isNotBlank() && body.contains(account.name, ignoreCase = true)
        }
        if (nameMatches.size == 1) return nameMatches.first()

        logDebug("Genuine ambiguity between ${candidateAccounts.size} accounts; sending to pending")
        return null
    }

    private fun logDebug(message: String) {
        try {
            Log.d("BankProviderRegistry", message)
        } catch (_: Throwable) {
            // Ignored in pure JVM unit tests where android.util.Log is not mocked
        }
    }
}
