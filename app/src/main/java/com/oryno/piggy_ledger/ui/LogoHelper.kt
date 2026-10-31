package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.AccountType

fun getProviderDrawableRes(providerName: String?): Int? {
    val clean = providerName?.trim()?.lowercase() ?: return null
    return when {
        clean.contains("vodafone") -> R.drawable.logo_vodafone_cash
        clean.contains("orange") -> R.drawable.logo_orange_cash
        clean.contains("e&") || clean.contains("etisalat") -> R.drawable.logo_etisalat_cash
        clean.contains("we pay") || clean == "we" || clean.contains("telecom egypt") -> R.drawable.logo_we_pay
        clean.contains("instapay") || clean.contains("انستاباي") -> R.drawable.logo_instapay
        clean.contains("telda") -> R.drawable.logo_telda
        clean.contains("qnb") || clean.contains("قطر الوطني") -> R.drawable.logo_qnb
        clean.contains("cib") || clean.contains("commercial international") || clean.contains("التجاري الدولي") -> R.drawable.logo_cib
        clean.contains("abk") || clean.contains("أهلي كويتي") || clean.contains("الاهلي الكويتي") || clean.contains("الأهلي الكويتي") -> R.drawable.logo_abk
        clean.contains("aub") || clean.contains("ahli united") || clean.contains("الأهلي المتحد") || clean.contains("الاهلي المتحد") -> R.drawable.logo_aub
        clean.contains("national bank of egypt") || clean.contains("nbe") || clean.contains("الأهلي المصري") || clean.contains("الاهلي المصري") || clean.contains("أهلي مصري") || clean.contains("البنك الأهلي") || clean.contains("البنك الاهلي") || clean == "الأهلي" || clean == "الاهلي" -> R.drawable.logo_nbe
        clean.contains("banque misr") || clean.contains("بنك مصر") -> R.drawable.logo_banque_misr
        clean.contains("banque du caire") || clean.contains("بنك القاهرة") || clean.contains("caire") -> R.drawable.logo_banque_du_caire
        clean.contains("alex") || clean.contains("إسكندرية") || clean.contains("اسكندرية") -> R.drawable.logo_alexbank
        clean.contains("arab african") || clean.contains("aaib") || clean.contains("العربي الأفريقي") || clean.contains("العربي الافريقي") -> R.drawable.logo_aaib
        clean.contains("abu dhabi islamic") || clean.contains("adib") || clean.contains("أبوظبي الإسلامي") || clean.contains("ابوظبي الاسلامي") -> R.drawable.logo_adib
        clean.contains("abu dhabi commercial") || clean.contains("adcb") || clean.contains("أبوظبي التجاري") || clean.contains("ابوظبي التجاري") -> R.drawable.logo_adcb
        clean.contains("first abu dhabi") || clean.contains("fab") || clean.contains("أبوظبي الأول") || clean.contains("ابوظبي الاول") || clean.contains("عوده") -> R.drawable.logo_fab
        clean.contains("agricole") || clean.contains("كريدي أجريكول") || clean.contains("كريدي اجريكول") -> R.drawable.logo_cae
        clean.contains("emirates nbd") || clean.contains("الإمارات دبي") || clean.contains("الامارات دبي") -> R.drawable.logo_enbd
        clean.contains("housing and development") || clean.contains("hdb") || clean.contains("التعمير والإسكان") || clean.contains("التعمير والاسكان") -> R.drawable.logo_hdb
        clean.contains("eg bank") || clean.contains("egyptian gulf") || clean.contains("المصري الخليجي") -> R.drawable.logo_egb
        clean.contains("saib") || clean.contains("سيب") -> R.drawable.logo_saib
        clean.contains("albaraka") || clean.contains("baraka") || clean.contains("البركة") -> R.drawable.logo_albaraka
        clean.contains("attijariwafa") || clean.contains("التجاري وفا") -> R.drawable.logo_attijariwafa
        clean.contains("arab bank") || clean.contains("البنك العربي") -> R.drawable.logo_arab_bank
        clean.contains("hsbc") -> R.drawable.logo_hsbc
        clean.contains("faisal") || clean.contains("فيصل") -> R.drawable.logo_faisal
        clean.contains("citibank") || clean.contains("سيتي بنك") -> R.drawable.logo_citibank
        clean.contains("mashreq") || clean.contains("المشرق") -> R.drawable.logo_mashreq
        clean.contains("suez canal") || clean.contains("قناة السويس") -> R.drawable.logo_suez_canal
        clean.contains("united bank") || clean.contains("المتحد") -> R.drawable.logo_united_bank
        clean.contains("ebank") || clean.contains("تنمية الصادرات") -> R.drawable.logo_ebank
        clean.contains("nbk") || clean.contains("الكويت الوطني") -> R.drawable.logo_nbk
        clean.contains("bank abc") || clean.contains("المؤسسة العربية المصرفية") -> R.drawable.logo_bank_abc
        clean.contains("aibank") || clean.contains("الاستثمار العربي") -> R.drawable.logo_aibank
        clean.contains("midbank") || clean.contains("ميد بنك") || clean.contains("ميدبنك") -> R.drawable.logo_midbank
        clean.contains("arab international bank") || clean.contains("العربي الدولي") -> R.drawable.logo_arab_international_bank
        clean.contains("carbon") -> R.drawable.logo_carbon
        clean.contains("chipper cash") -> R.drawable.logo_chipper_cash
        clean.contains("discovery bank") -> R.drawable.logo_discovery_bank
        clean.contains("khazna") -> R.drawable.logo_khazna
        clean.contains("kuda bank") -> R.drawable.logo_kuda_bank
        clean.contains("moniepoint") -> R.drawable.logo_moniepoint
        clean.contains("opay") -> R.drawable.logo_opay
        clean.contains("palmpay") -> R.drawable.logo_palmpay
        clean.contains("tymebank") -> R.drawable.logo_tymebank
        clean.contains("wave mobile money") -> R.drawable.logo_wave_mobile_money
        clean.contains("gxbank") -> R.drawable.logo_gxbank
        clean.contains("hdfc bank") -> R.drawable.logo_hdfc_bank
        clean.contains("icici bank") -> R.drawable.logo_icici_bank
        clean.contains("idfc first bank") -> R.drawable.logo_idfc_first_bank
        clean.contains("kakaobank") -> R.drawable.logo_kakaobank
        clean.contains("kotak") -> R.drawable.logo_kotak
        clean.contains("mybank") -> R.drawable.logo_mybank
        clean.contains("paytm") -> R.drawable.logo_paytm
        clean.contains("phonepe") -> R.drawable.logo_phonepe
        clean.contains("rakuten bank") -> R.drawable.logo_rakuten_bank
        clean.contains("sbi / yono sbi") -> R.drawable.logo_sbi_yono_sbi
        clean.contains("sbi sumishin net bank") -> R.drawable.logo_sbi_sumishin_net_bank
        clean.contains("tonik bank") -> R.drawable.logo_tonik_bank
        clean.contains("toss bank") -> R.drawable.logo_toss_bank
        clean.contains("webank") -> R.drawable.logo_webank
        clean.contains("yes bank") -> R.drawable.logo_yes_bank
        clean.contains("chase uk") -> R.drawable.logo_chase_uk
        clean.contains("ing") -> R.drawable.logo_ing
        clean.contains("klarna") -> R.drawable.logo_klarna
        clean.contains("kroo") -> R.drawable.logo_kroo
        clean.contains("lloyds bank") -> R.drawable.logo_lloyds_bank
        clean.contains("monese") -> R.drawable.logo_monese
        clean.contains("monzo") -> R.drawable.logo_monzo
        clean.contains("n26") -> R.drawable.logo_n26
        clean.contains("natwest") -> R.drawable.logo_natwest
        clean.contains("papara") -> R.drawable.logo_papara
        clean.contains("qonto") -> R.drawable.logo_qonto
        clean.contains("revolut") -> R.drawable.logo_revolut
        clean.contains("santander") -> R.drawable.logo_santander
        clean.contains("starling bank") -> R.drawable.logo_starling_bank
        clean.contains("trade republic") -> R.drawable.logo_trade_republic
        clean.contains("wise") -> R.drawable.logo_wise
        clean.contains("bunq") -> R.drawable.logo_bunq
        clean.contains("airwallex") -> R.drawable.logo_airwallex
        clean.contains("apple wallet") -> R.drawable.logo_apple_wallet
        clean.contains("google wallet") -> R.drawable.logo_google_wallet
        clean.contains("payoneer") -> R.drawable.logo_payoneer
        clean.contains("plaid") -> R.drawable.logo_plaid
        clean.contains("remitly") -> R.drawable.logo_remitly
        clean.contains("stripe") -> R.drawable.logo_stripe
        clean.contains("fab") -> R.drawable.logo_fab
        clean.contains("gulf bank") -> R.drawable.logo_gulf_bank
        clean.contains("liv") -> R.drawable.logo_liv
        clean.contains("mashreq neo") -> R.drawable.logo_mashreq_neo
        clean.contains("nbb") -> R.drawable.logo_nbb
        clean.contains("neo") -> R.drawable.logo_neo
        clean.contains("one zero") -> R.drawable.logo_one_zero
        clean.contains("qib") -> R.drawable.logo_qib
        clean.contains("rakbank") -> R.drawable.logo_rakbank
        clean.contains("riyad bank") -> R.drawable.logo_riyad_bank
        clean.contains("sab") -> R.drawable.logo_sab
        clean.contains("snb") -> R.drawable.logo_snb
        clean.contains("stc bank") -> R.drawable.logo_stc_bank
        clean.contains("saudi investment bank") -> R.drawable.logo_saudi_investment_bank
        clean.contains("sohar international") -> R.drawable.logo_sohar_international
        clean.contains("tabby") -> R.drawable.logo_tabby
        clean.contains("tamara") -> R.drawable.logo_tamara
        clean.contains("warba bank") -> R.drawable.logo_warba_bank
        clean.contains("weyay bank") -> R.drawable.logo_weyay_bank
        clean.contains("wio") -> R.drawable.logo_wio
        clean.contains("ila bank") -> R.drawable.logo_ila_bank
        clean.contains("tiqmo") -> R.drawable.logo_tiqmo
        clean.contains("urpay") -> R.drawable.logo_urpay
        clean.contains("albert") -> R.drawable.logo_albert
        clean.contains("ally bank") -> R.drawable.logo_ally_bank
        clean.contains("brex") -> R.drawable.logo_brex
        clean.contains("cash app") -> R.drawable.logo_cash_app
        clean.contains("chime") -> R.drawable.logo_chime
        clean.contains("current") -> R.drawable.logo_current
        clean.contains("koho") -> R.drawable.logo_koho
        clean.contains("mercury") -> R.drawable.logo_mercury
        clean.contains("moneylion") -> R.drawable.logo_moneylion
        clean.contains("neo financial") -> R.drawable.logo_neo_financial
        clean.contains("paypal") -> R.drawable.logo_paypal
        clean.contains("ramp") -> R.drawable.logo_ramp
        clean.contains("sofi") -> R.drawable.logo_sofi
        clean.contains("varo bank") -> R.drawable.logo_varo_bank
        clean.contains("venmo") -> R.drawable.logo_venmo
        clean.contains("wealthsimple") -> R.drawable.logo_wealthsimple
        clean.contains("wells fargo") -> R.drawable.logo_wells_fargo
        clean.contains("alex bank") -> R.drawable.logo_alex_bank
        clean.contains("douugh") -> R.drawable.logo_douugh
        clean.contains("heartland bank") -> R.drawable.logo_heartland_bank
        clean.contains("judo bank") -> R.drawable.logo_judo_bank
        clean.contains("kiwibank") -> R.drawable.logo_kiwibank
        clean.contains("stake") -> R.drawable.logo_stake
        clean.contains("up bank") -> R.drawable.logo_up_bank
        clean.contains("zeller") -> R.drawable.logo_zeller
        clean.contains("albo") -> R.drawable.logo_albo
        clean.contains("banco inter") -> R.drawable.logo_banco_inter
        clean.contains("bnext") -> R.drawable.logo_bnext
        clean.contains("c6 bank") -> R.drawable.logo_c6_bank
        clean.contains("klar") -> R.drawable.logo_klar
        clean.contains("neon") -> R.drawable.logo_neon
        clean.contains("nubank") -> R.drawable.logo_nubank
        clean.contains("picpay") -> R.drawable.logo_picpay
        clean.contains("ualá") -> R.drawable.logo_ual
        clean.contains("apple wallet") -> R.drawable.logo_apple_wallet
        clean.contains("google wallet") -> R.drawable.logo_google_wallet
        else -> null
    }
}

