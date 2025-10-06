package com.peekguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Notification Receiver for PeekGuard
 * Handles notification actions and dismissals
 */
class NotificationReceiver : BroadcastReceiver() {
    
    private val tag = "NotificationReceiver"
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            ACTION_DISMISS_ALERT -> {
                Log.d(tag, "Alert dismissed by user")
                // Handle alert dismissal
            }
            ACTION_OPEN_APP -> {
                Log.d(tag, "Opening app from notification")
                // Open main activity
                val mainIntent = Intent(context, Class.forName("com.peekguard.app.MainActivity")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(mainIntent)
            }
        }
    }
    
    companion object {
        const val ACTION_DISMISS_ALERT = "com.peekguard.app.ACTION_DISMISS_ALERT"
        const val ACTION_OPEN_APP = "com.peekguard.app.ACTION_OPEN_APP"
    }
}