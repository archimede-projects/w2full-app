package com.archimede.w2full.data.mimit

import android.util.Log

fun interface MimitLogger {
    fun error(message: String, throwable: Throwable)
}

class LogcatMimitLogger : MimitLogger {
    override fun error(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    companion object {
        const val TAG = "W2Full-MIMIT"
    }
}
