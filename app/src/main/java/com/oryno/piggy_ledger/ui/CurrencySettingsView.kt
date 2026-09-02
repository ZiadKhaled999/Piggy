package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.*

@Composable
fun CurrencySettingsView(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit = {}
) {
    val appCurrency by viewModel.appCurrency.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            availableCurrencies
        } else {
            val query = searchQuery.trim()
            availableCurrencies.filter {
                it.code.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.symbol.contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_ellipsis), color = TextLight) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextLight)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PinkPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC)
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredCurrencies, key = { it.code }) { currency ->
                val isSelected = appCurrency.equals(currency.code, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setAppCurrency(currency.code)
                        },
                    color = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currency.flag,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            Column {
                                Text(
                                    text = currency.name,
                                    color = NavyDark,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${currency.code} • ${currency.symbol}",
                                    color = TextLight,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = PinkPrimary)
                        } else {
                            Text(
                                text = currency.symbol,
                                color = TextLight,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
