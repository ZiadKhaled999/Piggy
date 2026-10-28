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
    
    // Check known wallet names
    if (name.contains("Vodafone", ignoreCase = true)) {
        return BrandConfig("VF", Color(0xFFE60000))
    } else if (name.contains("Orange", ignoreCase = true)) {
        return BrandConfig("OR", Color(0xFFFF7900))
    } else if (name.contains("e&", ignoreCase = true) || name.contains("Etisalat", ignoreCase = true)) {
        return BrandConfig("e&", Color(0xFF749B3E))
    } else if (name.contains("WE", ignoreCase = true)) {
        return BrandConfig("WE", Color(0xFF4A154B))
    } else if (name.contains("InstaPay", ignoreCase = true) || name.contains("انستاباي", ignoreCase = true)) {
        return BrandConfig("IP", Color(0xFF701A75))
    } else if (name.contains("Telda", ignoreCase = true)) {
        return BrandConfig("TD", Color(0xFF0F172A))
    }

    // Match popular Egyptian bank names with brand colors and clean initials
    return when {
        name.contains("National Bank", ignoreCase = true) || name.contains("NBE", ignoreCase = true) || name.contains("الأهلي", ignoreCase = true) -> {
            BrandConfig("NBE", Color(0xFF005B41))
        }
        name.contains("Banque Misr", ignoreCase = true) || name.contains("Misr", ignoreCase = true) || name.contains("مصر", ignoreCase = true) -> {
            BrandConfig("BM", Color(0xFF900C3F))
        }
        name.contains("Commercial International", ignoreCase = true) || name.contains("CIB", ignoreCase = true) || name.contains("التجاري الدولي", ignoreCase = true) -> {
            BrandConfig("CIB", Color(0xFF1E3A8A))
        }
        name.contains("Caire", ignoreCase = true) || name.contains("القاهرة", ignoreCase = true) -> {
            BrandConfig("BDC", Color(0xFFE05B00))
        }
        name.contains("Arab African", ignoreCase = true) || name.contains("AAIB", ignoreCase = true) || name.contains("العربي الأفريقي", ignoreCase = true) -> {
            BrandConfig("AAIB", Color(0xFF1E3A8A))
        }
        name.contains("Abu Dhabi Islamic", ignoreCase = true) || name.contains("ADIB", ignoreCase = true) || name.contains("أبوظبي الإسلامي", ignoreCase = true) -> {
            BrandConfig("ADIB", Color(0xFF0D9488))
        }
        name.contains("Agricole", ignoreCase = true) || name.contains("كريدي أجريكول", ignoreCase = true) -> {
            BrandConfig("CAE", Color(0xFF059669))
        }
        name.contains("Emirates NBD", ignoreCase = true) || name.contains("الإمارات دبي", ignoreCase = true) -> {
            BrandConfig("ENBD", Color(0xFF1E3A8A))
        }
        name.contains("Housing and Development", ignoreCase = true) || name.contains("HDB", ignoreCase = true) || name.contains("التعمير والإسكان", ignoreCase = true) -> {
            BrandConfig("HDB", Color(0xFF059669))
        }
        name.contains("Gulf", ignoreCase = true) || name.contains("EG Bank", ignoreCase = true) || name.contains("الخليجي", ignoreCase = true) -> {
            BrandConfig("EGB", Color(0xFF0A2540))
        }
        name.contains("SAIB", ignoreCase = true) || name.contains("سيب", ignoreCase = true) -> {
            BrandConfig("SAIB", Color(0xFF1E3A8A))
        }
        name.contains("Baraka", ignoreCase = true) || name.contains("البركة", ignoreCase = true) -> {
            BrandConfig("ABG", Color(0xFF8B5A2B))
        }
        name.contains("Attijariwafa", ignoreCase = true) || name.contains("التجاري وفا", ignoreCase = true) -> {
            BrandConfig("AWB", Color(0xFFEAB308))
        }
        name.contains("Arab Bank", ignoreCase = true) || name.contains("البنك العربي", ignoreCase = true) -> {
            BrandConfig("ARAB", Color(0xFF006400))
        }
        name.contains("ADCB", ignoreCase = true) || name.contains("أبوظبي التجاري", ignoreCase = true) -> {
            BrandConfig("ADCB", Color(0xFFD32F2F))
        }
        name.contains("EBank", ignoreCase = true) || name.contains("تنمية الصادرات", ignoreCase = true) -> {
            BrandConfig("EB", Color(0xFF008080), R.drawable.logo_ebank_vector)
        }
        name.contains("United Bank", ignoreCase = true) || name.contains("المتحد", ignoreCase = true) -> {
            BrandConfig("UB", Color(0xFF1565C0), R.drawable.logo_united_bank_vector)
        }
        name.contains("Suez Canal", ignoreCase = true) || name.contains("قناة السويس", ignoreCase = true) -> {
            BrandConfig("SCB", Color(0xFF0D47A1), R.drawable.logo_suez_canal_vector)
        }
        name.contains("Mashreq", ignoreCase = true) || name.contains("المشرق", ignoreCase = true) -> {
            BrandConfig("MSH", Color(0xFFFF5722), R.drawable.logo_mashreq_vector)
        }
        name.contains("Citibank", ignoreCase = true) || name.contains("سيتي", ignoreCase = true) -> {
            BrandConfig("CITI", Color(0xFF0288D1), R.drawable.logo_citibank_vector)
        }
        name.contains("Alex", ignoreCase = true) || name.contains("الإسكندرية", ignoreCase = true) -> {
            BrandConfig("ALX", Color(0xFF4D1A7F))
        }
        name.contains("QNB", ignoreCase = true) || name.contains("قطر", ignoreCase = true) -> {
            BrandConfig("QNB", Color(0xFF6F1E51))
        }
        name.contains("HSBC", ignoreCase = true) -> {
            BrandConfig("HSBC", Color(0xFFE50914))
        }
        name.contains("Faisal", ignoreCase = true) || name.contains("فيصل", ignoreCase = true) -> {
            BrandConfig("FIB", Color(0xFF1B4D3E))
        }
        else -> {
            // Generics based on account type
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
    val domain = when {
        name.contains("Vodafone", ignoreCase = true) -> "vodafone.com"
        name.contains("Orange", ignoreCase = true) -> "orange.com"
        name.contains("Etisalat", ignoreCase = true) || name.contains("e&", ignoreCase = true) -> "eand.com"
        name.contains("WE Pay", ignoreCase = true) || name.contains("Telecom Egypt", ignoreCase = true) || name.contains("WE", ignoreCase = true) -> "te.eg"
        name.contains("InstaPay", ignoreCase = true) || name.contains("انستاباي", ignoreCase = true) -> "instapay.eg"
        name.contains("Telda", ignoreCase = true) -> "telda.app"
        name.contains("National Bank", ignoreCase = true) || name.contains("NBE", ignoreCase = true) || name.contains("الأهلي", ignoreCase = true) -> "nbe.com.eg"
        name.contains("Banque Misr", ignoreCase = true) || name.contains("Misr", ignoreCase = true) || name.contains("مصر", ignoreCase = true) -> "banquemisr.com"
        name.contains("Commercial International", ignoreCase = true) || name.contains("CIB", ignoreCase = true) || name.contains("التجاري الدولي", ignoreCase = true) -> "cibeg.com"
        name.contains("Caire", ignoreCase = true) || name.contains("القاهرة", ignoreCase = true) -> "bdc.com.eg"
        name.contains("Arab African", ignoreCase = true) || name.contains("AAIB", ignoreCase = true) || name.contains("العربي الأفريقي", ignoreCase = true) -> "aaib.com"
        name.contains("Abu Dhabi Islamic", ignoreCase = true) || name.contains("ADIB", ignoreCase = true) || name.contains("أبوظبي الإسلامي", ignoreCase = true) -> "adib.eg"
        name.contains("Agricole", ignoreCase = true) || name.contains("كريدي أجريكول", ignoreCase = true) -> "credit-agricole.com.eg"
        name.contains("Emirates NBD", ignoreCase = true) || name.contains("الإمارات دبي", ignoreCase = true) -> "emiratesnbd.com.eg"
        name.contains("Housing and Development", ignoreCase = true) || name.contains("HDB", ignoreCase = true) || name.contains("التعمير والإسكان", ignoreCase = true) -> "hdb-egypt.com"
        name.contains("Gulf", ignoreCase = true) || name.contains("EG Bank", ignoreCase = true) || name.contains("الخليجي", ignoreCase = true) -> "eg-bank.com"
        name.contains("SAIB", ignoreCase = true) || name.contains("سيب", ignoreCase = true) -> "saib.com.eg"
        name.contains("Baraka", ignoreCase = true) || name.contains("البركة", ignoreCase = true) -> "albaraka.com.eg"
        name.contains("Attijariwafa", ignoreCase = true) || name.contains("التجاري وفا", ignoreCase = true) -> "attijariwafabank.com.eg"
        name.contains("Arab Bank", ignoreCase = true) || name.contains("البنك العربي", ignoreCase = true) -> "arabbank.com.eg"
        name.contains("ADCB", ignoreCase = true) || name.contains("أبوظبي التجاري", ignoreCase = true) -> "adcb.com.eg"
        name.contains("Alex", ignoreCase = true) || name.contains("الإسكندرية", ignoreCase = true) -> "alexbank.com"
        name.contains("QNB", ignoreCase = true) || name.contains("قطر", ignoreCase = true) -> "qnbalahli.com"
        name.contains("HSBC", ignoreCase = true) -> "hsbc.com.eg"
        name.contains("Faisal", ignoreCase = true) || name.contains("فيصل", ignoreCase = true) -> "faisalbank.com.eg"
        name.contains("Citibank", ignoreCase = true) || name.contains("سيتي", ignoreCase = true) -> "citibank.com"
        name.contains("Mashreq", ignoreCase = true) || name.contains("المشرق", ignoreCase = true) -> "mashreqbank.com"
        name.contains("Suez Canal", ignoreCase = true) || name.contains("قناة السويس", ignoreCase = true) -> "scbank.com.eg"
        name.contains("United Bank", ignoreCase = true) || name.contains("المتحد", ignoreCase = true) -> "theubeg.com"
        name.contains("EBank", ignoreCase = true) || name.contains("تنمية الصادرات", ignoreCase = true) -> "ebank.com.eg"
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
