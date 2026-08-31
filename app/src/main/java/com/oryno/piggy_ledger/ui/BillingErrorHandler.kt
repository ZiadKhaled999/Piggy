package com.oryno.piggy_ledger.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.oryno.piggy_ledger.R
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

object BillingErrorHandler {

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    fun formatPurchasesError(context: Context, error: PurchasesError?): String {
        if (error == null) return context.getString(R.string.billing_error_general)

        val rawMsg = error.message.lowercase()
        val isNetworkIssue = !isOnline(context) ||
            error.code == PurchasesErrorCode.NetworkError ||
            rawMsg.contains("error performing request") ||
            rawMsg.contains("network") ||
            rawMsg.contains("offline") ||
            rawMsg.contains("unable to resolve host") ||
            rawMsg.contains("no address associated") ||
            rawMsg.contains("timeout") ||
            rawMsg.contains("failed to connect") ||
            rawMsg.contains("connection refused") ||
            rawMsg.contains("socket") ||
            rawMsg.contains("internet")

        if (isNetworkIssue) {
            return context.getString(R.string.billing_error_offline)
        }

        return when (error.code) {
            PurchasesErrorCode.PurchaseCancelledError -> ""
            PurchasesErrorCode.StoreProblemError -> "Google Play is temporarily unavailable. Please try again later."
            PurchasesErrorCode.ProductNotAvailableForPurchaseError -> "This subscription plan is currently unavailable."
            PurchasesErrorCode.PurchaseNotAllowedError -> "In-app purchases are not allowed on this device."
            PurchasesErrorCode.PurchaseInvalidError -> "Invalid purchase details. Please try again."
            PurchasesErrorCode.PaymentPendingError -> "Payment is currently pending. Please check Google Play."
            else -> {
                if (error.message.isNotBlank()) {
                    error.message
                } else {
                    context.getString(R.string.billing_error_general)
                }
            }
        }
    }

    fun formatThrowable(context: Context, throwable: Throwable?): String {
        if (throwable == null) return context.getString(R.string.billing_error_general)
        
        val rawMsg = throwable.message?.lowercase() ?: ""
        val isNetworkIssue = !isOnline(context) ||
            rawMsg.contains("error performing request") ||
            rawMsg.contains("network") ||
            rawMsg.contains("offline") ||
            rawMsg.contains("unable to resolve host") ||
            rawMsg.contains("no address associated") ||
            rawMsg.contains("timeout") ||
            rawMsg.contains("failed to connect") ||
            rawMsg.contains("socket") ||
            throwable is java.net.UnknownHostException ||
            throwable is java.net.SocketTimeoutException ||
            throwable is java.io.IOException

        if (isNetworkIssue) {
            return context.getString(R.string.billing_error_offline)
        }

        return throwable.message ?: context.getString(R.string.billing_error_general)
    }
}
