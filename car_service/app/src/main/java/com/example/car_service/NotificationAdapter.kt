package com.example.car_service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<FirebaseHelper.AppNotification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notifIcon: TextView = itemView.findViewById(R.id.notifIcon)
        val notifTitle: TextView = itemView.findViewById(R.id.notifTitle)
        val notifMessage: TextView = itemView.findViewById(R.id.notifMessage)
        val notifTime: TextView = itemView.findViewById(R.id.notifTime)
        val notifAudience: TextView = itemView.findViewById(R.id.notifAudience)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notif = notifications[position]

        // Pick icon based on title keyword
        val icon = when {
            notif.title.contains("offer", ignoreCase = true) ||
                    notif.title.contains("sale", ignoreCase = true) ||
                    notif.title.contains("special", ignoreCase = true) -> "🎉"
            notif.title.contains("reminder", ignoreCase = true) -> "🔔"
            notif.title.contains("welcome", ignoreCase = true) -> "⭐"
            notif.title.contains("update", ignoreCase = true) ||
                    notif.title.contains("new", ignoreCase = true) -> "📱"
            else -> "🔔"
        }

        holder.notifIcon.text = icon
        holder.notifTitle.text = notif.title
        holder.notifMessage.text = notif.message
        holder.notifTime.text = formatTime(notif.sentAt)

        // Show audience badge
        holder.notifAudience.text = when (notif.audience) {
            "pro" -> "PRO MEMBERS ONLY"
            "free" -> "FREE USERS"
            else -> "ALL USERS"
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifs: List<FirebaseHelper.AppNotification>) {
        notifications = newNotifs
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}