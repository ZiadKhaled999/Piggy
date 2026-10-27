package com.oryno.piggy_ledger.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.oryno.piggy_ledger.R

object BiometricHelper {

    fun authenticateToUnhide(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            var activity: FragmentActivity? = null
            var ctx: Context? = context
            while (ctx is ContextWrapper) {
                if (ctx is FragmentActivity) {
                    activity = ctx
                    break
                }
                ctx = ctx.baseContext
            }

            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                onSuccess()
                return
            }

            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                onSuccess()
                return
            }

            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.biometric_unhide_title))
                .setSubtitle(context.getString(R.string.biometric_unhide_subtitle))
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("BiometricHelper", "Biometric authentication failed to initiate", e)
            onSuccess()
        }
    }
}