data class BrandConfig(
    val initials: String,
    val brandColor: Color,
    val drawableRes: Int? = null
)

fun getBrandConfig(providerName: String?, accountType: AccountType): BrandConfig {
    val name = providerName?.trim() ?: ""
    val clean = name.lowercase()
    
    // Check known wallet names
    if (clean.contains("vodafone")) {
        return BrandConfig("VF", Color(0xFFE60000), getProviderDrawableRes(providerName))
    } else if (clean.contains("orange")) {
        return BrandConfig("OR", Color(0xFFFF7900), getProviderDrawableRes(providerName))
    } else if (clean.contains("e&") || clean.contains("etisalat")) {
        return BrandConfig("e&", Color(0xFF749B3E), getProviderDrawableRes(providerName))
    } else if (clean.contains("we pay") || clean == "we" || clean.contains("telecom egypt")) {
        return BrandConfig("WE", Color(0xFF7C3AED), getProviderDrawableRes(providerName))
    } else if (clean.contains("instapay") || clean.contains("انستاباي")) {
        return BrandConfig("IP", Color(0xFF701A75), getProviderDrawableRes(providerName))
    } else if (clean.contains("telda")) {
        return BrandConfig("TD", Color(0xFF0F172A), getProviderDrawableRes(providerName))
    }

    // Match bank names
    return when {
        clean.contains("qnb") || clean.contains("قطر الوطني") -> {
            BrandConfig("QNB", Color(0xFF6F1E51), getProviderDrawableRes(providerName))
        }
        clean.contains("cib") || clean.contains("commercial international") || clean.contains("التجاري الدولي") -> {
            BrandConfig("CIB", Color(0xFF1E3A8A), getProviderDrawableRes(providerName))
        }
        clean.contains("abk") || clean.contains("أهلي كويتي") || clean.contains("الاهلي الكويتي") || clean.contains("الأهلي الكويتي") -> {
            BrandConfig("ABK", Color(0xFF003366), getProviderDrawableRes(providerName))
        }
        clean.contains("aub") || clean.contains("ahli united") || clean.contains("الأهلي المتحد") || clean.contains("الاهلي المتحد") -> {
            BrandConfig("AUB", Color(0xFF800020), getProviderDrawableRes(providerName))
        }
        clean.contains("national bank of egypt") || clean.contains("nbe") || clean.contains("الأهلي المصري") || clean.contains("الاهلي المصري") || clean.contains("أهلي مصري") || clean.contains("البنك الأهلي") || clean.contains("البنك الاهلي") || clean == "الأهلي" || clean == "الاهلي" -> {
            BrandConfig("NBE", Color(0xFF005B41), getProviderDrawableRes(providerName))
        }
        clean.contains("banque misr") || clean.contains("بنك مصر") -> {
            BrandConfig("BM", Color(0xFF900C3F), getProviderDrawableRes(providerName))
        }
        clean.contains("banque du caire") || clean.contains("بنك القاهرة") || clean.contains("caire") -> {
            BrandConfig("BDC", Color(0xFFE05B00), getProviderDrawableRes(providerName))
        }
        clean.contains("alex") || clean.contains("إسكندرية") || clean.contains("اسكندرية") -> {
            BrandConfig("ALX", Color(0xFF4D1A7F), getProviderDrawableRes(providerName))
        }
        clean.contains("arab african") || clean.contains("aaib") || clean.contains("العربي الأفريقي") || clean.contains("العربي الافريقي") -> {
            BrandConfig("AAIB", Color(0xFF1E3A8A), getProviderDrawableRes(providerName))
        }
        clean.contains("abu dhabi islamic") || clean.contains("adib") || clean.contains("أبوظبي الإسلامي") || clean.contains("ابوظبي الاسلامي") -> {
            BrandConfig("ADIB", Color(0xFF0D9488), getProviderDrawableRes(providerName))
        }
        clean.contains("abu dhabi commercial") || clean.contains("adcb") || clean.contains("أبوظبي التجاري") || clean.contains("ابوظبي التجاري") -> {
            BrandConfig("ADCB", Color(0xFFD32F2F), getProviderDrawableRes(providerName))
        }
        clean.contains("first abu dhabi") || clean.contains("fab") || clean.contains("أبوظبي الأول") || clean.contains("ابوظبي الاول") || clean.contains("عوده") -> {
            BrandConfig("FAB", Color(0xFFC8102E), getProviderDrawableRes(providerName))
        }
        clean.contains("agricole") || clean.contains("كريدي أجريكول") || clean.contains("كريدي اجريكول") -> {
            BrandConfig("CAE", Color(0xFF059669), getProviderDrawableRes(providerName))
        }
        clean.contains("emirates nbd") || clean.contains("الإمارات دبي") || clean.contains("الامارات دبي") -> {
            BrandConfig("ENBD", Color(0xFF1E3A8A), getProviderDrawableRes(providerName))
        }
        clean.contains("housing and development") || clean.contains("hdb") || clean.contains("التعمير والإسكان") || clean.contains("التعمير والاسكان") -> {
            BrandConfig("HDB", Color(0xFF059669), getProviderDrawableRes(providerName))
        }
        clean.contains("eg bank") || clean.contains("egyptian gulf") || clean.contains("المصري الخليجي") -> {
            BrandConfig("EGB", Color(0xFF0A2540), getProviderDrawableRes(providerName))
        }
        clean.contains("saib") || clean.contains("سيب") -> {
            BrandConfig("SAIB", Color(0xFF1E3A8A), getProviderDrawableRes(providerName))
        }
        clean.contains("albaraka") || clean.contains("baraka") || clean.contains("البركة") -> {
            BrandConfig("ABG", Color(0xFF8B5A2B), getProviderDrawableRes(providerName))
        }
        clean.contains("attijariwafa") || clean.contains("التجاري وفا") -> {
            BrandConfig("AWB", Color(0xFFEAB308), getProviderDrawableRes(providerName))
        }
        clean.contains("arab bank") || clean.contains("البنك العربي") -> {
            BrandConfig("ARAB", Color(0xFF006400), getProviderDrawableRes(providerName))
        }
        clean.contains("hsbc") -> {
            BrandConfig("HSBC", Color(0xFFE50914), getProviderDrawableRes(providerName))
        }
        clean.contains("faisal") || clean.contains("فيصل") -> {
            BrandConfig("FIB", Color(0xFF1B4D3E), getProviderDrawableRes(providerName))
        }
        clean.contains("citibank") || clean.contains("سيتي بنك") -> {
            BrandConfig("CITI", Color(0xFF0288D1), getProviderDrawableRes(providerName))
        }
        clean.contains("mashreq") || clean.contains("المشرق") -> {
            BrandConfig("MSH", Color(0xFFFF5722), getProviderDrawableRes(providerName))
        }
        clean.contains("suez canal") || clean.contains("قناة السويس") -> {
            BrandConfig("SCB", Color(0xFF0D47A1), getProviderDrawableRes(providerName))
        }
        clean.contains("united bank") || clean.contains("المتحد") -> {
            BrandConfig("UB", Color(0xFF1565C0), getProviderDrawableRes(providerName))
        }
        clean.contains("ebank") || clean.contains("تنمية الصادرات") -> {
            BrandConfig("EB", Color(0xFF008080), getProviderDrawableRes(providerName))
        }
        clean.contains("nbk") || clean.contains("الكويت الوطني") -> {
            BrandConfig("NBK", Color(0xFF002A54), getProviderDrawableRes(providerName))
        }
        clean.contains("bank abc") || clean.contains("المؤسسة العربية المصرفية") -> {
            BrandConfig("ABC", Color(0xFF008080), getProviderDrawableRes(providerName))
        }
        clean.contains("aibank") || clean.contains("الاستثمار العربي") -> {
            BrandConfig("AIB", Color(0xFF0F172A), getProviderDrawableRes(providerName))
        }
        clean.contains("midbank") || clean.contains("ميد بنك") || clean.contains("ميدبنك") -> {
            BrandConfig("MID", Color(0xFF4A154B), getProviderDrawableRes(providerName))
        }
        clean.contains("arab international bank") || clean.contains("العربي الدولي") -> {
            BrandConfig("AIB", Color(0xFF102A43), getProviderDrawableRes(providerName))
        }
        else -> {
            val defaultInitials = if (name.isNotEmpty()) {
                name.split(" ").filter { it.isNotEmpty() }.map { it.take(1) }.joinToString("").take(3).uppercase()
            } else {
                when (accountType) {
                    AccountType.BANK -> "BNK"
                    AccountType.CARD -> "CRD"
                    AccountType.WALLET -> "WLT"
                    AccountType.CASH -> "CSH"
                    AccountType.INVESTMENT -> "INV"
                    AccountType.ASSET_GOLD -> "GLD"
                    AccountType.ASSET_SILVER -> "SLV"
                    AccountType.CERTIFICATE -> "CRT"
                    AccountType.ASSET_PROPERTY -> "PRP"
                    else -> "ACC"
                }
            }
            val defaultColor = when (accountType) {
                AccountType.BANK -> Color(0xFF1E293B)
                AccountType.CARD -> Color(0xFF0F172A)
                AccountType.WALLET -> Color(0xFF0D9488)
                AccountType.CASH -> Color(0xFF059669)
                AccountType.ASSET_GOLD -> Color(0xFFD97706)
                AccountType.ASSET_SILVER -> Color(0xFF64748B)
                else -> Color(0xFF6366F1)
            }
            BrandConfig(defaultInitials, defaultColor, getProviderDrawableRes(providerName))
        }
    }
}

