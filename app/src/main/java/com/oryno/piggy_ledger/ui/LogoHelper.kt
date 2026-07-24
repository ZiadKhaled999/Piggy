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
    if (providerName == null) return null
    return when (providerName.trim()) {
        "Vodafone Cash" -> R.drawable.img_logo_vodafone_cash_1783701536523
        "Orange Cash" -> R.drawable.img_logo_orange_cash_1783701552279
        "e& Cash" -> R.drawable.img_logo_etisalat_cash_1783701564184
        "WE Pay" -> R.drawable.img_logo_we_pay_1783701575271
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
    
    // Check known drawables first
    val walletDrawable = when {
        name.equals("Vodafone Cash", ignoreCase = true) -> R.drawable.img_logo_vodafone_cash_1783701536523
        name.equals("Orange Cash", ignoreCase = true) -> R.drawable.img_logo_orange_cash_1783701552279
        name.equals("e& Cash", ignoreCase = true) || name.equals("Etisalat Cash", ignoreCase = true) -> R.drawable.img_logo_etisalat_cash_1783701564184
        name.equals("WE Pay", ignoreCase = true) -> R.drawable.img_logo_we_pay_1783701575271
        else -> null
    }
    
    if (walletDrawable != null) {
        return BrandConfig(
            initials = name.take(2).uppercase(),
            brandColor = when {
                name.contains("Vodafone") -> Color(0xFFE60000)
                name.contains("Orange") -> Color(0xFFFF7900)
                name.contains("e&") || name.contains("Etisalat") -> Color(0xFF749B3E)
                name.contains("WE") -> Color(0xFF4A154B)
                else -> Color(0xFF0F172A)
            },
            drawableRes = walletDrawable
        )
    }

    // Match popular Egyptian bank names
    return when {
        name.contains("National Bank", ignoreCase = true) || name.contains("NBE", ignoreCase = true) || name.contains("الأهلي", ignoreCase = true) -> {
            BrandConfig("NBE", Color(0xFF005B41), R.drawable.logo_nbe_1783715909255)
        }
        name.contains("Banque Misr", ignoreCase = true) || name.contains("Misr", ignoreCase = true) || name.contains("مصر", ignoreCase = true) -> {
            BrandConfig("BM", Color(0xFF900C3F), R.drawable.logo_banque_misr_1783715922421)
        }
        name.contains("Commercial International", ignoreCase = true) || name.contains("CIB", ignoreCase = true) || name.contains("التجاري الدولي", ignoreCase = true) -> {
            BrandConfig("CIB", Color(0xFF1E3A8A), R.drawable.logo_cib_1783715931251)
        }
        name.contains("Caire", ignoreCase = true) || name.contains("القاهرة", ignoreCase = true) -> {
            BrandConfig("BDC", Color(0xFFE05B00), R.drawable.logo_bdc_1783716597873)
        }
        name.contains("Arab African", ignoreCase = true) || name.contains("AAIB", ignoreCase = true) || name.contains("العربي الأفريقي", ignoreCase = true) -> {
            BrandConfig("AAIB", Color(0xFF1E3A8A), R.drawable.logo_aaib_1783716610841)
        }
        name.contains("Abu Dhabi Islamic", ignoreCase = true) || name.contains("ADIB", ignoreCase = true) || name.contains("أبوظبي الإسلامي", ignoreCase = true) -> {
            BrandConfig("ADIB", Color(0xFF0D9488), R.drawable.logo_adib_1783716621576)
        }
        name.contains("Agricole", ignoreCase = true) || name.contains("كريدي أجريكول", ignoreCase = true) -> {
            BrandConfig("CAE", Color(0xFF059669), R.drawable.logo_credit_agricole_1783716632682)
        }
        name.contains("Emirates NBD", ignoreCase = true) || name.contains("الإمارات دبي", ignoreCase = true) -> {
            BrandConfig("ENBD", Color(0xFF1E3A8A), R.drawable.logo_emirates_nbd_1783716643622)
        }
        name.contains("Housing and Development", ignoreCase = true) || name.contains("HDB", ignoreCase = true) || name.contains("التعمير والإسكان", ignoreCase = true) -> {
            BrandConfig("HDB", Color(0xFF059669), R.drawable.logo_hdb_1783716655537)
        }
        name.contains("Gulf", ignoreCase = true) || name.contains("EG Bank", ignoreCase = true) || name.contains("الخليجي", ignoreCase = true) -> {
            BrandConfig("EGB", Color(0xFF0A2540), R.drawable.logo_eg_bank_1783717332920)
        }
        name.contains("SAIB", ignoreCase = true) || name.contains("سيب", ignoreCase = true) -> {
            BrandConfig("SAIB", Color(0xFF1E3A8A), R.drawable.logo_saib_1783717347772)
        }
        name.contains("Baraka", ignoreCase = true) || name.contains("البركة", ignoreCase = true) -> {
            BrandConfig("ABG", Color(0xFF8B5A2B), R.drawable.logo_albaraka_1783717358259)
        }
        name.contains("Attijariwafa", ignoreCase = true) || name.contains("التجاري وفا", ignoreCase = true) -> {
            BrandConfig("AWB", Color(0xFFEAB308), R.drawable.logo_attijariwafa_1783717367255)
        }
        name.contains("Arab Bank", ignoreCase = true) || name.contains("البنك العربي", ignoreCase = true) -> {
            BrandConfig("ARAB", Color(0xFF006400), R.drawable.logo_arab_bank_1783730739805)
        }
        name.contains("ADCB", ignoreCase = true) || name.contains("أبوظبي التجاري", ignoreCase = true) -> {
            BrandConfig("ADCB", Color(0xFFD32F2F), R.drawable.logo_adcb_1783730751082)
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
            BrandConfig("ALX", Color(0xFF4D1A7F), R.drawable.logo_alexbank_1783715942186)
        }
        name.contains("QNB", ignoreCase = true) || name.contains("قطر", ignoreCase = true) -> {
            BrandConfig("QNB", Color(0xFF6F1E51), R.drawable.logo_qnb_1783715951981)
        }
        name.contains("HSBC", ignoreCase = true) -> {
            BrandConfig("HSBC", Color(0xFFE50914), R.drawable.logo_hsbc_1783715962071)
        }
        name.contains("InstaPay", ignoreCase = true) || name.contains("انستاباي", ignoreCase = true) -> {
            BrandConfig("IP", Color(0xFF701A75), R.drawable.logo_instapay_1783715982206)
        }
        name.contains("Faisal", ignoreCase = true) || name.contains("فيصل", ignoreCase = true) -> {
            BrandConfig("FIB", Color(0xFF1B4D3E), R.drawable.logo_faisal_1783715972071)
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
                }
            }
            val defaultColor = when (accountType) {
                AccountType.BANK -> Color(0xFF1E293B)
                AccountType.CARD -> Color(0xFF0F172A)
                AccountType.WALLET -> Color(0xFF0D9488)
                AccountType.CASH -> Color(0xFF059669)
            }
            BrandConfig(defaultInitials, defaultColor)
        }
    }
}

@Composable
fun BrandLogo(
    provider: String?,
    accountType: AccountType,
    iconColorHex: String?,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("BrandLogo", "Rendering logo for provider: $provider, type: $accountType")
    val config = getBrandConfig(provider, accountType)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (config.drawableRes != null) Color.Transparent else {
                    if (iconColorHex != null) {
                        try {
                            Color(android.graphics.Color.parseColor(iconColorHex))
                        } catch (e: Exception) {
                            config.brandColor
                        }
                    } else {
                        config.brandColor
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (config.drawableRes != null) {
            Image(
                painter = painterResource(id = config.drawableRes),
                contentDescription = provider ?: "Logo",
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Render high-fidelity monogram/initials or beautiful type-safe icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(2.dp)
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
    }
}
