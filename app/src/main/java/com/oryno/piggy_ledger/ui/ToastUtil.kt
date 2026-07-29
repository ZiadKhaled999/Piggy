package com.oryno.piggy_ledger.ui

import android.content.Context
import android.widget.Toast

object ToastUtil {
    private var currentToast: Toast? = null

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        currentToast?.cancel()
        currentToast = Toast.makeText(context, message, duration)
        currentToast?.show()
    }
}