const val LOGO_DEV_TOKEN = "pk_czS62GgySjmeVrI9uvB4kg"

/**
 * Builds a Logo.dev URL using the provided identifier (domain, name/{Name}, ticker/{Ticker}, etc.)
 * with the publishable key, transparent PNG formatting, size 128, retina, and fallback=404.
 */
fun buildLogoDevUrl(domainOrIdentifier: String): String {
    val clean = domainOrIdentifier.trim()
    val path = if (clean.startsWith("name/") || clean.startsWith("ticker/") || clean.startsWith("isin/") || clean.startsWith("crypto/") || clean.contains(".")) {
        clean
    } else {
        "name/$clean"
    }
    return "https://img.logo.dev/$path?token=$LOGO_DEV_TOKEN&size=128&format=png&retina=true&fallback=404"
}

fun getOnlineProviderLogoUrl(providerName: String?): String? {
    if (providerName.isNullOrBlank()) return null
    val name = providerName.trim()
    val clean = name.lowercase()
    
    val domain = when {
        // Wallets
        clean.contains("vodafone") -> "vodafone.com"
        clean.contains("orange") -> "orange.com"
        clean.contains("etisalat") || clean.contains("e&") -> "eand.com"
        clean.contains("we pay") || clean.contains("telecom egypt") || clean == "we" -> "te.eg"
        clean.contains("instapay") || clean.contains("انستاباي") -> "instapay.eg"
        clean.contains("telda") -> "telda.app"

        // Specific Banks First (prevent false collisions with الأهلي or مصر)
        clean.contains("qnb") || clean.contains("قطر الوطني") -> "qnbalahli.com"
        clean.contains("cib") || clean.contains("commercial international") || clean.contains("التجاري الدولي") -> "cibeg.com"
        clean.contains("abk") || (clean.contains("أهلي") && clean.contains("كويت")) || (clean.contains("اهلي") && clean.contains("كويت")) -> "abkegypt.com"
        clean.contains("aub") || (clean.contains("أهلي") && clean.contains("متحد")) || (clean.contains("اهلي") && clean.contains("متحد")) -> "aub.com.eg"
        clean.contains("national bank of egypt") || clean.contains("nbe") || clean.contains("الأهلي المصري") || clean.contains("الاهلي المصري") || clean.contains("أهلي مصري") || clean.contains("البنك الأهلي") || clean.contains("البنك الاهلي") || (clean.contains("أهلي") && !clean.contains("كويت") && !clean.contains("متحد")) || (clean.contains("اهلي") && !clean.contains("كويت") && !clean.contains("متحد")) -> "nbe.com.eg"
        
        clean.contains("banque misr") || clean.contains("بنك مصر") -> "banquemisr.com"
        clean.contains("banque du caire") || clean.contains("بنك القاهرة") || clean.contains("caire") -> "bdc.com.eg"
        clean.contains("alex") || clean.contains("إسكندرية") || clean.contains("اسكندرية") -> "alexbank.com"
        clean.contains("arab african") || clean.contains("aaib") || clean.contains("العربي الأفريقي") || clean.contains("العربي الافريقي") -> "aaib.com"
        clean.contains("abu dhabi islamic") || clean.contains("adib") || clean.contains("أبوظبي الإسلامي") || clean.contains("ابوظبي الاسلامي") -> "adib.eg"
        clean.contains("abu dhabi commercial") || clean.contains("adcb") || clean.contains("أبوظبي التجاري") || clean.contains("ابوظبي التجاري") -> "adcb.com.eg"
        clean.contains("first abu dhabi") || clean.contains("fab") || clean.contains("أبوظبي الأول") || clean.contains("ابوظبي الاول") || clean.contains("عوده") -> "bankfab.com"
        clean.contains("agricole") || clean.contains("كريدي أجريكول") || clean.contains("كريدي اجريكول") -> "credit-agricole.com.eg"
        clean.contains("emirates nbd") || clean.contains("الإمارات دبي") || clean.contains("الامارات دبي") -> "emiratesnbd.com.eg"
        clean.contains("housing and development") || clean.contains("hdb") || clean.contains("التعمير والإسكان") || clean.contains("التعمير والاسكان") -> "hdb-egypt.com"
        clean.contains("eg bank") || clean.contains("egyptian gulf") || clean.contains("المصري الخليجي") -> "eg-bank.com"
        clean.contains("saib") || clean.contains("سيب") -> "saib.com.eg"
        clean.contains("albaraka") || clean.contains("baraka") || clean.contains("البركة") -> "albaraka.com.eg"
        clean.contains("attijariwafa") || clean.contains("التجاري وفا") -> "attijariwafabank.com.eg"
        clean.contains("arab bank") || clean.contains("البنك العربي") -> "arabbank.com.eg"
        clean.contains("hsbc") -> "hsbc.com.eg"
        clean.contains("faisal") || clean.contains("فيصل") -> "faisalbank.com.eg"
        clean.contains("citibank") || clean.contains("سيتي بنك") -> "citibank.com"
        clean.contains("mashreq") || clean.contains("المشرق") -> "mashreqbank.com"
        clean.contains("suez canal") || clean.contains("قناة السويس") -> "scbank.com.eg"
        clean.contains("united bank") || clean.contains("المتحد") -> "theubeg.com"
        clean.contains("ebank") || clean.contains("تنمية الصادرات") -> "ebank.com.eg"
        clean.contains("nbk") || clean.contains("الكويت الوطني") -> "nbk.com"
        clean.contains("bank abc") || clean.contains("المؤسسة العربية المصرفية") -> "bank-abc.com"
        clean.contains("aibank") || clean.contains("الاستثمار العربي") -> "aibank.com.eg"
        clean.contains("midbank") || clean.contains("ميد بنك") || clean.contains("ميدبنك") -> "midbank.com.eg"
        clean.contains("arab international bank") || clean.contains("العربي الدولي") -> "aib.com.eg"
        else -> null
    }

    return if (domain != null) {
        buildLogoDevUrl(domain)
    } else {
        buildLogoDevUrl("name/$name")
    }
}

