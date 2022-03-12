package com.fractel.pushnotifications;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MessagingService extends FirebaseMessagingService {

  private static final String TAG = "PushPluginMsgService";

  // VoIP
  private static final String CHANNEL_VOIP = "Voip";
  private static final String CHANNEL_NAME = "FracTELfone Call";
  private BroadcastReceiver voipNotificationActionBR;
  public static final int VOIP_NOTIFICATION_ID = 168697;
  public static final int oneTimeID = (int) SystemClock.uptimeMillis();

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);
    Log.d(TAG, "onMessageReceived");
    Log.d(TAG, "myFirebaseMessagingService - onMessageReceived - message: " + remoteMessage);
    try {
      Log.d(TAG, "From: " + remoteMessage.getFrom());

      // Check if message contains a data payload.
      if (remoteMessage.getData().size() > 0) {
        Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        showVOIPNotification(remoteMessage);
        // startActivity(intentForLaunchActivity());
      }
    } catch (Exception e) {
      Log.d(TAG, "error onMessageReceived");
    }
  }

  // VoIP implementation
  private Intent intentForLaunchActivity() {
    PackageManager pm = getPackageManager();
    return pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
  }

  private Uri defaultRingtoneUri() {
    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
  }

  private void createNotificationChannel() {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel channel = new NotificationChannel(CHANNEL_VOIP, CHANNEL_NAME, importance);
      channel.setDescription("Channel For VOIP Calls");

      // Set ringtone to notification (>= Android O)
      AudioAttributes audioAttributes = new AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .build();
      channel.setSound(defaultRingtoneUri(), audioAttributes);

      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
      Log.d(TAG, "createNotificationChannel");
    }
  }

  private void showVOIPNotification(RemoteMessage remoteMessage) {
    Map<String, String> messageData = remoteMessage.getData();
    createNotificationChannel();
    Log.d(TAG, "showVOIPNotification");
    // Prepare data from messageData
    String caller = "Unknown caller";
    if (messageData.containsKey("Username")) {
      caller = messageData.get("Username");
    }
    String callId = messageData.get("callId");
    String callbackUrl = messageData.get("callbackUrl");

    // Update Webhook status to CONNECTED
    // updateWebhookVOIPStatus(callbackUrl, callId,
    // IncomingCallActivity.VOIP_CONNECTED);

    // Intent for LockScreen or tapping on notification
    Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
    fullScreenIntent.putExtra("caller", caller);
    PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
        fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    // Intent for tapping on Answer
    Intent acceptIntent = new Intent(IncomingCallActivity.VOIP_ACCEPT);
    PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 10, acceptIntent, 0);

    // Intent for tapping on Reject
    Intent declineIntent = new Intent(IncomingCallActivity.VOIP_DECLINE);
    PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 20, declineIntent, 0);

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_VOIP)
        .setSmallIcon(getResources().getIdentifier("pushicon", "drawable", getPackageName()))
        .setContentTitle("Incoming call")
        .setContentText(caller)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        // Show main activity on lock screen or when tapping on notification
        .setFullScreenIntent(fullScreenPendingIntent, true)
        // Show Accept button
        .addAction(new NotificationCompat.Action(0, "Accept",
            acceptPendingIntent))
        // Show decline action
        .addAction(new NotificationCompat.Action(0, "Decline",
            declinePendingIntent))
        // Make notification dismiss on user input action
        .setAutoCancel(true)
        // Cannot be swiped by user
        .setOngoing(true)
        // Set ringtone to notification (< Android O)
        .setSound(defaultRingtoneUri());

    // automatically cancels after 30 seconds
    // .setTimeoutAfter(30000);

    Notification incomingCallNotification = notificationBuilder.build();

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    // Display notification
    notificationManager.notify(VOIP_NOTIFICATION_ID, incomingCallNotification);
    new TimerTaskExample(20);
    // Add broadcast receiver for notification button actions
    if (voipNotificationActionBR == null) {
      IntentFilter filter = new IntentFilter();
      filter.addAction(IncomingCallActivity.VOIP_ACCEPT);
      filter.addAction(IncomingCallActivity.VOIP_DECLINE);
      filter.addAction(IncomingCallActivity.VOIP_MISSED);
      Log.d(TAG, "Initializing BR");
      Context appContext = this.getApplicationContext();
      String finalCaller = caller;
      voipNotificationActionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          // Remove BR after responding to notification action
          appContext.unregisterReceiver(voipNotificationActionBR);
          voipNotificationActionBR = null;

          // Handle action
          dismissVOIPNotification();

          String voipStatus = intent.getAction();
          // Update Webhook status to CONNECTED
          // updateWebhookVOIPStatus(callbackUrl, callId, voipStatus);

          // Start cordova activity on answer
          if (voipStatus.equals(IncomingCallActivity.VOIP_ACCEPT)) {

            // Storing incomingCall data into SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);

            // Creating an Editor object to edit(write to the file)
            SharedPreferences.Editor myEdit = sharedPreferences.edit();

            // Storing the key and its value as the data fetched from edittext
            myEdit.putBoolean("incomingCall", true);

            // Once the changes have been made,
            // we need to commit to apply those changes made,
            // otherwise, it will throw an error
            myEdit.commit();

            startActivity(intentForLaunchActivity());
            FirebasePushPlugin.onNewRemoteMessage(remoteMessage);
            // FirebasePushPlugin.sendRemoteMessage(remoteMessage);
          }
          if (voipStatus.equals(IncomingCallActivity.VOIP_MISSED)) {
            showMissedCallNotification(finalCaller);
          }
        }
      };
      Log.d(TAG, "registering BR");
      appContext.registerReceiver(voipNotificationActionBR, filter);
    }
  }

  private void dismissVOIPNotification() {
    NotificationManagerCompat.from(this).cancel(VOIP_NOTIFICATION_ID);
    if (IncomingCallActivity.instance != null) {
      IncomingCallActivity.instance.finish();
    }
  }

  private void showMissedCallNotification(String caller) {
    dismissVOIPNotification();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel channel = new NotificationChannel("missedCall", CHANNEL_NAME, importance);
      channel.setDescription("Channel For VOIP Missed Calls");
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
      }

      Intent declineIntent = new Intent(IncomingCallActivity.VOIP_DECLINE);
    PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 20, declineIntent, 0);

      NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "missedCall");
      notificationBuilder.setContentTitle("Missed call")
        .setContentText(caller)
        .setSmallIcon(getResources().getIdentifier("pushicon", "drawable", getPackageName()))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        // Make notification dismiss on user input action
        .setAutoCancel(true)
        // Cannot be swiped by user
        .setOngoing(false)
        // Set ringtone to notification (< Android O)
        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        // Show decline action
//        .addAction(new NotificationCompat.Action(0, "Dismiss",
//            declinePendingIntent));
      Notification missedCallNotification = notificationBuilder.build();

      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
      notificationManager.notify(
        VOIP_NOTIFICATION_ID,
        missedCallNotification);
    }

  public class TimerTaskExample {
    Timer timer; // creating a variable named timer of type Timer

    public TimerTaskExample(int seconds) {
      timer = new Timer(); // creating an instance of the timer class
      timer.schedule(new Reminder(), seconds * 1000);
    }

    class Reminder extends TimerTask {
      public void run() {
      //  showMissedCallNotification();
        System.out.println("Incoming Call stopped, missed call notification..");
        timer.cancel(); // Terminate the timer thread
      }
    }
  }

  @Override
  public void onNewToken(String token) {
    super.onNewToken(token);
    Log.e("Refreshed token:", token);
    FirebasePushPlugin.onNewToken(token);
  }
}