package com.example.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object WishDropNotificationManager {
  const val CHANNEL_ID = "wishdrop_invitations"
  private const val CHANNEL_NAME = "WishDrop Meghívások & Értesítések"
  private const val CHANNEL_DESC = "Értesítések közös listameghívásokról és családi frissítésekről"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.createNotificationChannel(channel)
    }
  }

  @android.annotation.SuppressLint("MissingPermission")
  fun postInvitationNotification(
    context: Context,
    inviterName: String,
    listTitle: String,
    role: String,
    isHungarian: Boolean = true,
  ) {
    createNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val title = if (isHungarian) {
      "🎁 Új WishDrop listameghívás!"
    } else {
      "🎁 New WishDrop List Invitation!"
    }

    val message = if (isHungarian) {
      "$inviterName meghívott a(z) \"$listTitle\" közös kívánságlistára ($role)!"
    } else {
      "$inviterName invited you to the \"$listTitle\" shared wishlist ($role)!"
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .build()

    val notificationId = (System.currentTimeMillis() % 100_000).toInt()
    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission not granted or restricted
    }
  }

  fun sendEmailInvitation(
    context: Context,
    recipientEmail: String,
    inviterName: String,
    listTitle: String,
    role: String,
    isHungarian: Boolean = true,
  ): Boolean {
    val subject = if (isHungarian) {
      "Meghívó a(z) \"$listTitle\" WishDrop kívánságlistára"
    } else {
      "Invitation to \"$listTitle\" WishDrop wishlist"
    }

    val body = if (isHungarian) {
      """
        Szia!
        
        $inviterName meghívott a(z) "$listTitle" közös kívánságlistára a WishDrop alkalmazásban ($role jogosultsággal)!
        
        A listához való csatlakozáshoz és az ajándékok böngészéséhez töltsd le az appot az alábbi linkről:
        👉 https://github.com/aistudio/wishdrop/releases
        
        A regisztráció után a lista automatikusan megjelenik a profilodban.
        
        Szeretettel,
        A WishDrop csapata
      """.trimIndent()
    } else {
      """
        Hi!
        
        $inviterName has invited you to collaborate on the "$listTitle" shared wishlist on WishDrop (role: $role)!
        
        To join the list and coordinate gifts, download the WishDrop app here:
        👉 https://github.com/aistudio/wishdrop/releases
        
        After signing up with this email, the shared wishlist will automatically appear in your profile.
        
        Best regards,
        The WishDrop Team
      """.trimIndent()
    }

    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:${Uri.encode(recipientEmail.trim())}")
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, body)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(Intent.createChooser(emailIntent, if (isHungarian) "Meghívó küldése e-mailben" else "Send invitation email"))
      true
    } catch (_: Exception) {
      // Fallback to generic ACTION_SEND if mailto handler is not configured
      try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = "message/rfc822"
          putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
          putExtra(Intent.EXTRA_SUBJECT, subject)
          putExtra(Intent.EXTRA_TEXT, body)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, if (isHungarian) "Meghívó küldése" else "Send invitation"))
        true
      } catch (_: Exception) {
        false
      }
    }
  }
}