@Composable
fun BrandLogo(
    provider: String?,
    accountType: AccountType,
    iconColorHex: String?,
    modifier: Modifier = Modifier,
    logoUrl: String? = null
) {
    val onlineUrl = logoUrl ?: getOnlineProviderLogoUrl(provider)
    val config = getBrandConfig(provider, accountType)
    
    val bg = if (iconColorHex != null) {
        try {
            Color(android.graphics.Color.parseColor(iconColorHex))
        } catch (e: Exception) {
            config.brandColor
        }
    } else {
        config.brandColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (config.drawableRes != null) {
            Image(
                painter = painterResource(id = config.drawableRes),
                contentDescription = provider ?: "Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentScale = ContentScale.Fit
            )
        } else if (!onlineUrl.isNullOrEmpty()) {
            coil.compose.SubcomposeAsyncImage(
                model = onlineUrl,
                contentDescription = provider ?: "Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentScale = ContentScale.Fit,
                loading = {
                    MonogramBadge(config = config, bg = bg)
                },
                error = {
                    if (config.drawableRes != null) {
                        Image(
                            painter = painterResource(id = config.drawableRes),
                            contentDescription = provider ?: "Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        MonogramBadge(config = config, bg = bg)
                    }
                }
            )
        } else if (config.drawableRes != null) {
            Image(
                painter = painterResource(id = config.drawableRes),
                contentDescription = provider ?: "Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            MonogramBadge(config = config, bg = bg)
        }
    }
}

@Composable
private fun MonogramBadge(
    config: BrandConfig,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = config.initials,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp,
            maxLines = 1
        )
    }
}
