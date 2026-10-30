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

fun getProviderDrawableRes(providerName: String?): Int? = null

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
        return BrandConfig("VF", Color(0xFFE60000))
    } else if (clean.contains("orange")) {
        return BrandConfig("OR", Color(0xFFFF7900))
    } else if (clean.contains("e&") || clean.contains("etisalat")) {
        return BrandConfig("e&", Color(0xFF749B3E))
    } else if (clean.contains("we pay") || clean == "we" || clean.contains("telecom egypt")) {
        return BrandConfig("WE", Color(0xFF7C3AED))
    } else if (clean.contains("instapay") || clean.contains("انستاباي")) {
        return BrandConfig("IP", Color(0xFF701A75))
    } else if (clean.contains("telda")) {
        return BrandConfig("TD", Color(0xFF0F172A))
    }

    // Match bank names
    return when {
        clean.contains("qnb") || clean.contains("قطر الوطني") -> {
            BrandConfig("QNB", Color(0xFF6F1E51))
        }
        clean.contains("cib") || clean.contains("commercial international") || clean.contains("التجاري الدولي") -> {
            BrandConfig("CIB", Color(0xFF1E3A8A))
        }
        clean.contains("abk") || clean.contains("أهلي كويتي") || clean.contains("الاهلي الكويتي") || clean.contains("الأهلي الكويتي") -> {
            BrandConfig("ABK", Color(0xFF003366))
        }
        clean.contains("aub") || clean.contains("ahli united") || clean.contains("الأهلي المتحد") || clean.contains("الاهلي المتحد") -> {
            BrandConfig("AUB", Color(0xFF800020))
        }
        clean.contains("national bank of egypt") || clean.contains("nbe") || clean.contains("الأهلي المصري") || clean.contains("الاهلي المصري") || clean.contains("أهلي مصري") || clean.contains("البنك الأهلي") || clean.contains("البنك الاهلي") || clean == "الأهلي" || clean == "الاهلي" -> {
            BrandConfig("NBE", Color(0xFF005B41))
        }
        clean.contains("banque misr") || clean.contains("بنك مصر") -> {
            BrandConfig("BM", Color(0xFF900C3F))
        }
        clean.contains("banque du caire") || clean.contains("بنك القاهرة") || clean.contains("caire") -> {
            BrandConfig("BDC", Color(0xFFE05B00))
        }
        clean.contains("alex") || clean.contains("إسكندرية") || clean.contains("اسكندرية") -> {
            BrandConfig("ALX", Color(0xFF4D1A7F))
        }
        clean.contains("arab african") || clean.contains("aaib") || clean.contains("العربي الأفريقي") || clean.contains("العربي الافريقي") -> {
            BrandConfig("AAIB", Color(0xFF1E3A8A))
        }
        clean.contains("abu dhabi islamic") || clean.contains("adib") || clean.contains("أبوظبي الإسلامي") || clean.contains("ابوظبي الاسلامي") -> {
            BrandConfig("ADIB", Color(0xFF0D9488))
        }
        clean.contains("abu dhabi commercial") || clean.contains("adcb") || clean.contains("أبوظبي التجاري") || clean.contains("ابوظبي التجاري") -> {
            BrandConfig("ADCB", Color(0xFFD32F2F))
        }
        clean.contains("first abu dhabi") || clean.contains("fab") || clean.contains("أبوظبي الأول") || clean.contains("ابوظبي الاول") || clean.contains("عوده") -> {
            BrandConfig("FAB", Color(0xFFC8102E))
        }
        clean.contains("agricole") || clean.contains("كريدي أجريكول") || clean.contains("كريدي اجريكول") -> {
            BrandConfig("CAE", Color(0xFF059669))
        }
        clean.contains("emirates nbd") || clean.contains("الإمارات دبي") || clean.contains("الامارات دبي") -> {
            BrandConfig("ENBD", Color(0xFF1E3A8A))
        }
        clean.contains("housing and development") || clean.contains("hdb") || clean.contains("التعمير والإسكان") || clean.contains("التعمير والاسكان") -> {
            BrandConfig("HDB", Color(0xFF059669))
        }
        clean.contains("eg bank") || clean.contains("egyptian gulf") || clean.contains("المصري الخليجي") -> {
            BrandConfig("EGB", Color(0xFF0A2540))
        }
        clean.contains("saib") || clean.contains("سيب") -> {
            BrandConfig("SAIB", Color(0xFF1E3A8A))
        }
        clean.contains("albaraka") || clean.contains("baraka") || clean.contains("البركة") -> {
            BrandConfig("ABG", Color(0xFF8B5A2B))
        }
        clean.contains("attijariwafa") || clean.contains("التجاري وفا") -> {
            BrandConfig("AWB", Color(0xFFEAB308))
        }
        clean.contains("arab bank") || clean.contains("البنك العربي") -> {
            BrandConfig("ARAB", Color(0xFF006400))
        }
        clean.contains("hsbc") -> {
            BrandConfig("HSBC", Color(0xFFE50914))
        }
        clean.contains("faisal") || clean.contains("فيصل") -> {
            BrandConfig("FIB", Color(0xFF1B4D3E))
        }
        clean.contains("citibank") || clean.contains("سيتي بنك") -> {
            BrandConfig("CITI", Color(0xFF0288D1))
        }
        clean.contains("mashreq") || clean.contains("المشرق") -> {
            BrandConfig("MSH", Color(0xFFFF5722))
        }
        clean.contains("suez canal") || clean.contains("قناة السويس") -> {
            BrandConfig("SCB", Color(0xFF0D47A1))
        }
        clean.contains("united bank") || clean.contains("المتحد") -> {
            BrandConfig("UB", Color(0xFF1565C0))
        }
        clean.contains("ebank") || clean.contains("تنمية الصادرات") -> {
            BrandConfig("EB", Color(0xFF008080))
        }
        clean.contains("nbk") || clean.contains("الكويت الوطني") -> {
            BrandConfig("NBK", Color(0xFF002A54))
        }
        clean.contains("bank abc") || clean.contains("المؤسسة العربية المصرفية") -> {
            BrandConfig("ABC", Color(0xFF008080))
        }
        clean.contains("aibank") || clean.contains("الاستثمار العربي") -> {
            BrandConfig("AIB", Color(0xFF0F172A))
        }
        clean.contains("midbank") || clean.contains("ميد بنك") || clean.contains("ميدبنك") -> {
            BrandConfig("MID", Color(0xFF4A154B))
        }
        clean.contains("arab international bank") || clean.contains("العربي الدولي") -> {
            BrandConfig("AIB", Color(0xFF102A43))
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
            BrandConfig(defaultInitials, defaultColor)
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
        if (!onlineUrl.isNullOrEmpty()) {
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
