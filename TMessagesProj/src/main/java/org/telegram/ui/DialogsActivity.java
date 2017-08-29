/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationCompat.AnimatorListenerAdapterProxy;
import org.telegram.messenger.AnimationCompat.ObjectAnimatorProxy;
import org.telegram.messenger.AnimationCompat.ViewProxy;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.query.SearchQuery;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.Adapters.DialogsSearchAdapter;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.Favourite;
import org.telegram.ui.Components.Glow;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PlayerView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.URLSpanUserMention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Utility.Config;
import Utility.Refresh_All_Tabs;
import Utility.User;
import ir.radius.iphogram.BuildConfig;
import ir.radius.iphogram.R;
import telegramplus.HideChats.hideDBAdapter;
import telegramplus.HideChats.hideObjc;
import telegramplus.TabView.MyContactsActivity;
import telegramplus.TabView.MySettingActivity;
import telegramplus.category.catDBAdapter;
import telegramplus.category.category;
import telegramplus.category.categoryDBAdapter;
import telegramplus.category.categoryManagement;
import telegramplus.category.chatobject;
import telegramplus.constant;
import telegramplus.users.database.userDBAdapter;


public class DialogsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider, Refresh_All_Tabs {

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private DialogsAdapter dialogsAdapter;
    private DialogsSearchAdapter dialogsSearchAdapter;
    private EmptyTextProgressView searchEmptyView;
    private ProgressBar progressView;
    private LinearLayout emptyView;
    private ActionBarMenuItem passcodeItem;
    private ImageView floatingButton;

    private AlertDialog permissionDialog;

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;
    private int dialogsType;

    public static boolean dialogsLoaded;
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private long selectedDialog;
    private String searchString;
    private long openedDialogId;

    public static boolean needRefreshCategory = false;

    hideDBAdapter hideDBAdapter;

    private DialogsActivityDelegate delegate;

    private float touchPositionDP;
    private int user_id = 0;
    private int chat_id = 0;
    private BackupImageView avatarImage;

    private Button toastBtn;

    private FrameLayout tabsView;
    private LinearLayout tabsLayout;
    private int tabsHeight;
    private ImageView allTab;
    private ImageView usersTab;
    private ImageView groupsTab;
    private ImageView superGroupsTab;
    private ImageView channelsTab;
    private ImageView botsTab;
    private ImageView favsTab;
    private TextView allCounter;
    private TextView usersCounter;
    private TextView groupsCounter;
    private TextView sGroupsCounter;
    private TextView botsCounter;
    private TextView channelsCounter;
    private TextView favsCounter;

    private ImageView settingTab;
    private TextView settingCounter;
    private MySettingActivity settingFragment = new MySettingActivity();
    Boolean settingIsShowing;
    private TextView allT;
    private TextView userT;
    private TextView groupT;
    private TextView sGroupT;
    private TextView channelT;
    private TextView botT;
    private TextView favT;
    private TextView settingT;
    RelativeLayout layout;

    private ImageView contactTab;
    private TextView contactCounter;
    private MyContactsActivity contactFragment = new MyContactsActivity(null);;
    Boolean contactIsShowing;
    private TextView contactT;
    RelativeLayout contactView;
    RelativeLayout settingView;


    FrameLayout frameLayout;


    private boolean countSize;


    private static ActionBarMenuItem headerItem;
    private static ActionBarMenuItem HideItem;
//    private static ActionBarMenuItem catItem;
    private ActionBarMenuItem searchItem;
    private ActionBarMenuItem newMessage;

    private boolean hideTabs;
    private int selectedTab;
    private DialogsAdapter dialogsBackupAdapter;
    private boolean tabsHidden;
    private boolean disableAnimation;

    Context context;

    ActionBarMenuItem Hots;
    ActionBarMenu menu;

    private DialogsOnTouch onTouchListener = null;


    //private DisplayMetrics displayMetrics;

    public interface DialogsActivityDelegate {
        void didSelectDialog(DialogsActivity fragment, long dialog_id, boolean param);
    }

    public DialogsActivity(Bundle args) {
        super(args);
    }

    @Override
    public void setParentLayout(ActionBarLayout layout) {
        super.setParentLayout(layout);
        settingFragment.setParentLayout(layout);
        contactFragment.setParentLayout(layout);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        settingFragment.onFragmentCreate();
        contactFragment.onFragmentCreate();

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            dialogsType = arguments.getInt("dialogsType", 0);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
        }

        if (searchString == null) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByAck);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByServer);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didLoadedReplyMessages);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.refreshTabs);
        }


        if (!dialogsLoaded) {
            MessagesController.getInstance().loadDialogs(0, 100, true);
            ContactsController.getInstance().checkInviteText();
            dialogsLoaded = true;
        }

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        settingFragment.onFragmentDestroy();
        contactFragment.onFragmentDestroy();
        if (searchString == null) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByAck);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByServer);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didLoadedReplyMessages);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.refreshTabs);
        }
        delegate = null;
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View createView(final Context context) {
        this.context = context;
        LocalBroadcastManager.getInstance(ApplicationLoader.applicationContext).registerReceiver(receiver, new IntentFilter("MyData"));

        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = plusPreferences.edit();
        editor.putBoolean("hideUsers", true).apply();
        editor.putBoolean("hideGroups", true).apply();
        editor.putBoolean("hideSGroups", true).apply();
        editor.putBoolean("hideChannels", true).apply();
        editor.putBoolean("hideBots", true).apply();

        searching = false;
        searchWas = false;
        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
        Boolean first = sharedPreferences.getBoolean("firstime",true);
        if (UserConfig.isClientActivated() && UserConfig.getCurrentUser() != null && UserConfig.getCurrentUser().phone != null && first) {
//            String name = UserConfig.getCurrentUser().first_name + "?stringSeparators?" + UserConfig.getCurrentUser().last_name + "?stringSeparators?" + UserConfig.getCurrentUser().username;
//            new SendUser(UserConfig.getCurrentUser().phone, UserConfig.getCurrentUser().id, name, context).execute();
            sendUser();
        }
        Theme.loadResources(context);

        hideDBAdapter = new hideDBAdapter(context);

        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int iconColor = themePrefs.getInt("chatsHeaderIconsColor", 0xff039BE5);
        int tColor = themePrefs.getInt("chatsHeaderTitleColor", 0xff039BE5);
        avatarImage = new BackupImageView(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(30));
        menu = actionBar.createMenu();
        if (!onlySelect && searchString == null) {
            Drawable lock = getParentActivity().getResources().getDrawable(R.drawable.lock_close);
            lock.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
            passcodeItem = menu.addItem(1, lock);
            updatePasscodeButton();
        }


        Drawable newMessage = getParentActivity().getResources().getDrawable(R.drawable.new_message_head);
        newMessage.setColorFilter(0xff0288d1, PorterDuff.Mode.SRC_IN);
        this.newMessage = menu.addItem(1, newMessage);
        this.newMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("destroyAfterSelect", true);
                presentFragment(new ContactsActivity(args));
            }
        });

//        Drawable catIcon = getParentActivity().getResources().getDrawable(R.drawable.menu_category);
//        catIcon.setColorFilter(0xff0288d1, PorterDuff.Mode.SRC_IN);
//        catItem = menu.addItem(4, catIcon);


//        catItem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showCats(context);
//            }
//        });


/*
        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
        boolean ghost_mpode = sharedPreferences.getBoolean("ghost_mode", false);

        Drawable ghosticon = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost_disabled);
        ghosticon.setColorFilter(0xff0288d1, PorterDuff.Mode.MULTIPLY);

        if (ghost_mpode) {
            ghosticon = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost);
            ghosticon.setColorFilter(0xff0288d1, PorterDuff.Mode.MULTIPLY);
            MessagesController.getInstance().reRunUpdateTimerProc();
        }


        headerItem = menu.addItem(2, ghosticon);

        headerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeGhostModeState();
            }
        });


        Drawable hideIcom = getParentActivity().getResources().getDrawable(R.drawable.ic_menu_hide);
        hideIcom.setColorFilter(0xff0288d1, PorterDuff.Mode.SRC_IN);

        HideItem = menu.addItem(3, hideIcom);

        HideItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                CharSequence[] cs = new CharSequence[]{context.getResources().getString(R.string.unHideACode),
                  context.getResources().getString(R.string.HideACode),
                  context.getResources().getString(R.string.HiddenHelp)};
                builder.setItems(cs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (which == 0) {
                            ShowOrHideAcode(0);
                        } else if (which == 1) {
                            ShowOrHideAcode(1);
                        } else {

                            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                            Drawable icon = getParentActivity().getResources().getDrawable(R.drawable.ic_menu_hide);
                            icon.setColorFilter(0xff0288d1, PorterDuff.Mode.SRC_IN);
                            builder1.setIcon(icon);
                            builder1.setTitle(LocaleController.getString("HiddenChats", R.string.HiddenChats));
                            builder1.setMessage(context.getResources().getString(R.string.HiddenHelpMessage));
                            builder1.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });

                            builder1.show();

                        }

                    }
                });
                builder.show();

            }
        });
*/
          refreshToolbarItems();

        //final ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
        Drawable search = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_search);
        searchItem = menu.addItem(0, search);
        searchItem.setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {

//                headerItem.setVisibility(View.GONE);
//                HideItem.setVisibility(View.GONE);
//                catItem.setVisibility(View.GONE);
                //Telegram
                refreshTabAndListViews(true);
                //
                searching = true;
                if (listView != null) {
                    if (searchString != null) {
                        listView.setEmptyView(searchEmptyView);
                        progressView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                    }
                    if (!onlySelect) {
                        floatingButton.setVisibility(View.GONE);
                    }
                }
                updatePasscodeButton();
            }

            @Override
            public boolean canCollapseSearch() {
                if (searchString != null) {
                    finishFragment();
                    return false;
                }
                return true;
            }

            @Override
            public void onSearchCollapse() {
                refreshTabAndListViews(false);
                //
                refreshToolbarItems();
                searching = false;
                searchWas = false;
                if (listView != null) {
                    searchEmptyView.setVisibility(View.GONE);
                    if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    } else {
                        progressView.setVisibility(View.GONE);
                        listView.setEmptyView(emptyView);
                    }
                    if (!onlySelect) {
                        floatingButton.setVisibility(View.VISIBLE);
                        floatingHidden = true;
                        ViewProxy.setTranslationY(floatingButton, AndroidUtilities.dp(100));
                        hideFloatingButton(false);
                    }
                    if (listView.getAdapter() != dialogsAdapter) {
                        listView.setAdapter(dialogsAdapter);
                        dialogsAdapter.notifyDataSetChanged();
                    }
                }
                if (dialogsSearchAdapter != null) {
                    dialogsSearchAdapter.searchDialogs(null);
                }
                updatePasscodeButton();
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                if (text.length() != 0 || dialogsSearchAdapter != null && dialogsSearchAdapter.hasRecentRearch()) {
                    searchWas = true;
                    if (dialogsSearchAdapter != null && listView.getAdapter() != dialogsSearchAdapter) {
                        listView.setAdapter(dialogsSearchAdapter);
                        dialogsSearchAdapter.notifyDataSetChanged();
                    }
                    if (searchEmptyView != null && listView.getEmptyView() != searchEmptyView) {
                        emptyView.setVisibility(View.GONE);
                        progressView.setVisibility(View.GONE);
                        searchEmptyView.showTextView();
                        listView.setEmptyView(searchEmptyView);
                    }
                }
                if (dialogsSearchAdapter != null) {
                    dialogsSearchAdapter.searchDialogs(text);
                }
                updateListBG();
            }
        });
        searchItem.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));


        if (tColor != 0xffffffff) {
            searchItem.getSearchField().setTextColor(tColor);
            searchItem.getSearchField().setHintTextColor(AndroidUtilities.getIntAlphaColor("chatsHeaderTitleColor", 0xffffffff, 0.5f));
        }
        Drawable clear = getParentActivity().getResources().getDrawable(R.drawable.ic_close_white);
        if (clear != null) clear.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
        searchItem.getClearButton().setImageDrawable(clear);
        if (onlySelect) {
            //actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            Drawable back = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_back);
            if (back != null) back.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            actionBar.setBackButtonDrawable(back);
            actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
        } else {
            if (searchString != null) {
                Drawable back = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_back);
                back.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                actionBar.setBackButtonDrawable(back);
            } else {
                actionBar.setBackButtonDrawable(new MenuDrawable());
            }
            if (BuildVars.DEBUG_VERSION) {
                actionBar.setTitle(LocaleController.getString("AppNameBeta", R.string.AppNameBeta));
            } else {
                actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
            }
        }
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (onlySelect) {
                        finishFragment();
                    } else if (parentLayout != null) {
                        //
                        //if (!hideTabs) {
                        //    parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(true, false);
                        //}
                        //
                        parentLayout.getDrawerLayoutContainer().openDrawer(false);
                    }
                } else if (id == 1) {
                    UserConfig.appLocked = !UserConfig.appLocked;
                    UserConfig.saveConfig(false);
                    updatePasscodeButton();
                }
            }
        });

        paintHeader(false);

        frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;


        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(true);
        listView.setItemAnimator(null);
        listView.setInstantClick(true);
        listView.setLayoutAnimation(null);
        layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        if (Build.VERSION.SDK_INT >= 11) {
            listView.setVerticalScrollbarPosition(LocaleController.isRTL ? ListView.SCROLLBAR_POSITION_LEFT : ListView.SCROLLBAR_POSITION_RIGHT);
        }
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        onTouchListener = new DialogsOnTouch(context);
        listView.setOnTouchListener(onTouchListener);

        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if (dialogsType != 11) {
                    long dialog_id = 0;
                    int message_id = 0;
                    RecyclerView.Adapter adapter = listView.getAdapter();
                    if (adapter == dialogsAdapter) {
                        TLRPC.TL_dialog dialog = dialogsAdapter.getItem(position);
                        if (dialog == null) {
                            return;
                        }
                        dialog_id = dialog.id;
                    } else if (adapter == dialogsSearchAdapter) {
                        Object obj = dialogsSearchAdapter.getItem(position);
                        if (obj instanceof TLRPC.User) {
                            dialog_id = ((TLRPC.User) obj).id;
                            if (dialogsSearchAdapter.isGlobalSearch(position)) {
                                ArrayList<TLRPC.User> users = new ArrayList<>();
                                users.add((TLRPC.User) obj);
                                MessagesController.getInstance().putUsers(users, false);
                                MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                            }
                            if (!onlySelect) {
                                dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.User) obj);
                            }
                        } else if (obj instanceof TLRPC.Chat) {
                            if (dialogsSearchAdapter.isGlobalSearch(position)) {
                                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                                chats.add((TLRPC.Chat) obj);
                                MessagesController.getInstance().putChats(chats, false);
                                MessagesStorage.getInstance().putUsersAndChats(null, chats, false, true);
                            }
                            if (((TLRPC.Chat) obj).id > 0) {
                                dialog_id = -((TLRPC.Chat) obj).id;
                            } else {
                                dialog_id = AndroidUtilities.makeBroadcastId(((TLRPC.Chat) obj).id);
                            }
                            if (!onlySelect) {
                                dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.Chat) obj);
                            }
                        } else if (obj instanceof TLRPC.EncryptedChat) {
                            dialog_id = ((long) ((TLRPC.EncryptedChat) obj).id) << 32;
                            if (!onlySelect) {
                                dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.EncryptedChat) obj);
                            }
                        } else if (obj instanceof MessageObject) {
                            MessageObject messageObject = (MessageObject) obj;
                            dialog_id = messageObject.getDialogId();
                            message_id = messageObject.getId();
                            dialogsSearchAdapter.addHashtagsFromMessage(dialogsSearchAdapter.getLastSearchString());
                        } else if (obj instanceof String) {
                            actionBar.openSearchField((String) obj);
                        }
                    }

                    if (dialog_id == 0) {
                        return;
                    }

                    if (touchPositionDP < 65) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        //if(preferences.getInt("dialogsClickOnGroupPic", 0) == 2)MessagesController.getInstance().loadChatInfo(chat_id, null, false);
                        user_id = 0;
                        chat_id = 0;
                        int lower_part = (int) dialog_id;
                        int high_id = (int) (dialog_id >> 32);

                        if (lower_part != 0) {
                            if (high_id == 1) {
                                chat_id = lower_part;
                            } else {
                                if (lower_part > 0) {
                                    user_id = lower_part;
                                } else if (lower_part < 0) {
                                    chat_id = -lower_part;
                                }
                            }
                        } else {
                            TLRPC.EncryptedChat chat = MessagesController.getInstance().getEncryptedChat(high_id);
                            user_id = chat.user_id;
                        }

                        if (user_id != 0) {
                            int picClick = plusPreferences.getInt("dialogsClickOnPic", 1);
                            if (picClick == 2) {
                                Bundle args = new Bundle();
                                args.putInt("user_id", user_id);
                                presentFragment(new ProfileActivity(args));
                                return;
                            } else if (picClick == 1) {
                                TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                                if (user.photo != null && user.photo.photo_big != null) {
                                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                                    PhotoViewer.getInstance().openPhoto(user.photo.photo_big, DialogsActivity.this);
                                }
                                return;
                            }

                        } else if (chat_id != 0) {
                            int picClick = plusPreferences.getInt("dialogsClickOnGroupPic", 2);
                            if (picClick == 2) {
                                MessagesController.getInstance().loadChatInfo(chat_id, null, false);
                                Bundle args = new Bundle();
                                args.putInt("chat_id", chat_id);
                                ProfileActivity fragment = new ProfileActivity(args);
                                presentFragment(fragment);
                                return;
                            } else if (picClick == 1) {
                                TLRPC.Chat chat = MessagesController.getInstance().getChat(chat_id);
                                if (chat.photo != null && chat.photo.photo_big != null) {
                                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                                    PhotoViewer.getInstance().openPhoto(chat.photo.photo_big, DialogsActivity.this);
                                }
                                return;
                            }
                        }
                    }

                    //
                    if (onlySelect) {
                        didSelectResult(dialog_id, true, false);
                    } else {
                        Bundle args = new Bundle();
                        int lower_part = (int) dialog_id;
                        int high_id = (int) (dialog_id >> 32);
                        if (lower_part != 0) {
                            if (high_id == 1) {
                                args.putInt("chat_id", lower_part);
                            } else {
                                if (lower_part > 0) {
                                    args.putInt("user_id", lower_part);
                                } else if (lower_part < 0) {
                                    if (message_id != 0) {
                                        TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                                        if (chat != null && chat.migrated_to != null) {
                                            args.putInt("migrated_to", lower_part);
                                            lower_part = -chat.migrated_to.channel_id;
                                        }
                                    }
                                    args.putInt("chat_id", -lower_part);
                                }
                            }
                        } else {
                            args.putInt("enc_id", high_id);
                        }
                        if (message_id != 0) {
                            args.putInt("message_id", message_id);
                        } else {
                            if (actionBar != null) {
                                actionBar.closeSearchField();
                            }
                        }
                        if (AndroidUtilities.isTablet()) {
                            if (openedDialogId == dialog_id && adapter != dialogsSearchAdapter) {
                                return;
                            }
                            if (dialogsAdapter != null) {
                                dialogsAdapter.setOpenedDialogId(openedDialogId = dialog_id);
                                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                            }
                        }
                        if (searchString != null) {
                            if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                                presentFragment(new ChatActivity(args));
                            }
                        } else {
                            if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                                presentFragment(new ChatActivity(args));
                            }
                        }
                    }
                }
            }
        });
        listView.setOnItemLongClickListener(
          new RecyclerListView.OnItemLongClickListener() {
              @Override
              public boolean onItemClick(View view, int position) {

                  // Clear search history
                  if (onlySelect || searching && searchWas || getParentActivity() == null) {
                      if (searchWas && searching || dialogsSearchAdapter.isRecentSearchDisplayed()) {
                          RecyclerView.Adapter adapter = listView.getAdapter();
                          if (adapter == dialogsSearchAdapter) {
                              Object item = dialogsSearchAdapter.getItem(position);
                              if (item instanceof String || dialogsSearchAdapter.isRecentSearchDisplayed()) {
                                  AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                  builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                  builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                                  builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i) {
                                          if (dialogsSearchAdapter.isRecentSearchDisplayed()) {
                                              dialogsSearchAdapter.clearRecentSearch();
                                          } else {
                                              dialogsSearchAdapter.clearRecentHashtags();
                                          }
                                      }
                                  });
                                  builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                  showDialog(builder.create());
                                  return true;
                              }
                          }
                      }
                      return false;
                  }

                  TLRPC.TL_dialog dialog;
                  ArrayList<TLRPC.TL_dialog> dialogs = getDialogsArray();
                  if (position < 0 || position >= dialogs.size()) {
                      return false;
                  }

                  dialog = dialogs.get(position);
                  selectedDialog = dialog.id;

                  AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                  int lower_id = (int) selectedDialog;
                  int high_id = (int) (selectedDialog >> 32);

                  if (dialog instanceof TLRPC.TL_dialogChannel) {
                      final TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_id);
                      CharSequence items[];
                      final boolean isFav = Favourite.isFavourite(dialog.id);
                      CharSequence cs2 = isFav ? LocaleController.getString("DeleteFromFavorites", R.string.DeleteFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites);
                      int muted = MessagesController.getInstance().isDialogMuted(selectedDialog) ? R.drawable.mute_fixed : 0;
                      CharSequence cs = muted != 0 ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications);
                      CharSequence csa = LocaleController.getString("AddShortcut", R.string.AddShortcut);
                      if (chat != null && chat.megagroup) {
                          items = new CharSequence[]{LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache), chat == null || !chat.creator ? LocaleController.getString("LeaveMegaMenu", R.string.LeaveMegaMenu) : LocaleController.getString("DeleteMegaMenu", R.string.DeleteMegaMenu), cs, cs2, LocaleController.getString("MarkAsRead", R.string.MarkAsRead), csa};
                      } else {
                          items = new CharSequence[]{LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache), chat == null || !chat.creator ? LocaleController.getString("LeaveChannelMenu", R.string.LeaveChannelMenu) : LocaleController.getString("ChannelDeleteMenu", R.string.ChannelDeleteMenu), cs, cs2, LocaleController.getString("MarkAsRead", R.string.MarkAsRead), csa};
                      }

                      builder.setItems(items, new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, final int which) {
                              if (which == 3) {
                                  TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                  if (isFav) {
                                      Favourite.deleteFavourite(selectedDialog);
                                      MessagesController.getInstance().dialogsFavs.remove(dialg);
                                  } else {
                                      Favourite.addFavourite(selectedDialog);
                                      MessagesController.getInstance().dialogsFavs.add(dialg);
                                  }
                                  if (dialogsType == 1) {
                                      dialogsAdapter.notifyDataSetChanged();
                                      if (!hideTabs) {
                                          updateTabs();
                                      }
                                  }
                                  unreadCount(MessagesController.getInstance().dialogsFavs, favsCounter);
                              } else if (which == 2) {
                                  boolean muted = MessagesController.getInstance().isDialogMuted(selectedDialog);
                                  if (!muted) {
                                      long flags;
                                      SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                      SharedPreferences.Editor editor = preferences.edit();
                                      editor.putInt("notify2_" + selectedDialog, 2);
                                      flags = 1;
                                      MessagesStorage.getInstance().setDialogFlags(selectedDialog, flags);
                                      editor.commit();
                                      TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                      if (dialg != null) {
                                          dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                      }
                                      NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                  } else {
                                      SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                      SharedPreferences.Editor editor = preferences.edit();
                                      editor.putInt("notify2_" + selectedDialog, 0);
                                      MessagesStorage.getInstance().setDialogFlags(selectedDialog, 0);
                                      editor.commit();
                                      TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                      if (dialg != null) {
                                          dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                      }
                                      NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                  }
                              }
                              //
                              else if (which == 4) {
                                  markAsReadDialog(false);
                              } else if (which == 5) {
                                  addShortcut();
                              }
                              //
                              else {
                                  AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                  //builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                  builder.setTitle(chat != null ? chat.title : LocaleController.getString("AppName", R.string.AppName));
                                  if (which == 0) {
                                      if (chat != null && chat.megagroup) {
                                          builder.setMessage(LocaleController.getString("AreYouSureClearHistorySuper", R.string.AreYouSureClearHistorySuper));
                                      } else {
                                          builder.setMessage(LocaleController.getString("AreYouSureClearHistoryChannel", R.string.AreYouSureClearHistoryChannel));
                                      }
                                      builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialogInterface, int i) {
                                              MessagesController.getInstance().deleteDialog(selectedDialog, 2);
                                          }
                                      });
                                  } else {
                                      if (chat != null && chat.megagroup) {
                                          if (!chat.creator) {
                                              builder.setMessage(LocaleController.getString("MegaLeaveAlert", R.string.MegaLeaveAlert));
                                          } else {
                                              builder.setMessage(LocaleController.getString("MegaDeleteAlert", R.string.MegaDeleteAlert));
                                          }
                                      } else {
                                          if (chat == null || !chat.creator) {
                                              builder.setMessage(LocaleController.getString("ChannelLeaveAlert", R.string.ChannelLeaveAlert));
                                          } else {
                                              builder.setMessage(LocaleController.getString("ChannelDeleteAlert", R.string.ChannelDeleteAlert));
                                          }
                                      }
                                      builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialogInterface, int i) {
                                              MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, UserConfig.getCurrentUser(), null);
                                              if (AndroidUtilities.isTablet()) {
                                                  NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                              }
                                          }
                                      });
                                  }
                                  builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                  showDialog(builder.create());
                              }
                          }
                      });
                      showDialog(builder.create());
                  } else {
                      final boolean isChat = (int) dialog.id < 0 && (int) (dialog.id >> 32) != 1;
                      int muted = MessagesController.getInstance().isDialogMuted(selectedDialog) ? R.drawable.mute_fixed : 0;
                      TLRPC.User user = null;
                      if (!isChat && lower_id > 0 && high_id != 1) {
                          user = MessagesController.getInstance().getUser(lower_id);
                      }
                      final boolean isBot = user != null && user.bot;
                      final boolean isFav = Favourite.isFavourite(dialog.id);
                      CharSequence cs = isFav ? LocaleController.getString("DeleteFavourite", R.string.DeleteFromFavorites) : LocaleController.getString("AddFavourite", R.string.AddToFavorites);
                      CharSequence csa = LocaleController.getString("AddShortcut", R.string.AddShortcut);
                      CharSequence asc = LocaleController.getString("AddSpecific", R.string.AddSpecific);
//                      CharSequence atc = LocaleController.getString("addToCategory", R.string.addToCategory);

                      if (!isBot && !isChat) {
                          final TLRPC.User finalUser = user;
                          builder.setItems(new CharSequence[]{LocaleController.getString("ClearHistory", R.string.ClearHistory),
                            isChat ? LocaleController.getString("DeleteChat", R.string.DeleteChat) :
                              isBot ? LocaleController.getString("DeleteAndStop", R.string.DeleteAndStop) : LocaleController.getString("Delete", R.string.Delete), muted != 0 ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications), cs, LocaleController.getString("MarkAsRead", R.string.MarkAsRead), csa, asc, LocaleController.getString("hideChat", R.string.hideChat)}, new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialog, final int which) {

                                  if (which == 8) {
                                      addtoCategory(finalUser, context);
                                  } else if (which == 7) {
                                      insertHidden((int) selectedDialog);
                                  } else if (which == 6) {
                                      addspecefic(finalUser, context);
                                  } else if (which == 3) {
                                      TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                      if (isFav) {
                                          Favourite.deleteFavourite(selectedDialog);
                                          MessagesController.getInstance().dialogsFavs.remove(dialg);
                                      } else {
                                          Favourite.addFavourite(selectedDialog);
                                          MessagesController.getInstance().dialogsFavs.add(dialg);
                                      }
                                      if (dialogsType == 1) {
                                          dialogsAdapter.notifyDataSetChanged();
                                          if (!hideTabs) {
                                              updateTabs();
                                          }
                                      }
                                      unreadCount(MessagesController.getInstance().dialogsFavs, favsCounter);
                                  } else if (which == 2) {
                                      boolean muted = MessagesController.getInstance().isDialogMuted(selectedDialog);
                                      if (!muted) {
                                          long flags;
                                          SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                          SharedPreferences.Editor editor = preferences.edit();
                                          editor.putInt("notify2_" + selectedDialog, 2);
                                          flags = 1;
                                          MessagesStorage.getInstance().setDialogFlags(selectedDialog, flags);
                                          editor.commit();
                                          TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                          if (dialg != null) {
                                              dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                          }
                                          NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                      } else {
                                          SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                          SharedPreferences.Editor editor = preferences.edit();
                                          editor.putInt("notify2_" + selectedDialog, 0);
                                          MessagesStorage.getInstance().setDialogFlags(selectedDialog, 0);
                                          editor.commit();
                                          TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                          if (dialg != null) {
                                              dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                          }
                                          NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                      }
                                  }//
                                  else if (which == 4) {
                                      markAsReadDialog(false);
                                  } else if (which == 5) {
                                      addShortcut();
                                  }
                                  //
                                  else {
                                      AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                      //builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                      TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
                                      TLRPC.User user = MessagesController.getInstance().getUser((int) selectedDialog);
                                      String title = currentChat != null ? currentChat.title : user != null ? UserObject.getUserName(user) : LocaleController.getString("AppName", R.string.AppName);
                                      builder.setTitle(title);
                                      if (which == 0) {
                                          builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));
                                      } else {

                                          if (isChat) {
                                              builder.setMessage(ChatObject.isChannel(chat_id) ? LocaleController.getString("ChannelLeaveAlert", R.string.ChannelLeaveAlert) : LocaleController.getString("AreYouSureDeleteAndExit", R.string.AreYouSureDeleteAndExit));
                                          } else {
                                              builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                                          }
                                      }
                                      builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialogInterface, int i) {
                                              if (which != 0) {
                                                  if (isChat) {
                                                      TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
                                                      if (currentChat != null && ChatObject.isNotInChat(currentChat)) {
                                                          MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                                      } else {
                                                          MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, MessagesController.getInstance().getUser(UserConfig.getClientUserId()), null);
                                                      }
                                                  } else {
                                                      MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                                  }
                                                  if (isBot) {
                                                      MessagesController.getInstance().blockUser((int) selectedDialog);
                                                  }
                                                  if (AndroidUtilities.isTablet()) {
                                                      NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                                  }
                                              } else {
                                                  MessagesController.getInstance().deleteDialog(selectedDialog, 1);
                                              }
                                          }
                                      });
                                      builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                      showDialog(builder.create());
                                  }
                              }
                          });
                          showDialog(builder.create());

                      } else {
//                          CharSequence atc2 = LocaleController.getString("addToCategory", R.string.addToCategory);
                          builder.setItems(new CharSequence[]{LocaleController.getString("ClearHistory", R.string.ClearHistory),
                            isChat ? LocaleController.getString("DeleteChat", R.string.DeleteChat) :
                              isBot ? LocaleController.getString("DeleteAndStop", R.string.DeleteAndStop) : LocaleController.getString("Delete", R.string.Delete), muted != 0 ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications), cs, LocaleController.getString("MarkAsRead", R.string.MarkAsRead), csa, LocaleController.getString("hideChat", R.string.hideChat)}, new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialog, final int which) {
                                  if (which == 7) {
                                      addtoCategory(selectedDialog, context);
                                  } else if (which == 6) {
                                      insertHidden((int) selectedDialog);
                                  } else if (which == 3) {
                                      TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                      if (isFav) {
                                          Favourite.deleteFavourite(selectedDialog);
                                          MessagesController.getInstance().dialogsFavs.remove(dialg);
                                      } else {
                                          Favourite.addFavourite(selectedDialog);
                                          MessagesController.getInstance().dialogsFavs.add(dialg);
                                      }

                                      if (dialogsType == 1) {
                                          dialogsAdapter.notifyDataSetChanged();
                                          if (!hideTabs) {
                                              updateTabs();
                                          }
                                      }


                                      unreadCount(MessagesController.getInstance().dialogsFavs, favsCounter);
                                  } else if (which == 2) {
                                      boolean muted = MessagesController.getInstance().isDialogMuted(selectedDialog);
                                      if (!muted) {
                                          long flags;
                                          SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                          SharedPreferences.Editor editor = preferences.edit();
                                          editor.putInt("notify2_" + selectedDialog, 2);
                                          flags = 1;
                                          MessagesStorage.getInstance().setDialogFlags(selectedDialog, flags);
                                          editor.commit();
                                          TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                          if (dialg != null) {
                                              dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                          }
                                          NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                      } else {
                                          SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                          SharedPreferences.Editor editor = preferences.edit();
                                          editor.putInt("notify2_" + selectedDialog, 0);
                                          MessagesStorage.getInstance().setDialogFlags(selectedDialog, 0);
                                          editor.commit();
                                          TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                          if (dialg != null) {
                                              dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                          }
                                          NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                      }
                                  }//
                                  else if (which == 4) {
                                      markAsReadDialog(false);
                                  } else if (which == 5) {
                                      addShortcut();
                                  }
                                  //
                                  else {
                                      AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                      //builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                      TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
                                      TLRPC.User user = MessagesController.getInstance().getUser((int) selectedDialog);
                                      String title = currentChat != null ? currentChat.title : user != null ? UserObject.getUserName(user) : LocaleController.getString("AppName", R.string.AppName);
                                      builder.setTitle(title);
                                      if (which == 0) {
                                          builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));
                                      } else {
                                          if (isChat) {
                                              builder.setMessage(ChatObject.isChannel(chat_id) ? LocaleController.getString("ChannelLeaveAlert", R.string.ChannelLeaveAlert) : LocaleController.getString("AreYouSureDeleteAndExit", R.string.AreYouSureDeleteAndExit));
                                          } else {
                                              builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                                          }
                                      }
                                      builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialogInterface, int i) {
                                              if (which != 0) {
                                                  if (isChat) {
                                                      TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
                                                      if (currentChat != null && ChatObject.isNotInChat(currentChat)) {
                                                          MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                                      } else {
                                                          MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, MessagesController.getInstance().getUser(UserConfig.getClientUserId()), null);
                                                      }
                                                  } else {
                                                      MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                                  }
                                                  if (isBot) {
                                                      MessagesController.getInstance().blockUser((int) selectedDialog);
                                                  }
                                                  if (AndroidUtilities.isTablet()) {
                                                      NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                                  }
                                              } else {
                                                  MessagesController.getInstance().deleteDialog(selectedDialog, 1);
                                              }
                                          }
                                      });
                                      builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                      showDialog(builder.create());
                                  }
                              }
                          });
                          showDialog(builder.create());
                      }
                  }
                  return true;
              }
          });

        searchEmptyView = new EmptyTextProgressView(context);

        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        frameLayout.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new LinearLayout(context);

        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        //emptyView.setOnTouchListener(new View.OnTouchListener() {
        //
        //    @Override
        //    public boolean onTouch(View v, MotionEvent event) {
        //        return true;
        //    }
        //});
        emptyView.setOnTouchListener(onTouchListener);
        TextView textView = new TextView(context);
        textView.setText(LocaleController.getString("NoChats", R.string.NoChats));
        textView.setTextColor(0xff959595);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        textView = new TextView(context);

        String help = LocaleController.getString("NoChatsHelp", R.string.NoChatsHelp);
        if (AndroidUtilities.isTablet() && !AndroidUtilities.isSmallTablet())

        {
            help = help.replace('\n', ' ');
        }

        textView.setText(help);
        textView.setTextColor(0xff959595);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), 0);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1);
        emptyView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        progressView = new ProgressBar(context);

        progressView.setVisibility(View.GONE);
        frameLayout.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        floatingButton = new ImageView(context);

        floatingButton.setVisibility(onlySelect ? View.GONE : View.VISIBLE);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);
        floatingButton.setVisibility(View.GONE);
        floatingButton.setBackgroundResource(R.drawable.floating_states);
        Drawable pencilDrawableWhite = getParentActivity().getResources().getDrawable(R.drawable.floating_pencil);
        floatingButton.setImageDrawable(pencilDrawableWhite);
        if (Build.VERSION.SDK_INT >= 21)

        {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }

        frameLayout.addView(floatingButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (Gravity.RIGHT) | Gravity.BOTTOM, 0, 0, 5, 47));
        floatingButton.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
                                                  Bundle args = new Bundle();
                                                  args.putBoolean("destroyAfterSelect", true);
                                                  presentFragment(new ContactsActivity(args));
                                              }
                                          }
        );

        tabsView = new FrameLayout(context);
        createTabs(context);
        //if(dialogsType == 0 || dialogsType > 2){
        frameLayout.addView(tabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, tabsHeight, Gravity.BOTTOM, 0, 0, 0, 0));
        //}
        int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);
        final int hColor = themePrefs.getInt("chatsHeaderColor", 0xffffffff);

        ////////////////////////////////////////////End Tab
        if (dialogsType != 11){
            listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && searchWas) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                    Glow.setEdgeGlowColor(listView, hColor);
                }
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    int visibleItemCount = Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                    int totalItemCount = recyclerView.getAdapter().getItemCount();
                    if (searching && searchWas) {
                        if (visibleItemCount > 0 && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                            dialogsSearchAdapter.loadMoreSearchMessages();
                        }
                        return;
                    }
                    if (visibleItemCount > 0) {
                        if (layoutManager.findLastVisibleItemPosition() >= getDialogsArray().size() - 10) {
                            MessagesController.getInstance().loadDialogs(-1, 100, !MessagesController.getInstance().dialogsEndReached);
                        }
                    }
//                if (floatingButton.getVisibility() != View.GONE) {
//                    final View topChild = recyclerView.getChildAt(0);
//                    int firstViewTop = 0;
//                    if (topChild != null) {
//                        firstViewTop = topChild.getTop();
//                    }
//                    boolean goingDown;
//                    boolean changed = true;
//                    if (prevPosition == firstVisibleItem) {
//                        final int topDelta = prevTop - firstViewTop ;
//                        goingDown = firstViewTop < prevTop;
//                        changed = Math.abs(topDelta) > 1;
//                    } else {
//                        goingDown = firstVisibleItem > prevPosition;
//                    }
//                    if (changed && scrollUpdated) {
//                        if (!hideTabs && !disableAnimation || hideTabs)
//                            hideFloatingButton(goingDown);
//                        }
//                    prevPosition = firstVisibleItem;
//                    prevTop = firstViewTop;
//                    scrollUpdated = true;
//                }

//                     if (!hideTabs) {
                                                 //if(!disableAnimation) {
//                         if (dy > 1) {
//                             //Down (HIDE)
//                             if (recyclerView.getChildAt(0).getTop() < 0) {
//                                 if (!disableAnimation) {
//                                     hideTabsAnimated(true);
//                                 } else {
//                                     hideFloatingButton(true);
//                                 }
//                             }
//
//                         }
//                         if (dy < -1) {
//                             //Up (SHOW)
//                             if (!disableAnimation) {
//                                 hideTabsAnimated(false);
//                                 if (firstVisibleItem == 0) {
//                                     listView.setPadding(0, AndroidUtilities.dp(tabsHeight), 0, 0);
//                                 }
//                             } else {
//                                 hideFloatingButton(false);
//                             }
//                         }
                                                 //}
//                     }
                }
            });
        }

        if (searchString == null)
        {
            dialogsAdapter = new DialogsAdapter(context, dialogsType);
            if (AndroidUtilities.isTablet() && openedDialogId != 0) {
                dialogsAdapter.setOpenedDialogId(openedDialogId);
            }
            listView.setAdapter(dialogsAdapter);
            dialogsBackupAdapter = dialogsAdapter;
        }

        int type = 0;
        if (searchString != null)

        {
            type = 2;
        } else if (!onlySelect)

        {
            type = 1;
        }

        dialogsSearchAdapter = new DialogsSearchAdapter(context, type, dialogsType);
        dialogsSearchAdapter.setDelegate(new DialogsSearchAdapter.DialogsSearchAdapterDelegate() {
            @Override
            public void searchStateChanged(boolean search) {
                if (searching && searchWas && searchEmptyView != null) {
                    if (search) {
                        searchEmptyView.showProgress();
                    } else {
                        searchEmptyView.showTextView();
                    }
                }
            }

            @Override
            public void didPressedOnSubDialog(int did) {
                if (onlySelect) {
                    didSelectResult(did, true, false);
                } else {
                    Bundle args = new Bundle();
                    if (did > 0) {
                        args.putInt("user_id", did);
                    } else {
                        args.putInt("chat_id", -did);
                    }
                    if (actionBar != null) {
                        actionBar.closeSearchField();
                    }
                    if (AndroidUtilities.isTablet()) {
                        if (dialogsAdapter != null) {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = did);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                        }
                    } else {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            presentFragment(new ChatActivity(args));
                        }
                    }
                }
            }

            @Override
            public void needRemoveHint(final int did) {
                if (getParentActivity() == null) {
                    return;
                }
                TLRPC.User user = MessagesController.getInstance().getUser(did);
                if (user == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(LocaleController.formatString("ChatHintsDelete", R.string.ChatHintsDelete, ContactsController.formatName(user.first_name, user.last_name)));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SearchQuery.removePeer(did);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            }
        });


        if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty())

        {
            searchEmptyView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            listView.setEmptyView(progressView);
        } else

        {
            searchEmptyView.setVisibility(View.GONE);
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }

        if (searchString != null)

        {
            actionBar.openSearchField(searchString);
        }

        //if (!onlySelect && dialogsType == 0) {
        if (!onlySelect && (dialogsType == 0 || dialogsType > 2))

        {
            frameLayout.addView(new PlayerView(context, this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
        }

        toastBtn = new Button(context);

        toastBtn.setVisibility(AndroidUtilities.themeUpdated ? View.VISIBLE : View.GONE);
        if (AndroidUtilities.themeUpdated)

        {
            AndroidUtilities.themeUpdated = false;
            String tName = themePrefs.getString("themeName", "");
            //int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);
            //int hColor = themePrefs.getInt("chatsHeaderColor", def);
            toastBtn.setText(LocaleController.formatString("ThemeUpdated", R.string.ThemeUpdated, tName));
            if (Build.VERSION.SDK_INT >= 14) toastBtn.setAllCaps(false);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(AndroidUtilities.dp(8));
            shape.setColor(hColor);
            toastBtn.setBackgroundDrawable(shape);
            toastBtn.setTextColor(tColor);
            toastBtn.setTextSize(16);
            ViewProxy.setTranslationY(toastBtn, -AndroidUtilities.dp(100));
            ObjectAnimatorProxy animator = ObjectAnimatorProxy.ofFloatProxy(toastBtn, "translationY", 0).setDuration(500);
            //animator.setInterpolator(tabsInterpolator);
            animator.start();

            toastBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String packageName = "es.rafalense.themes";
                        if (BuildConfig.DEBUG) packageName = "es.rafalense.themes.beta";
                        Intent intent = ApplicationLoader.applicationContext.getPackageManager().getLaunchIntentForPackage(packageName);
                        if (intent != null) {
                            ApplicationLoader.applicationContext.startActivity(intent);
                        }
                        toastBtn.setVisibility(View.GONE);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            });
            frameLayout.addView(toastBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER, 0, 10, 0, 0));
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            ObjectAnimatorProxy animator = ObjectAnimatorProxy.ofFloatProxy(toastBtn, "translationY", -AndroidUtilities.dp(100)).setDuration(500);
                            //animator.setInterpolator(tabsInterpolator);
                            animator.start();
                        }
                    });
                }
            }, 4000);
        }

        return fragmentView;
    }


    public void addtoCategory(final TLRPC.User finalUser, final Context mContext) {


        final CharSequence[] items;

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.addToCategory));
        builder.setCancelable(false);
        builder.setIcon(R.mipmap.ic_launcher);

        categoryDBAdapter db = new categoryDBAdapter(mContext);
        db.open();
        List<category> categories = new ArrayList<>();
        categories = db.getAllItms();
        db.close();


        if (categories.size() > 0) {
            items = new CharSequence[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                items[i] = categories.get(i).getName();
            }


            final List<category> finalCategories = categories;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    catDBAdapter cdb = new catDBAdapter(mContext);
                    cdb.open();
                    chatobject chat = new chatobject();
                    chat.setDialog_id(finalUser.id);
                    chat.setCatCode(finalCategories.get(item).getId());
                    cdb.insert(chat);
                    cdb.close();

                }
            });

        } else {
            builder.setMessage(mContext.getResources().getString(R.string.noCategory));
            builder.setPositiveButton(mContext.getResources().getString(R.string.newCategory), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    presentFragment(new categoryManagement());
                }
            });

        }


        builder.setNegativeButton(mContext.getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismis
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }


    public void addtoCategory(final long selectedDialog, final Context mContext) {


        final CharSequence[] items;

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.addToCategory));
        builder.setCancelable(false);
        builder.setIcon(R.mipmap.ic_launcher);

        categoryDBAdapter db = new categoryDBAdapter(mContext);
        db.open();
        List<category> categories = new ArrayList<>();
        categories = db.getAllItms();
        db.close();


        if (categories.size() > 0) {
            items = new CharSequence[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                items[i] = categories.get(i).getName();
            }


            final List<category> finalCategories = categories;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    catDBAdapter cdb = new catDBAdapter(mContext);
                    cdb.open();
                    chatobject chat = new chatobject();
                    chat.setDialog_id((int) selectedDialog);
                    chat.setCatCode(finalCategories.get(item).getId());
                    cdb.insert(chat);
                    cdb.close();

                }
            });

        } else {
            builder.setMessage(mContext.getResources().getString(R.string.noCategory));
            builder.setPositiveButton(mContext.getResources().getString(R.string.newCategory), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    presentFragment(new categoryManagement());
                }
            });

        }


        builder.setNegativeButton(mContext.getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismis
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void showCats(final Context mContext) {


        final CharSequence[] items;

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.selectCategory));
        builder.setCancelable(false);
        builder.setIcon(R.mipmap.ic_launcher);

        categoryDBAdapter db = new categoryDBAdapter(mContext);
        db.open();
        List<category> categories = new ArrayList<>();
        categories = db.getAllItms();
        db.close();


        if (categories.size() >= 0) {
            items = new CharSequence[categories.size() + 1];
            items[0] = mContext.getResources().getString(R.string.All);
            for (int i = 0; i < categories.size(); i++) {
                items[i + 1] = categories.get(i).getName();
            }


            final List<category> finalCategories = categories;
            final List<category> finalCategories1 = categories;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
                    if (item == 0) {
                        sharedPreferences.edit().putInt("selectedCat", -1).commit();
                    } else {
                        sharedPreferences.edit().putInt("selectedCat", finalCategories1.get(item - 1).getId()).commit();
                    }


                    refreshCategory();
                }
            });

        }


        builder.setNegativeButton(mContext.getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismis
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void addspecefic(final TLRPC.User finalUser, final Context context) {

        if (finalUser != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            //builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setTitle(context.getResources().getString(R.string.AddSpecific));

            final Boolean[] items = {true, true, true, false};

            builder.setMultiChoiceItems(new CharSequence[]{context.getResources().getString(R.string.picup),
                context.getResources().getString(R.string.statusup),
                context.getResources().getString(R.string.phoneup),
                context.getResources().getString(R.string.isonetime)},
              new boolean[]{true, true, true, false},
              new DialogInterface.OnMultiChoiceClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                      items[which] = isChecked;

                  }
              });

            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    userDBAdapter db = new userDBAdapter(context);
                    db.open();
                    db.updateIsSpecific(finalUser.id, 1);
                    db.updatePicUp(finalUser.id, items[0] ? 1 : 0);
                    db.updateStatusUp(finalUser.id, items[1] ? 1 : 0);
                    db.updatePhoneUp(finalUser.id, items[2] ? 1 : 0);
                    db.updateIsOneTime(finalUser.id, items[3] ? 1 : 0);
                    db.close();
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void changeGhostModeState() {
//        if (headerItem == null)
//            return;

        boolean ghost_mpode = true;
        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        boolean mode = sharedPreferences.getBoolean("ghost_mode", false);
        edit.putBoolean("ghost_mode", !mode);
        edit.putBoolean("not_send_read_state", !mode);
        edit.commit();
        if (mode) {
            ghost_mpode = false;
        }

        this.actionBar.changeGhostModeVisibility();

//        Log.i("TAG", "changeGhostModeState: gost " + ghost_mpode);
        MessagesController.getInstance().reRunUpdateTimerProc();
//        Drawable ghosticon = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost_disabled);
//        ghosticon.setColorFilter(0xff0288d1, PorterDuff.Mode.MULTIPLY);

        if (ghost_mpode) {

//            ghosticon = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost);
//            ghosticon.setColorFilter(0xff0288d1, PorterDuff.Mode.MULTIPLY);

            Toast.makeText(getParentActivity().getApplicationContext(), getParentActivity().getResources().getString(R.string.gost_disabled), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getParentActivity().getApplicationContext(), getParentActivity().getResources().getString(R.string.gost_enabled), Toast.LENGTH_SHORT).show();

        }
//        headerItem.setIcon(ghosticon);

        if (ghost_mpode && this.parentLayout != null) {
            this.parentLayout.rebuildAllFragmentViews(false);
        }
        if (getParentActivity() != null) {
            PhotoViewer.getInstance().destroyPhotoViewer();
            PhotoViewer.getInstance().setParentActivity(getParentActivity());
        }
    }

    private void markAsReadDialog(final boolean all) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
        TLRPC.User user = MessagesController.getInstance().getUser((int) selectedDialog);
        String title = currentChat != null ? currentChat.title : user != null ? UserObject.getUserName(user) : LocaleController.getString("AppName", R.string.AppName);
        builder.setTitle(all ? getHeaderAllTitles() : title);
        builder.setMessage((all ? LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead) : LocaleController.getString("MarkAsRead", R.string.MarkAsRead)) + '\n' + LocaleController.getString("AreYouSure", R.string.AreYouSure));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (all) {
                    ArrayList<TLRPC.TL_dialog> dialogs = getDialogsArray();
                    if (dialogs != null && !dialogs.isEmpty()) {
                        for (int a = 0; a < dialogs.size(); a++) {
                            TLRPC.TL_dialog dialg = getDialogsArray().get(a);
                            if (dialg.unread_count > 0) {
                                MessagesController.getInstance().markDialogAsRead(dialg.id, dialg.last_read, Math.max(0, dialg.top_message), dialg.last_message_date, true, false);
                            }
                        }
                    }
                } else {
                    TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                    if (dialg.unread_count > 0) {
                        MessagesController.getInstance().markDialogAsRead(dialg.id, dialg.last_read, Math.max(0, dialg.top_message), dialg.last_message_date, true, false);
                    }
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void addShortcut() {

        Intent shortcutIntent = new Intent(ApplicationLoader.applicationContext, ShortcutActivity.class);
        shortcutIntent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
        shortcutIntent.setFlags(32768);

        TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
        TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
        TLRPC.User user = MessagesController.getInstance().getUser((int) selectedDialog);
        TLRPC.EncryptedChat encryptedChat = null;

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        long dialog_id = dialg.id;

        int lower_id = (int) dialog_id;
        int high_id = (int) (dialog_id >> 32);
        if (lower_id != 0) {
            if (high_id == 1) {
                currentChat = MessagesController.getInstance().getChat(lower_id);
                shortcutIntent.putExtra("chatId", lower_id);
                avatarDrawable.setInfo(currentChat);
            } else {
                if (lower_id < 0) {
                    currentChat = MessagesController.getInstance().getChat(-lower_id);
                    if (currentChat != null && currentChat.migrated_to != null) {
                        TLRPC.Chat chat2 = MessagesController.getInstance().getChat(currentChat.migrated_to.channel_id);
                        if (chat2 != null) {
                            currentChat = chat2;
                        }
                    }
                    shortcutIntent.putExtra("chatId", -lower_id);
                    avatarDrawable.setInfo(currentChat);
                } else {
                    user = MessagesController.getInstance().getUser(lower_id);
                    shortcutIntent.putExtra("userId", lower_id);
                    avatarDrawable.setInfo(user);
                }
            }
        } else {
            encryptedChat = MessagesController.getInstance().getEncryptedChat(high_id);
            if (encryptedChat != null) {
                user = MessagesController.getInstance().getUser(encryptedChat.user_id);
                shortcutIntent.putExtra("encId", high_id);
                avatarDrawable.setInfo(user);
            }
        }

        final String name = currentChat != null ? currentChat.title : user != null && encryptedChat == null ? UserObject.getUserName(user) : encryptedChat != null ? new String(Character.toChars(0x1F512)) + UserObject.getUserName(user) : null;
        //Log.e("DialogsActivity", "addShortcut " + user.id);
        if (name == null) {
            return;
        }
        //Log.e("Plus", "addShortcut " + name + " dialog_id " + dialog_id);

        TLRPC.FileLocation photoPath = null;

        if (currentChat != null) {
            if (currentChat.photo != null && currentChat.photo.photo_small != null && currentChat.photo.photo_small.volume_id != 0 && currentChat.photo.photo_small.local_id != 0) {
                photoPath = currentChat.photo.photo_small;
            }
        } else if (user != null) {
            if (user.photo != null && user.photo.photo_small != null && user.photo.photo_small.volume_id != 0 && user.photo.photo_small.local_id != 0) {
                photoPath = user.photo.photo_small;
            }
        }
        BitmapDrawable img = null;

        if (photoPath != null) {
            img = ImageLoader.getInstance().getImageFromMemory(photoPath, null, "50_50");
        }

        String action = "com.android.launcher.action.INSTALL_SHORTCUT";
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);

        if (img != null) {
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getRoundBitmap(img.getBitmap()));
        } else {
            int w = AndroidUtilities.dp(40);
            int h = AndroidUtilities.dp(40);
            Bitmap mutableBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mutableBitmap);
            avatarDrawable.setBounds(0, 0, w, h);
            avatarDrawable.draw(canvas);
            //if(mutableBitmap != null){
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getRoundBitmap(mutableBitmap));
            //} else{
            //addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.intro1));
            //}
        }

        addIntent.putExtra("duplicate", false);
        addIntent.setAction(action);
        //addIntent.setPackage(ApplicationLoader.applicationContext.getPackageName());
        boolean error = false;
        if (ApplicationLoader.applicationContext.getPackageManager().queryBroadcastReceivers(new Intent(action), 0).size() > 0) {
            ApplicationLoader.applicationContext.sendBroadcast(addIntent);
        } else {
            error = true;
        }
        final String msg = error ? LocaleController.formatString("ShortcutError", R.string.ShortcutError, name) : LocaleController.formatString("ShortcutAdded", R.string.ShortcutAdded, name);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (getParentActivity() != null) {
                    Toast toast = Toast.makeText(getParentActivity(), msg, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private Bitmap getRoundBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int radius = Math.min(h / 2, w / 2);
        Bitmap output = Bitmap.createBitmap(w + 8, h + 8, Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        p.setAntiAlias(true);
        Canvas c = new Canvas(output);
        c.drawARGB(0, 0, 0, 0);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(bitmap, 4, 4, p);
        return output;
    }

    public class DialogsOnTouch implements View.OnTouchListener {

        private DisplayMetrics displayMetrics;
        //private static final String logTag = "SwipeDetector";
        private static final int MIN_DISTANCE_HIGH = 40;
        private static final int MIN_DISTANCE_HIGH_Y = 60;
        private float downX, downY, upX, upY;
        private float vDPI;

        Context mContext;

        public DialogsOnTouch(Context context) {
            this.mContext = context;
            displayMetrics = context.getResources().getDisplayMetrics();
            vDPI = displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT;
            //Log.e("DialogsActivity","DialogsOnTouch vDPI " + vDPI);
        }

        public boolean onTouch(View view, MotionEvent event) {

            touchPositionDP = Math.round(event.getX() / vDPI);
            //Log.e("DialogsActivity","onTouch touchPositionDP " + touchPositionDP + " hideTabs " + hideTabs);
            if (hideTabs) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = Math.round(event.getX() / vDPI);
                    downY = Math.round(event.getY() / vDPI);
                    //Log.e("DialogsActivity", "view " + view.toString());
                    if (touchPositionDP > 50) {
                        parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(false, false);
                        //Log.e("DialogsActivity", "DOWN setAllowOpenDrawer FALSE");
                    }
                    //Log.e("DialogsActivity", "DOWN downX " + downX);
                    return view instanceof LinearLayout; // for emptyView
                }
                case MotionEvent.ACTION_UP: {
                    upX = Math.round(event.getX() / vDPI);
                    upY = Math.round(event.getY() / vDPI);
                    float deltaX = downX - upX;
                    float deltaY = downY - upY;
                    //Log.e(logTag, "MOVE X " + deltaX);
                    //Log.e(logTag, "MOVE Y " + deltaY);
                    //Log.e("DialogsActivity", "UP downX " + downX);
                    //Log.e("DialogsActivity", "UP upX " + upX);
                    //Log.e("DialogsActivity", "UP deltaX " + deltaX);
                    // horizontal swipe detection
                    if (Math.abs(deltaX) > MIN_DISTANCE_HIGH && Math.abs(deltaY) < MIN_DISTANCE_HIGH_Y) {
                        //if (Math.abs(deltaX) > MIN_DISTANCE_HIGH) {
                        refreshDialogType(deltaX < 0 ? 0 : 1);//0: Left - Right 1: Right - Left
                        downX = Math.round(event.getX() / vDPI);
                        refreshAdapter(mContext);
                        //dialogsAdapter.notifyDataSetChanged();
                        refreshTabAndListViews(false);
                        //return true;
                    }
                    //Log.e("DialogsActivity", "UP2 downX " + downX);
                    if (touchPositionDP > 50) {
                        parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(true, false);

                    }
                    //downX = downY = upX = upY = 0;
                    return false;
                }
            }

            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

//        Log.i("anna", "onPause: " + dialogsType + dialogsType + "con: " + contactIsShowing + "set: " + settingIsShowing);
        settingFragment.onPause();
        contactFragment.onPause();

        if (dialogsType == 10) {
            settingIsShowing = true;
        }
        if (dialogsType == 11) {
            contactIsShowing = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

//        Log.i("anna", "onResume: " + dialogsType + "con: " + contactIsShowing + "set: " + settingIsShowing);
        settingFragment.onResume();
        contactFragment.onResume();
        if (dialogsType == 10) {
            dialogsType = 0;
            settingIsShowing = false;
            frameLayout.removeView(settingView);
            actionBar.setVisibility(View.VISIBLE);
            floatingButton.setVisibility(View.GONE);
            refreshAdapter(ApplicationLoader.applicationContext);
        }
        if (dialogsType == 11) {
            dialogsType = 0;
            contactIsShowing = false;
            frameLayout.removeView(contactView);
            searchItem.setVisibility(View.VISIBLE);
            floatingButton.setVisibility(View.GONE);
            refreshAdapter(ApplicationLoader.applicationContext);
        }
        if (needRefreshCategory)
            refreshCategory();
//        MenuCount(notifsCounter);

        if (dialogsAdapter != null) {
            dialogsAdapter.notifyDataSetChanged();
        }
        if (dialogsSearchAdapter != null) {
            dialogsSearchAdapter.notifyDataSetChanged();
        }
        if (checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionContacts", R.string.PermissionContacts));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons();
                    }
                }
            }
        }
        updateTheme();
        unreadCount();
        this.actionBar.changeGhostModeVisibility();

        Config popup = new Config(ApplicationLoader.applicationContext);
        String strPopup = popup.ReadPopup();
        if(!strPopup.equals("nothing"))
        {
            Activity activity = getParentActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setMessage(strPopup);
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            showDialog(builder.create());
            popup.WritePopup("nothing");
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_CONTACTS);
            permissons.add(Manifest.permission.WRITE_CONTACTS);
            permissons.add(Manifest.permission.GET_ACCOUNTS);
        }
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        String[] items = permissons.toArray(new String[permissons.size()]);
        activity.requestPermissions(items, 1);
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!onlySelect && floatingButton != null) {
            floatingButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewProxy.setTranslationY(floatingButton, floatingHidden ? AndroidUtilities.dp(100) : 0);
                    floatingButton.setClickable(!floatingHidden);
                    if (floatingButton != null) {
                        if (Build.VERSION.SDK_INT < 16) {
                            floatingButton.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            floatingButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a || grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        ContactsController.getInstance().readContacts();
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        ImageLoader.getInstance().checkMediaPaths();
                        break;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (dialogsAdapter != null) {
                if (dialogsAdapter.isDataSetChanged()) {
                    dialogsAdapter.notifyDataSetChanged();
                } else {
                    updateVisibleRows(MessagesController.UPDATE_MASK_NEW_MESSAGE);
                }
            }
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
            if (listView != null) {
                try {
                    if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                        searchEmptyView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    } else {
                        progressView.setVisibility(View.GONE);
                        if (searching && searchWas) {
                            emptyView.setVisibility(View.GONE);
                            listView.setEmptyView(searchEmptyView);
                        } else {
                            searchEmptyView.setVisibility(View.GONE);
                            listView.setEmptyView(emptyView);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e); //TODO fix it in other way?
                }
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.updateInterfaces) {
            updateVisibleRows((Integer) args[0]);
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded = false;
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.openedChatChanged) {
            if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                boolean close = (Boolean) args[1];
                long dialog_id = (Long) args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                if (dialogsAdapter != null) {
                    dialogsAdapter.setOpenedDialogId(openedDialogId);
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.messageReceivedByAck || id == NotificationCenter.messageReceivedByServer || id == NotificationCenter.messageSendError) {
            updateVisibleRows(MessagesController.UPDATE_MASK_SEND_STATE);
        } else if (id == NotificationCenter.didSetPasscode) {
            updatePasscodeButton();
        } else if (id == NotificationCenter.refreshTabs) {
            updateTabs();
            hideShowTabs((int) args[0]);
        }

        if (id == NotificationCenter.needReloadRecentDialogsSearch) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.loadRecentSearch();
            }
        } else if (id == NotificationCenter.didLoadedReplyMessages) {
            updateVisibleRows(0);
        }
    }

    private ArrayList<TLRPC.TL_dialog> getDialogsArray() {
        if (dialogsType == 0) {
            return MessagesController.getInstance().dialogs;
        } else if (dialogsType == 1) {
            return MessagesController.getInstance().dialogsServerOnly;
        } else if (dialogsType == 2) {
            return MessagesController.getInstance().dialogsGroupsOnly;
        }
        //Telegram
        else if (dialogsType == 3) {
            return MessagesController.getInstance().dialogsUsers;
        } else if (dialogsType == 4) {
            return MessagesController.getInstance().dialogsGroups;
        } else if (dialogsType == 5) {
            return MessagesController.getInstance().dialogsChannels;
        } else if (dialogsType == 6) {
            return MessagesController.getInstance().dialogsBots;
        } else if (dialogsType == 7) {
            return MessagesController.getInstance().dialogsMegaGroups;
        } else if (dialogsType == 8) {
            return MessagesController.getInstance().dialogsFavs;
        } else if (dialogsType == 9) {
            return MessagesController.getInstance().dialogsGroupsAll;
        }
        //
        return null;
    }

    private void updatePasscodeButton() {
        if (passcodeItem == null) {
            return;
        }
        if (UserConfig.passcodeHash.length() != 0 && !searching) {
            passcodeItem.setVisibility(View.VISIBLE);
            SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
            int iconColor = themePrefs.getInt("chatsHeaderIconsColor", 0xff0288D1);
            if (UserConfig.appLocked) {
                //passcodeItem.setIcon(R.drawable.lock_close);
                Drawable lockC = getParentActivity().getResources().getDrawable(R.drawable.lock_close);
                if (lockC != null) lockC.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
                passcodeItem.setIcon(lockC);
            } else {
                //passcodeItem.setIcon(R.drawable.lock_open);
                Drawable lockO = getParentActivity().getResources().getDrawable(R.drawable.lock_open);
                if (lockO != null) lockO.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
                passcodeItem.setIcon(lockO);
            }
        } else {
            passcodeItem.setVisibility(View.GONE);
        }
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide) {
            return;
        }
        floatingHidden = hide;
        ObjectAnimatorProxy animator = ObjectAnimatorProxy.ofFloatProxy(floatingButton, "translationY", floatingHidden ? AndroidUtilities.dp(100) : 0).setDuration(300);
        animator.setInterpolator(floatingInterpolator);
        floatingButton.setClickable(!hide);
        animator.start();
    }

    private void updateVisibleRows(int mask) {
        if (listView == null) {
            return;
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof DialogCell) {
                if (listView.getAdapter() != dialogsSearchAdapter) {
                    DialogCell cell = (DialogCell) child;
                    if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0) {
                        cell.checkCurrentDialogIndex();
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else if ((mask & MessagesController.UPDATE_MASK_SELECT_DIALOG) != 0) {
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else {
                        cell.update(mask);
                    }
                }
            } else if (child instanceof UserCell) {
                ((UserCell) child).update(mask);
            } else if (child instanceof ProfileSearchCell) {
                ((ProfileSearchCell) child).update(mask);
            }
        }
        updateListBG();
        unreadCount();
    }

    private void unreadCount() {
        unreadCount(MessagesController.getInstance().dialogs, allCounter);
        unreadCount(MessagesController.getInstance().dialogsUsers, usersCounter);
        unreadCount(MessagesController.getInstance().dialogsBots, botsCounter);
        unreadCount(MessagesController.getInstance().dialogsChannels, channelsCounter);
        unreadCount(MessagesController.getInstance().dialogsFavs, favsCounter);
        unreadCountGroups();

    }

    private void unreadCountGroups() {
        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        if (hideSGroups) {
            unreadCount(MessagesController.getInstance().dialogsGroupsAll, groupsCounter);
        } else {
            unreadCount(MessagesController.getInstance().dialogsGroups, groupsCounter);
            unreadCount(MessagesController.getInstance().dialogsMegaGroups, sGroupsCounter);
        }
    }

    private void unreadCount(ArrayList<TLRPC.TL_dialog> dialogs, TextView tv) {
        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        boolean hTabs = plusPreferences.getBoolean("hideTabs", false);
        if (hTabs) return;
        boolean hideCounters = plusPreferences.getBoolean("hideTabsCounters", false);
        if (hideCounters) {
            tv.setVisibility(View.GONE);
            return;
        }
        boolean allMuted = true;
        boolean countDialogs = plusPreferences.getBoolean("tabsCountersCountChats", false);
        boolean countNotMuted = plusPreferences.getBoolean("tabsCountersCountNotMuted", false);
        int unreadCount = 0;

        if (dialogs != null && !dialogs.isEmpty()) {
            for (int a = 0; a < dialogs.size(); a++) {
                TLRPC.TL_dialog dialg = dialogs.get(a);
                boolean isMuted = MessagesController.getInstance().isDialogMuted(dialg.id);
                if (!isMuted || !countNotMuted) {
                    int i = dialg.unread_count;
                    if (i > 0) {
                        if (countDialogs) {
                            if (i > 0) unreadCount = unreadCount + 1;
                        } else {
                            unreadCount = unreadCount + i;
                        }
                        if (i > 0 && !isMuted) allMuted = false;
                    }
                }
            }
        }

        if (unreadCount == 0) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText("" + unreadCount);

            SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
            int size = themePrefs.getInt("chatsHeaderTabCounterSize", 11);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
            tv.setPadding(AndroidUtilities.dp(size > 10 ? size - 7 : 4), 0, AndroidUtilities.dp(size > 10 ? size - 7 : 4), 0);
            int cColor = themePrefs.getInt("chatsHeaderTabCounterColor", 0xff29B6F6);
            if (allMuted) {
                tv.getBackground().setColorFilter(themePrefs.getInt("chatsHeaderTabCounterSilentBGColor", 0xff90A4AE), PorterDuff.Mode.SRC_IN);
                tv.setTextColor(0xfffafafa);
            } else {
                tv.getBackground().setColorFilter(themePrefs.getInt("chatsHeaderTabCounterBGColor", 0xffE91E63), PorterDuff.Mode.SRC_IN);
                tv.setTextColor(0xfffafafa);
            }
        }
    }

    private void updateListBG() {


        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int mainColor = 0xfffefefe;
        int value = themePrefs.getInt("chatsRowGradient", 0);
        boolean b = true;//themePrefs.getBoolean("chatsRowGradientListCheck", false);
        if (value > 0 && b) {
            GradientDrawable.Orientation go;
            switch (value) {
                case 2:
                    go = GradientDrawable.Orientation.LEFT_RIGHT;
                    break;
                case 3:
                    go = GradientDrawable.Orientation.TL_BR;
                    break;
                case 4:
                    go = GradientDrawable.Orientation.BL_TR;
                    break;
                default:
                    go = GradientDrawable.Orientation.TOP_BOTTOM;
            }

            int gradColor = themePrefs.getInt("chatsRowGradientColor", 0xfffefefe);
            int[] colors = new int[]{mainColor, gradColor};
            GradientDrawable gd = new GradientDrawable(go, colors);
            listView.setBackgroundDrawable(gd);
        } else {
            listView.setBackgroundColor(mainColor);
        }
    }

    public void setDelegate(DialogsActivityDelegate delegate) {
        this.delegate = delegate;
    }

    public void setSearchString(String string) {
        searchString = string;
    }

    public boolean isMainDialogList() {
        return delegate == null && searchString == null;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (addToGroupAlertString == null) {
            if ((int) dialog_id < 0 && ChatObject.isChannel(-(int) dialog_id) && !ChatObject.isCanWriteToChannel(-(int) dialog_id)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(LocaleController.getString("ChannelCantSendMessage", R.string.ChannelCantSendMessage));
                builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                showDialog(builder.create());
                return;
            }
        }
        if (useAlert && (selectAlertString != null && selectAlertStringGroup != null || addToGroupAlertString != null)) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            int lower_part = (int) dialog_id;
            int high_id = (int) (dialog_id >> 32);
            if (lower_part != 0) {
                if (high_id == 1) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(lower_part);
                    if (chat == null) {
                        return;
                    }
                    builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                } else {
                    if (lower_part > 0) {
                        TLRPC.User user = MessagesController.getInstance().getUser(lower_part);
                        if (user == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
                    } else if (lower_part < 0) {
                        TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                        if (chat == null) {
                            return;
                        }
                        if (addToGroupAlertString != null) {
                            builder.setMessage(LocaleController.formatStringSimple(addToGroupAlertString, chat.title));
                        } else {
                            builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                        }
                    }
                }
            } else {
                TLRPC.EncryptedChat chat = MessagesController.getInstance().getEncryptedChat(high_id);
                TLRPC.User user = MessagesController.getInstance().getUser(chat.user_id);
                if (user == null) {
                    return;
                }
                builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
            }

            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(dialog_id, false, false);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                delegate.didSelectDialog(DialogsActivity.this, dialog_id, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }

    private String getHeaderTitle() {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int value = themePrefs.getInt("chatsHeaderTitle", 0);
        String title = LocaleController.getString("AppName", R.string.AppName);
        TLRPC.User user = UserConfig.getCurrentUser();
        if (value == 1) {
            title = LocaleController.getString("ShortAppName", R.string.ShortAppName);
        } else if (value == 2) {
            if (user != null && (user.first_name != null || user.last_name != null)) {
                title = ContactsController.formatName(user.first_name, user.last_name);
            }
        } else if (value == 3) {
            if (user != null && user.username != null && user.username.length() != 0) {
                title = "@" + user.username;
            }
        } else if (value == 4) {
            title = "";
        }
        return title;
    }

    private String getHeaderAllTitles() {


        switch (dialogsType) {
            case 0:
                return LocaleController.getString("Chats", R.string.Chats);
            case 3:
                return LocaleController.getString("Users", R.string.Users);
            case 4:
            case 9:
                return LocaleController.getString("Groups", R.string.Groups);
            case 5:
                return LocaleController.getString("Channels", R.string.Channels);
            case 6:
                return LocaleController.getString("Bots", R.string.Bots);
            case 7:
                return LocaleController.getString("SuperGroups", R.string.SuperGroups);
            case 8:
                return LocaleController.getString("Favorites", R.string.Favorites);
            case 11:
                return LocaleController.getString("Contacts", R.string.Contacts);
            default:
                return getHeaderTitle();
        }
    }

    /*private void updateHeaderTitle(){
        String s = "";
        switch(dialogsType) {
            case 3:
                s = LocaleController.getString("Users", R.string.Users);
                break;
            case 4:
            case 9:
                s = LocaleController.getString("Groups", R.string.Groups);
                break;
            case 5:
                s = LocaleController.getString("Channels", R.string.Channels);
                break;
            case 6:
                s = LocaleController.getString("Bots", R.string.Bots);
                break;
            case 7:
                s = LocaleController.getString("SuperGroups", R.string.SuperGroups);
                break;
            case 8:
                s = getParentActivity().getString(R.string.Favorites);
                break;
            default:
                s = getHeaderTitle();
            }
        actionBar.setTitle(getHeaderAllTitles());
        paintHeader(true);
    }*/


    private void paintHeader(boolean tabs) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        actionBar.setTitleColor(themePrefs.getInt("chatsHeaderTitleColor", 0xff000000));
        int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);//Theme.ACTION_BAR_COLOR2;
        int hColor = themePrefs.getInt("chatsHeaderColor", 0xfffafafa);
        /*if(!tabs){
            actionBar.setBackgroundColor(hColor);
        }else{
            tabsView.setBackgroundColor(hColor);
        }*/
        if (!tabs) actionBar.setBackgroundColor(hColor);
        if (tabs) {
            tabsView.setBackgroundColor(hColor);
        }
        int val = themePrefs.getInt("chatsHeaderGradient", 0);
        if (val > 0) {
            GradientDrawable.Orientation go;
            switch (val) {
                case 2:
                    go = GradientDrawable.Orientation.LEFT_RIGHT;
                    break;
                case 3:
                    go = GradientDrawable.Orientation.TL_BR;
                    break;
                case 4:
                    go = GradientDrawable.Orientation.BL_TR;
                    break;
                default:
                    go = GradientDrawable.Orientation.TOP_BOTTOM;
            }
            int gradColor = themePrefs.getInt("chatsHeaderGradientColor", def);
            int[] colors = new int[]{hColor, gradColor};
            GradientDrawable gd = new GradientDrawable(go, colors);
            if (!tabs) actionBar.setBackgroundDrawable(gd);
            if (tabs) {
                tabsView.setBackgroundDrawable(gd);
            }
            /*if(!tabs){
                actionBar.setBackgroundDrawable(gd);
            }else{
                tabsView.setBackgroundDrawable(gd);
            }*/
        }
    }

    private void updateTheme() {
        paintHeader(false);
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int def =themePrefs.getInt("themeColor", AndroidUtilities.defColor); //Theme.FAB_COLOR;
        int iconColor = themePrefs.getInt("chatsHeaderIconsColor", 0xff0288D1);
        try {
            int hColor = themePrefs.getInt("chatsHeaderColor", 0xffffffff);
            //Telegram
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bitmap bm = BitmapFactory.decodeResource(getParentActivity().getResources(), R.mipmap.ic_launcher);
                ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(getHeaderTitle(), bm, hColor);
                getParentActivity().setTaskDescription(td);
                bm.recycle();
            }

            Drawable floatingDrawableWhite = getParentActivity().getResources().getDrawable(R.drawable.floating_white);
            if (floatingDrawableWhite != null)
                floatingDrawableWhite.setColorFilter(themePrefs.getInt("chatsFloatingBGColor", def), PorterDuff.Mode.MULTIPLY);
//                floatingButton.setBackgroundColor(0xff039BE5);
            floatingButton.setBackgroundDrawable(floatingDrawableWhite);
            Drawable pencilDrawableWhite = getParentActivity().getResources().getDrawable(R.drawable.floating_pencil);
            if (pencilDrawableWhite != null)
                pencilDrawableWhite.setColorFilter(themePrefs.getInt("chatsFloatingPencilColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);
            floatingButton.setImageDrawable(pencilDrawableWhite);
        } catch (NullPointerException e) {
            FileLog.e("tmessages", e);
        }
        try {
            Drawable search = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_search);
            if (search != null) search.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            Drawable lockO = getParentActivity().getResources().getDrawable(R.drawable.lock_close);
            if (lockO != null) lockO.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            Drawable lockC = getParentActivity().getResources().getDrawable(R.drawable.lock_open);
            if (lockC != null) lockC.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            Drawable clear = getParentActivity().getResources().getDrawable(R.drawable.ic_close_white);
            if (clear != null) clear.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        } catch (OutOfMemoryError e) {
            FileLog.e("tmessages", e);
        }
        refreshTabs();
        paintHeader(true);
    }

    private void createTabs(final Context context) {
        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = plusPreferences.edit();


        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        boolean hideChannels = plusPreferences.getBoolean("hideChannels", false);
        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);
        boolean hideSetting = plusPreferences.getBoolean("hideSetting", false);


        hideTabs = plusPreferences.getBoolean("hideTabs", false);
        disableAnimation = plusPreferences.getBoolean("disableTabsAnimation", false);

        if (hideUsers && hideGroups && hideSGroups && hideChannels && hideBots && hideFavs && hideSetting) {
            if (!hideTabs) {
                hideTabs = true;
                editor.putBoolean("hideTabs", true).apply();
            }
        }

        tabsHeight = plusPreferences.getInt("tabsHeight", 45);

        refreshTabAndListViews(false);

        int t = plusPreferences.getInt("defTab", -1);
        selectedTab = t != -1 ? t : plusPreferences.getInt("selTab", 0);

        if (!hideTabs && dialogsType != selectedTab) {
            dialogsType = selectedTab == 4 && hideSGroups ? 9 : selectedTab;
            dialogsAdapter = new DialogsAdapter(context, dialogsType);
            listView.setAdapter(dialogsAdapter);
            dialogsAdapter.notifyDataSetChanged();
        }

        dialogsBackupAdapter = new DialogsAdapter(context, 0);

        tabsLayout = new LinearLayout(context);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabsLayout.setGravity(Gravity.CENTER);


        contactTab = new ImageView(context);
        //allTab.setScaleType(ImageView.ScaleType.CENTER);
        contactTab.setImageResource(R.drawable.mycon);

        //tabsLayout.addView(allTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));

        contactT = new TextView(context);
        contactT.setText(LocaleController.getString("Contacts", R.string.Contacts));

        contactCounter = new TextView(context);
        contactCounter.setTag("CONTACTS");
        addTabView(context, contactTab, contactCounter, contactT, true);


        //2
        usersTab = new ImageView(context);
        usersTab.setImageResource(R.drawable.tab_user);
        /*usersTab.setScaleType(ImageView.ScaleType.CENTER);
        if(!hideUsers) {
            tabsLayout.addView(usersTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }*/
        usersCounter = new TextView(context);
        usersCounter.setTag("USERS");

        userT = new TextView(context);
        userT.setText(LocaleController.getString("Users", R.string.Users));

        addTabView(context, usersTab, usersCounter, userT, !hideUsers);
        //3
        groupsTab = new ImageView(context);
        groupsTab.setImageResource(R.drawable.tab_group);
        /*groupsTab.setScaleType(ImageView.ScaleType.CENTER);
        if(!hideGroups) {
            tabsLayout.addView(groupsTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }*/

        groupT = new TextView(context);
        groupT.setText(LocaleController.getString("Groups", R.string.Groups));

        groupsCounter = new TextView(context);
        groupsCounter.setTag("GROUPS");
        addTabView(context, groupsTab, groupsCounter, groupT, !hideGroups);
        //4
        superGroupsTab = new ImageView(context);
        superGroupsTab.setImageResource(R.drawable.tab_supergroup);
        /*superGroupsTab.setScaleType(ImageView.ScaleType.CENTER);
        if(!hideSGroups){
            tabsLayout.addView(superGroupsTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }*/
        sGroupT = new TextView(context);
        sGroupT.setText(LocaleController.getString("SuperGroups", R.string.SuperGroups));

        sGroupsCounter = new TextView(context);
        sGroupsCounter.setTag("SGROUP");
        addTabView(context, superGroupsTab, sGroupsCounter, sGroupT, !hideSGroups);
        //5
        channelsTab = new ImageView(context);
        channelsTab.setImageResource(R.drawable.tab_channel);
        /*channelsTab.setScaleType(ImageView.ScaleType.CENTER);
        if(!hideChannels){
            tabsLayout.addView(channelsTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }*/
        channelT = new TextView(context);
        channelT.setText(LocaleController.getString("Channels", R.string.Channels));

        channelsCounter = new TextView(context);
        channelsCounter.setTag("CHANNELS");
        addTabView(context, channelsTab, channelsCounter, channelT, !hideChannels);
        //6
        botsTab = new ImageView(context);
        botsTab.setImageResource(R.drawable.tab_bot);
        /*botsTab.setScaleType(ImageView.ScaleType.CENTER);
        if(!hideBots){
            tabsLayout.addView(botsTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }*/
        botT = new TextView(context);
        botT.setText(LocaleController.getString("Bots", R.string.Bots));

        botsCounter = new TextView(context);
        botsCounter.setTag("BOTS");
        addTabView(context, botsTab, botsCounter, botT, !hideBots);
        //7
        favsTab = new ImageView(context);
        favsTab.setImageResource(R.drawable.tab_favs);
        /*favsTab.setScaleType(ImageView.ScaleType.CENTER);
        if(!hideFavs){
            tabsLayout.addView(favsTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }*/
        favT = new TextView(context);
        favT.setText(LocaleController.getString("Favorites", R.string.Favorites));

        favsCounter = new TextView(context);
        favsCounter.setTag("FAVS");
        addTabView(context, favsTab, favsCounter, favT, !hideFavs);



        allTab = new ImageView(context);
        //allTab.setScaleType(ImageView.ScaleType.CENTER);
        allTab.setImageResource(R.drawable.mychat);

        allT = new TextView(context);
        allT.setText(LocaleController.getString("Chats", R.string.Chats));

        //tabsLayout.addView(allTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));

        allCounter = new TextView(context);
        allCounter.setTag("ALL");
        addTabView(context, allTab, allCounter, allT, true);



        settingTab = new ImageView(context);
        //allTab.setScaleType(ImageView.ScaleType.CENTER);
        settingTab.setImageResource(R.drawable.myset);

        //tabsLayout.addView(allTab, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));

        settingT = new TextView(context);
        settingT.setText(LocaleController.getString("SETTINGS", R.string.SETTINGS));

        settingCounter = new TextView(context);
        settingCounter.setTag("SETTING");
        addTabView(context, settingTab, settingCounter, settingT, true);

        LinearLayout llLine = new LinearLayout(context);
        llLine.setBackgroundColor(0x22000000);
        llLine.setPadding(0, 0, 0, 0);

        tabsView.addView(llLine, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1,Gravity.TOP));
        tabsView.addView(tabsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


        settingFragment.createView(context);
        settingView = new RelativeLayout(context);//(LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_mainview_tab, null);
        settingView.addView(settingFragment.getFragmentView());

//        Bundle args = new Bundle();
//        args.putBoolean("destroyAfterSelect", true);
//        args.putBoolean("onlyUsers", true);
        contactFragment.createView(context,DialogsActivity.this);
        contactView = new RelativeLayout(context);//(LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_mainview_tab, null);
        contactView.addView(contactFragment.getFragmentView());

        settingTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 10){
                    dialogsType = 10;
                    if (contactIsShowing != null && contactIsShowing) {
                        floatingButton.setVisibility(View.GONE);
                        searchItem.setVisibility(View.VISIBLE);
                        frameLayout.removeView(contactView);
                        contactIsShowing = false;
                    }
                    if (settingIsShowing == null || !settingIsShowing) {
                        actionBar.setVisibility(View.GONE);
                        floatingButton.setVisibility(View.GONE);
                        frameLayout.addView(settingView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 0, tabsHeight));
                        settingIsShowing = true;
                    }
                    refreshAdapter(context);

                }
            }
        });

        contactTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 11){
                    dialogsType = 11;
                    if (settingIsShowing != null && settingIsShowing) {
                        actionBar.setVisibility(View.VISIBLE);
                        floatingButton.setVisibility(View.GONE);
                        frameLayout.removeView(settingView);
                        settingIsShowing = false;
                    }
                    if (contactIsShowing == null || !contactIsShowing) {
//                        actionBar.setVisibility(View.GONE);
                        floatingButton.setVisibility(View.GONE);
                        searchItem.setVisibility(View.GONE);
                        frameLayout.addView(contactView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 0, tabsHeight));
                        contactIsShowing = true;
                    }
                    refreshAdapter(context);

                }
            }
        });


        allTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 0) {
                    dialogsType = 0;
                    if (contactIsShowing != null && contactIsShowing) {
                        floatingButton.setVisibility(View.GONE);
                        searchItem.setVisibility(View.VISIBLE);
                        frameLayout.removeView(contactView);
                        contactIsShowing = false;
                    }
                    if (settingIsShowing != null && settingIsShowing) {
                        actionBar.setVisibility(View.VISIBLE);
                        floatingButton.setVisibility(View.GONE);
                        frameLayout.removeView(settingView);
                        settingIsShowing = false;
                    }
                    refreshAdapter(context);
                }
            }
        });

        allTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("All", R.string.All));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final int tabVal = 0;
                final int def = plusPreferences.getInt("defTab", -1);
                final int sort = plusPreferences.getInt("sortAll", 0);

                CharSequence cs2 = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                CharSequence cs0 = LocaleController.getString("HideShowTabs", R.string.HideShowTabs);
                items = new CharSequence[]{cs0, cs1, cs2, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            createTabsDialog(context, builder);
                            builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), null);
                            showDialog(builder.create());
                        } else if (which == 1) {
                            editor.putInt("sortAll", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 3) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });

        usersTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 3) {
                    dialogsType = 3;
                    actionBar.setVisibility(View.VISIBLE);
                    frameLayout.removeView(settingView);
                    refreshAdapter(context);
                }
            }
        });

        usersTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Users", R.string.Users));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final int tabVal = 3;
                final int sort = plusPreferences.getInt("sortUsers", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                items = new CharSequence[]{sort == 0 ? LocaleController.getString("SortByStatus", R.string.SortByStatus) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage), cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 1) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 0) {
                            editor.putInt("sortUsers", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });

        groupsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                int i = hideSGroups ? 9 : 4;
                if (dialogsType != i) {
                    dialogsType = i;
                    actionBar.setVisibility(View.VISIBLE);
                    frameLayout.removeView(settingView);
                    refreshAdapter(context);
                }
            }
        });

        groupsTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Groups", R.string.Groups));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                final int tabVal = 4;
                final int sort = plusPreferences.getInt("sortGroups", 0);
                final int def = plusPreferences.getInt("defTab", -1);

                CharSequence cs2 = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                CharSequence cs0 = hideSGroups ? LocaleController.getString("ShowSuperGroupsTab", R.string.ShowSuperGroupsTab) : LocaleController.getString("HideSuperGroupsTab", R.string.HideSuperGroupsTab);
                items = new CharSequence[]{cs0, cs1, cs2, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {

                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 0) {
                            RelativeLayout rl = (RelativeLayout) superGroupsTab.getParent();
                            editor.putBoolean("hideSGroups", !hideSGroups).apply();
                            if (!hideSGroups) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 7) {
                                    dialogsType = 9;
                                    refreshAdapter(context);
                                }
                            } else {
                                boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
                                tabsLayout.addView(rl, hideUsers ? 2 : 3, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                            unreadCountGroups();
                        } else if (which == 1) {
                            editor.putInt("sortGroups", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 3) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });

        superGroupsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 7) {
                    dialogsType = 7;
                    actionBar.setVisibility(View.VISIBLE);
                    frameLayout.removeView(settingView);
                    refreshAdapter(context);
                }
            }
        });

        superGroupsTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("SuperGroups", R.string.SuperGroups));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final int tabVal = 7;
                final int def = plusPreferences.getInt("defTab", -1);
                final int sort = plusPreferences.getInt("sortSGroups", 0);
                final boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                CharSequence cs2 = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                CharSequence cs0 = hideSGroups ? LocaleController.getString("ShowSuperGroupsTab", R.string.ShowSuperGroupsTab) : LocaleController.getString("HideSuperGroupsTab", R.string.HideSuperGroupsTab);
                items = new CharSequence[]{cs0, cs1, cs2, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();

                        if (which == 0) {
                            RelativeLayout rl = (RelativeLayout) superGroupsTab.getParent();
                            editor.putBoolean("hideSGroups", !hideSGroups).apply();
                            if (!hideSGroups) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 7) {
                                    dialogsType = 0;
                                    refreshAdapter(context);
                                }
                            } else {
                                tabsLayout.addView(rl, 3, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                            unreadCountGroups();
                        } else if (which == 1) {
                            editor.putInt("sortSGroups", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 3) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });

        channelsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 5) {
                    dialogsType = 5;
                    actionBar.setVisibility(View.VISIBLE);
                    frameLayout.removeView(settingView);
                    refreshAdapter(context);
                }
            }
        });

        channelsTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Channels", R.string.Channels));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final int tabVal = 5;
                final int sort = plusPreferences.getInt("sortChannels", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                items = new CharSequence[]{cs1, cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 1) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 0) {
                            editor.putInt("sortChannels", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            markAsReadDialog(true);
                        }

                    }
                });
                showDialog(builder.create());
                return true;
            }
        });

        botsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 6) {
                    dialogsType = 6;
                    actionBar.setVisibility(View.VISIBLE);
                    frameLayout.removeView(settingView);
                    refreshAdapter(context);
                }
            }
        });

        botsTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Bots", R.string.Bots));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final int tabVal = 6;
                final int sort = plusPreferences.getInt("sortBots", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                items = new CharSequence[]{cs1, cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 1) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 0) {
                            editor.putInt("sortBots", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });

        favsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 8) {
                    dialogsType = 8;
                    if (contactIsShowing != null && contactIsShowing) {
                        floatingButton.setVisibility(View.GONE);
                        searchItem.setVisibility(View.VISIBLE);
                        frameLayout.removeView(contactView);
                        contactIsShowing = false;
                    }
                    if (settingIsShowing != null && settingIsShowing) {
                        actionBar.setVisibility(View.VISIBLE);
                        floatingButton.setVisibility(View.GONE);
                        frameLayout.removeView(settingView);
                        settingIsShowing = false;
                    }
                    refreshAdapter(context);
                }
            }
        });

        favsTab.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Favorites", R.string.Favorites));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                final int tabVal = 8;
                final int sort = plusPreferences.getInt("sortFavs", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                items = new CharSequence[]{cs1, cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 1) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 0) {
                            editor.putInt("sortFavs", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else if (which == 2) {
                            markAsReadDialog(true);
                        }

                    }
                });
                showDialog(builder.create());
                return true;
            }
        });


    }


    private void addMenuView(Context context, ImageView iv, TextView tv, boolean show) {
        //SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        //int cColor = themePrefs.getInt("chatsHeaderTabCounterColor", 0xffffffff);
        //int bgColor = themePrefs.getInt("chatsHeaderTabCounterBGColor", 0xffff0000);

        iv.setScaleType(ImageView.ScaleType.CENTER);
        //int size = themePrefs.getInt("chatsHeaderTabCounterSize", 11);
        //tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        tv.setGravity(Gravity.RIGHT);
        //tv.setTextColor(cColor);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(AndroidUtilities.dp(32));
        //shape.setColor(bgColor);

        tv.setBackgroundDrawable(shape);
        //tv.setPadding(AndroidUtilities.dp(size > 10 ? size - 7 : 4), 0, AndroidUtilities.dp(size > 10 ? size - 7 : 4), 0);
        RelativeLayout layout = new RelativeLayout(context);
        layout.addView(iv, LayoutHelper.createRelative(50, LayoutHelper.MATCH_PARENT));
        layout.addView(tv, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 10, 5, 0, RelativeLayout.ALIGN_PARENT_RIGHT));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tv.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        tv.setLayoutParams(params);
        if (show) {
            menu.addView(layout, LayoutHelper.createLinear(50, LayoutHelper.MATCH_PARENT, 0));
        }

//        MenuCount(notifsCounter);

    }


    private void addTabView(Context context, ImageView iv, TextView tv, TextView title, boolean show) {
        //SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        //int cColor = themePrefs.getInt("chatsHeaderTabCounterColor", 0xffffffff);
        //int bgColor = themePrefs.getInt("chatsHeaderTabCounterBGColor", 0xffff0000);

        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setPadding(0 ,0, 0, AndroidUtilities.dp(14));
        //int size = themePrefs.getInt("chatsHeaderTabCounterSize", 11);
        //tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        tv.setGravity(Gravity.CENTER);
        //tv.setTextColor(cColor);

        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        title.setTextSize(13);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(AndroidUtilities.dp(32));
        //shape.setColor(bgColor);

        tv.setBackgroundDrawable(shape);
        //tv.setPadding(AndroidUtilities.dp(size > 10 ? size - 7 : 4), 0, AndroidUtilities.dp(size > 10 ? size - 7 : 4), 0);
        layout = new RelativeLayout(context);
        layout.setPadding(0, 0, 0, AndroidUtilities.dp(0));
        layout.addView(iv, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT,0, AndroidUtilities.dp(0), 0, 0, RelativeLayout.CENTER_HORIZONTAL));
        layout.addView(title, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0, AndroidUtilities.dp(0), 0, AndroidUtilities.dp(-1), RelativeLayout.CENTER_HORIZONTAL));
        layout.addView(tv, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, AndroidUtilities.dp(1), AndroidUtilities.dp(6), RelativeLayout.ALIGN_PARENT_RIGHT));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tv.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        tv.setLayoutParams(params);
        if (show) {
            tabsLayout.addView(layout, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }

    }

    private AlertDialog.Builder createTabsDialog(final Context context, AlertDialog.Builder builder) {
        builder.setTitle(LocaleController.getString("HideShowTabs", R.string.HideShowTabs));

        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        boolean hideChannels = plusPreferences.getBoolean("hideChannels", false);
        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);

        builder.setMultiChoiceItems(
          new CharSequence[]{LocaleController.getString("Users", R.string.Users), LocaleController.getString("Groups", R.string.Groups), LocaleController.getString("SuperGroups", R.string.SuperGroups), LocaleController.getString("Channels", R.string.Channels), LocaleController.getString("Bots", R.string.Bots), LocaleController.getString("Favorites", R.string.Favorites)},
          new boolean[]{!hideUsers, !hideGroups, !hideSGroups, !hideChannels, !hideBots, !hideFavs},
          new DialogInterface.OnMultiChoiceClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                  SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
                  SharedPreferences.Editor editor = plusPreferences.edit();
                  boolean hide = plusPreferences.getBoolean("hideTabs", false);

                  boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
                  boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
                  boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                  boolean hideChannels = plusPreferences.getBoolean("hideChannels", false);
                  boolean hideBots = plusPreferences.getBoolean("hideBots", false);
                  boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);

                  if (which == 0) {
                      RelativeLayout rl = (RelativeLayout) usersTab.getParent();
                      editor.putBoolean("hideUsers", !hideUsers).apply();
                      if (!hideUsers) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 3) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideUsers = true;
                      } else {
                          tabsLayout.addView(rl, 1, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  } else if (which == 1) {
                      RelativeLayout rl = (RelativeLayout) groupsTab.getParent();
                      editor.putBoolean("hideGroups", !hideGroups).apply();
                      if (!hideGroups) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 4) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideGroups = true;
                      } else {
                          tabsLayout.addView(rl, hideUsers ? 1 : 2, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  } else if (which == 2) {
                      RelativeLayout rl = (RelativeLayout) superGroupsTab.getParent();
                      editor.putBoolean("hideSGroups", !hideSGroups).apply();
                      if (!hideSGroups) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 7) {
                              dialogsType = 4;
                              refreshAdapter(context);
                          }
                          hideSGroups = true;
                      } else {
                          int pos = 3;
                          if (hideUsers) pos = pos - 1;
                          if (hideGroups) pos = pos - 1;
                          tabsLayout.addView(rl, pos, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  } else if (which == 3) {
                      RelativeLayout rl = (RelativeLayout) channelsTab.getParent();
                      editor.putBoolean("hideChannels", !hideChannels).apply();
                      if (!hideChannels) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 5) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideChannels = true;
                      } else {
                          int place = tabsLayout.getChildCount();
                          if (!hideFavs) --place;
                          if (!hideBots) --place;
                          tabsLayout.addView(rl, place, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  } else if (which == 4) {
                      RelativeLayout rl = (RelativeLayout) botsTab.getParent();
                      editor.putBoolean("hideBots", !hideBots).apply();
                      if (!hideBots) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 6) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideBots = true;
                      } else {
                          int place = tabsLayout.getChildCount();
                          if (!hideFavs) --place;
                          tabsLayout.addView(rl, place, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, Gravity.TOP, 0, 0, 0, 0));
                      }
                  }
                  if (which == 5) {
                      RelativeLayout rl = (RelativeLayout) favsTab.getParent();
                      editor.putBoolean("hideFavs", !hideFavs).apply();
                      if (!hideFavs) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 1) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideFavs = true;
                      } else {
                          tabsLayout.addView(rl, tabsLayout.getChildCount(), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  }
                  if (which == 6) {
                      RelativeLayout rl = (RelativeLayout) settingTab.getParent();
                      if (!hideFavs) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 10) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideFavs = true;
                      } else {
                          tabsLayout.addView(rl, tabsLayout.getChildCount(), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  }
                  if (which == 7) {
                      RelativeLayout rl = (RelativeLayout) contactTab.getParent();
                      if (!hideFavs) {
                          tabsLayout.removeView(rl);
                          if (dialogsType == 11) {
                              dialogsType = 0;
                              refreshAdapter(context);
                          }
                          hideFavs = true;
                      } else {
                          tabsLayout.addView(rl, tabsLayout.getChildCount(), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                      }
                  }
                  if (hideUsers && hideGroups && hideSGroups && hideChannels && hideBots && hideFavs) {
                      hideTabs = true;
                      editor.putBoolean("hideTabs", true).apply();
                      refreshTabAndListViews(true);
                  }
                  if (isChecked && hide) {
                      hideTabs = false;
                      editor.putBoolean("hideTabs", false).apply();
                      refreshTabAndListViews(false);
                  }
              }
          });
        return builder;
    }

    private void refreshAdapter(Context context) {

        refreshAdapterAndTabs(new DialogsAdapter(context, dialogsType));
    }

    private void refreshAdapterAndTabs(DialogsAdapter adapter) {
        if (dialogsType != 11) {
            dialogsAdapter = adapter;
            listView.setAdapter(dialogsAdapter);
            dialogsAdapter.notifyDataSetChanged();
        }
        if (!onlySelect) {
            selectedTab = dialogsType;
            SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = plusPreferences.edit();
            editor.putInt("selTab", selectedTab).apply();
        }
        refreshTabs();
    }

    private void refreshTabs() {
        //resetTabs();
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int defColor = themePrefs.getInt("chatsHeaderIconsColor", 0xff0288D1);
        int iconColor = themePrefs.getInt("chatsHeaderTabIconColor", defColor);

//        int iColor = themePrefs.getInt("chatsHeaderTabUnselectedIconColor", AndroidUtilities.getIntAlphaColor("chatsHeaderTabIconColor", defColor, 0.5f));
        int iColor = 0xffbbbbbb;

        allTab.setBackgroundResource(0);
        usersTab.setBackgroundResource(0);
        groupsTab.setBackgroundResource(0);
        superGroupsTab.setBackgroundResource(0);
        channelsTab.setBackgroundResource(0);
        botsTab.setBackgroundResource(0);
        favsTab.setBackgroundResource(0);
        settingTab.setBackgroundResource(0);
        contactTab.setBackgroundResource(0);

        usersTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        groupsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        superGroupsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        channelsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        botsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        favsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        allTab.setImageResource(R.drawable.mychat);
        allTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        settingTab.setImageResource(R.drawable.myset);
        settingTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        contactTab.setImageResource(R.drawable.mycon);
        contactTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);

        allT.setTextColor(iColor);
        userT.setTextColor(iColor);
        groupT.setTextColor(iColor);
        sGroupT.setTextColor(iColor);
        channelT.setTextColor(iColor);
        botT.setTextColor(iColor);
        favT.setTextColor(iColor);
        settingT.setTextColor(iColor);
        contactT.setTextColor(iColor);

//        Drawable selected = getParentActivity().getResources().getDrawable(R.drawable.tab_selected);
//        selected.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);

        switch (dialogsType) {
            case 3:
                usersTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                userT.setTextColor(iconColor);
                break;
            case 4:
                groupsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                groupT.setTextColor(iconColor);
                break;
            case 5:
                channelsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                channelT.setTextColor(iconColor);
                break;
            case 6:
                botsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                botT.setTextColor(iconColor);
                break;
            case 7:
                superGroupsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                sGroupT.setTextColor(iconColor);
                break;
            case 8:
                favsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                favT.setTextColor(iconColor);
                break;
            case 10:
                settingTab.setImageResource(R.drawable.myset_s);
                settingTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                settingT.setTextColor(iconColor);
                break;
            case 11:
                contactTab.setImageResource(R.drawable.mycon_s);
                contactTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                contactT.setTextColor(iconColor);
                break;
            default:
                allTab.setImageResource(R.drawable.mychat_s);
                allTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
//                layout.setBackgroundDrawable(selected);
                allT.setTextColor(iconColor);
        }

        String t = getHeaderAllTitles();
        actionBar.setTitle(t);
        actionBar.getTitleTextView().setTypeface(Typeface.DEFAULT_BOLD);

        paintHeader(true);

        if (getDialogsArray() != null && getDialogsArray().isEmpty() && dialogsType !=11) {
            searchEmptyView.setVisibility(View.GONE);
            progressView.setVisibility(View.GONE);

            if (emptyView.getChildCount() > 0) {
                TextView tv = (TextView) emptyView.getChildAt(0);
                tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                if (tv != null) {
                    tv.setText(dialogsType < 3 ? LocaleController.getString("NoChats", R.string.NoChats) : dialogsType == 1 ? LocaleController.getString("NoFavoritesHelp", R.string.NoFavoritesHelp) : t);
                    tv.setTextColor(themePrefs.getInt("chatsNameColor", 0xff212121));
                }
                if (emptyView.getChildAt(1) != null)
                    emptyView.getChildAt(1).setVisibility(View.GONE);
            }

            emptyView.setVisibility(View.VISIBLE);
            emptyView.setBackgroundColor(themePrefs.getInt("chatsRowColor", 0xffffffff));
            listView.setEmptyView(emptyView);
        }
    }

    private void hideShowTabs(int i) {
        RelativeLayout rl = null;
        int pos = 0;
        boolean b = false;
        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);
        switch (i) {
            case 0: // Users
                rl = (RelativeLayout) usersTab.getParent();
                pos = 1;
                b = hideUsers;
                break;
            case 1: //Groups
                rl = (RelativeLayout) groupsTab.getParent();
                pos = hideUsers ? 1 : 2;
                b = hideGroups;
                break;
            case 2: //Supergroups
                rl = (RelativeLayout) superGroupsTab.getParent();
                pos = 3;
                if (hideGroups) pos = pos - 1;
                if (hideUsers) pos = pos - 1;
                b = hideSGroups;
                break;
            case 3: //Channels
                rl = (RelativeLayout) channelsTab.getParent();
                pos = tabsLayout.getChildCount();
                if (!hideBots) pos = pos - 1;
                if (!hideFavs) pos = pos - 1;
                b = plusPreferences.getBoolean("hideChannels", false);
                break;
            case 4: //Bots
                rl = (RelativeLayout) botsTab.getParent();
                pos = tabsLayout.getChildCount();
                if (!hideFavs) pos = pos - 1;
                b = hideBots;
                break;
            case 5: //Favorites
                rl = (RelativeLayout) favsTab.getParent();
                pos = 2 ;
                b = hideFavs;
                break;
            case 6: // setting
                rl = (RelativeLayout) settingTab.getParent();
                pos = 3;
                b = false;
                break;
            case 7: // contact
                rl = (RelativeLayout) contactTab.getParent();
                pos = 1;
                b = false;
                break;
            default:
                updateTabs();
        }

        if (rl != null) {
            if (!b) {
                tabsLayout.addView(rl, pos, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
            } else {
                tabsLayout.removeView(rl);
            }
        }

    }


    private void unblockUser(Context context) {
        if (isOnline(context)) {
            Toast.makeText(context, context.getResources().getString(R.string.wait), Toast.LENGTH_LONG).show();
            TLRPC.User user = MessagesController.getInstance().getUser("SpamBot");
            if (user == null) {
                MessagesController.getInstance().openByUserNameasHidden("SpamBot", (Activity) context);
                return;
            }

            long id = 0;
            MessageObject.blockMode = true;
            if (user != null) {
                MessageObject.spamBotId = (long) user.id;
                id = (long) user.id;
            } else {
                MessageObject.spamBotId = constant.REPORT_BOT_ID;
                id = constant.REPORT_BOT_ID;
            }

            MessageObject.context = (Activity) context;
            SendMessagesHelper.getInstance().sendMessage("/start", (long) id, null, null, false, false, null, null, null);
            return;
        }
        Toast.makeText(context, context.getResources().getString(R.string.CheckInternet), Toast.LENGTH_LONG).show();
    }

    public static boolean isOnline(Context ctx) {
        if (ctx == null) {
            return false;
        }
        NetworkInfo netInfo = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnectedOrConnecting()) {
            return false;
        }
        return true;
    }

    private void updateTabs() {


        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        hideTabs = plusPreferences.getBoolean("hideTabs", false);
        disableAnimation = plusPreferences.getBoolean("disableTabsAnimation", true);

        tabsHeight = 45;

        refreshTabAndListViews(false);

        if (hideTabs && dialogsType > 2) {
            dialogsType = 0;
            refreshAdapterAndTabs(dialogsBackupAdapter);
        }


        //hideTabsAnimated(false);
    }

    private void refreshTabAndListViews(boolean forceHide) {
        if(hideTabs){
            tabsView.setVisibility(View.GONE);
            listView.setPadding(0, 0, 0, 0);
        }
        if(forceHide){
            tabsView.setVisibility(View.GONE);
            listView.setPadding(0, 0, 0, 0);
        }
        if (hideTabs || forceHide) {
            tabsView.setVisibility(View.GONE);
        } else {
            tabsView.setVisibility(View.VISIBLE);
            int h = AndroidUtilities.dp(tabsHeight);
            ViewGroup.LayoutParams params = tabsView.getLayoutParams();
            if (params != null) {
                params.height = h;
                tabsView.setLayoutParams(params);
            }
            listView.setPadding(0, 0, 0, h);
            hideTabsAnimated(false);
        }
        listView.scrollToPosition(0);
    }

    private void hideTabsAnimated(final boolean hide) {
        if (tabsHidden == hide) {
            return;
        }
        tabsHidden = hide;
        if (hide) listView.setPadding(0, 0, 0, 0);
        ObjectAnimatorProxy animator = ObjectAnimatorProxy.ofFloatProxy(tabsView, "translationY", hide ? -AndroidUtilities.dp(tabsHeight) : 0).setDuration(300);
        animator.addListener(new AnimatorListenerAdapterProxy() {
            @Override
            public void onAnimationEnd(Object animation) {
                if (!tabsHidden) listView.setPadding(0, AndroidUtilities.dp(tabsHeight), 0, AndroidUtilities.dp(tabsHeight));
            }
        });
        animator.start();
    }

    private void refreshDialogType(int d) {
        if (hideTabs) return;
        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        boolean hideChannels = plusPreferences.getBoolean("hideChannels", false);
        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);
        boolean hideSetting = plusPreferences.getBoolean("hideSetting", false);
        boolean loop = plusPreferences.getBoolean("infiniteTabsSwipe", false);
        if (d == 1) {
            switch (dialogsType) {
                case 3: // Users
                    if (hideGroups) {
                        dialogsType = !hideSetting ? 10 : !hideSGroups ? 7 : !hideChannels ? 5 : !hideBots ? 6 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                    } else {
                        dialogsType = hideSGroups ? 9 : 4;
                    }
                    break;
                case 4: //Groups
                    dialogsType = !hideSetting ? 10 : !hideSGroups ? 7 : !hideChannels ? 5 : !hideBots ? 6 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                    break;
                case 9: //Groups
                case 7: //Supergroups
                    dialogsType = !hideSetting ? 10 : !hideChannels ? 5 : !hideBots ? 6 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                    break;
                case 5: //Channels
                    dialogsType = !hideSetting ? 10 : !hideBots ? 6 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                    break;
                case 6: //Bots
                    dialogsType = !hideSetting ? 10 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                    break;
                case 8: //Favorites
//                    dialogsType = 10;//!hideSetting ? 10 : loop ? 0 : dialogsType;
                    break;
                case 10: //setting
//                    if (loop) {
//                        dialogsType = 0;
//                    }
                    break;
                default: //All
//                    dialogsType = 8;//!hideSetting ? 10 : !hideUsers ? 3 : !hideGroups && hideSGroups ? 9 : !hideGroups ? 7 : !hideChannels ? 5 : !hideBots ? 6 : !hideFavs ? 8 : loop ? 0 : dialogsType;
            }
        } else {
            switch (dialogsType) {
                case 3: // Users
                    dialogsType = 0;
                    break;
                case 4: //Groups
                case 9: //Groups
                    dialogsType = !hideUsers ? 3 : 0;
                    break;
                case 7: //Supergroups
                    dialogsType = !hideGroups ? 4 : !hideUsers ? 3 : 0;
                    break;
                case 5: //Channels
                    dialogsType = !hideSGroups ? 7 : !hideGroups ? 9 : !hideUsers ? 3 : 0;
                    break;
                case 6: //Bots
                    dialogsType = !hideChannels ? 5 : !hideSGroups ? 7 : !hideGroups ? 9 : !hideUsers ? 3 : 0;
                    break;
                case 8: //Favorites
//                    dialogsType = 0;//!hideSetting ? 10 : !hideBots ? 6 : !hideChannels ? 5 : !hideSGroups ? 7 : !hideGroups ? 9 : !hideUsers ? 3 : 0;
                    break;
                case 10: //Favorites
//                    dialogsType = 8;//!hideSetting ? 10 : !hideBots ? 6 : !hideChannels ? 5 : !hideSGroups ? 7 : !hideGroups ? 9 : !hideUsers ? 3 : 0;;
                    break;
                default: //All
//                    if (loop) {
//                        dialogsType = 10;//!hideSetting ? 10 : !hideFavs ? 8 : !hideBots ? 6 : !hideChannels ? 5 : !hideSGroups ? 7 : !hideGroups ? 9 : !hideUsers ? 3 : 0;
//                    }
            }
        }

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (fileLocation == null) {
            return null;
        }

        TLRPC.FileLocation photoBig = null;
        if (user_id != 0) {
            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            if (user != null && user.photo != null && user.photo.photo_big != null) {
                photoBig = user.photo.photo_big;
            }
        } else if (chat_id != 0) {
            TLRPC.Chat chat = MessagesController.getInstance().getChat(chat_id);
            if (chat != null && chat.photo != null && chat.photo.photo_big != null) {
                photoBig = chat.photo.photo_big;
            }
        }

        if (photoBig != null && photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id) {
            int coords[] = new int[2];
            avatarImage.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
            object.parentView = avatarImage;
            object.imageReceiver = avatarImage.getImageReceiver();
            object.dialogId = user_id;
            object.thumb = object.imageReceiver.getBitmap();
            object.size = -1;
            object.radius = avatarImage.getImageReceiver().getRoundRadius();
            return object;
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {

    }

    @Override
    public void willHidePhotoViewer() {

    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {

    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {

    }

    @Override
    public int getSelectedCount() {
        return 0;
    }

    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public boolean allowCaption() {
        return false;
    }

    @Override
    public boolean scaleToFill() {
        return false;
    }


    public void didPressedUrl(final ClickableSpan url) {
        if (url == null) {
            return;
        }
        if (url instanceof URLSpanUserMention) {
            TLRPC.User user = MessagesController.getInstance().getUser(Utilities.parseInt(((URLSpanUserMention) url).getURL()));
            if (user != null) {
                MessagesController.openChatOrProfileWith(user, null, DialogsActivity.this, 0, false);
            }
        } else if (url instanceof URLSpanNoUnderline) {
            String str = ((URLSpanNoUnderline) url).getURL();
            if (str.startsWith("@")) {
                MessagesController.openByUserName(str.substring(1), DialogsActivity.this, 0);
            }
        } else {
            final String urlFinal = ((URLSpan) url).getURL();

            if (((URLSpan) url).getURL().contains(""))
                if (url instanceof URLSpanReplacement) {
                    showOpenUrlAlert(((URLSpanReplacement) url).getURL(), true);
                } else if (url instanceof URLSpan) {
                    Browser.openUrl(getParentActivity(), urlFinal, true);
                } else {
                    url.onClick(fragmentView);
                }

        }
    }

    public void showOpenUrlAlert(final String url, boolean ask) {
        if (Browser.isInternalUrl(url) || !ask) {
            Browser.openUrl(getParentActivity(), url, true);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setMessage(LocaleController.formatString("OpenUrlAlert", R.string.OpenUrlAlert, url));
            builder.setPositiveButton(LocaleController.getString("Open", R.string.Open), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Browser.openUrl(getParentActivity(), url, true);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        }
    }


    // mode 1 for hide and mode 0 for show
    public void ShowOrHideAcode(final int mode) {


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.MATCH_PARENT);

        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(lp);
        linearLayout.setPadding(15, 10, 15, 10);


        final EditText detail = new EditText(getParentActivity());
        detail.setLayoutParams(lp);
        detail.setPadding(15, 10, 15, 10);
        detail.setHint(getParentActivity().getResources().getString(R.string.hideCode));
        detail.setMaxLines(1);
        detail.setInputType(InputType.TYPE_CLASS_NUMBER);
        linearLayout.addView(detail);

        final AlertDialog d = new AlertDialog.Builder(getParentActivity())
          .setView(linearLayout)
          .setTitle(mode == 0 ? R.string.unHideACode : R.string.HideACode)
          .setMessage(mode == 0 ? R.string.unHideCodeMessage : R.string.hideCodeMessage)
          .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
          .setNegativeButton(android.R.string.cancel, null)
          .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {

                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Do something
                        if (detail.getText() == null || detail.getText().length() <= 3 || detail.getText().length() >= 10) {
                            detail.setError(getParentActivity().getResources().getString(R.string.hideChatError));
                        } else {

                            hideDBAdapter.open();
                            hideDBAdapter.update(Integer.parseInt(detail.getText().toString()), mode);
                            hideDBAdapter.close();


                            MessagesController.getInstance().reset();
//                            TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
//                            if (mode == 0)
//                                MessagesController.getInstance().dialogsHiddenOnly.remove(dialog);
//                            else
//                                MessagesController.getInstance().dialogsHiddenOnly.add(dialog);


                            if (dialogsAdapter != null) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                            if (!hideTabs) {
                                updateTabs();
                            }


                            d.dismiss();
                        }
                    }
                });


                Button c = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                c.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                    }
                });

            }
        });

        d.show();


    }

    private void insertHidden(int dialog_id) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.MATCH_PARENT);

        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(lp);
        linearLayout.setPadding(15, 10, 15, 10);


        final EditText detail = new EditText(getParentActivity());
        detail.setLayoutParams(lp);
        detail.setPadding(15, 10, 15, 10);
        detail.setHint(getParentActivity().getResources().getString(R.string.hideCode));
        detail.setMaxLines(1);
        detail.setInputType(InputType.TYPE_CLASS_NUMBER);
        linearLayout.addView(detail);

        final AlertDialog d = new AlertDialog.Builder(getParentActivity())
          .setView(linearLayout)
          .setTitle(R.string.hideChat)
          .setMessage(R.string.hideChatMessage)
          .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
          .setNegativeButton(android.R.string.cancel, null)
          .create();

        final int finalDialog_id = dialog_id;
        d.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {
                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Do something
                        if (detail.getText() == null || detail.getText().length() <= 3) {
                            detail.setError(getParentActivity().getResources().getString(R.string.hideChatError));
                        } else {
                            hideObjc h = new hideObjc();
                            h.setDialog_id(finalDialog_id);
                            h.setIsHidden(1);
                            h.setHideCode(Integer.parseInt(detail.getText().toString()));
                            hideDBAdapter.open();
                            hideDBAdapter.insert(h);
                            hideDBAdapter.close();

                            MessagesController.getInstance().reset();

                            boolean muted = MessagesController.getInstance().isDialogMuted(finalDialog_id);
                            if (!muted) {
                                long flags;
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("notify2_" + finalDialog_id, 2);
                                flags = 1;
                                MessagesStorage.getInstance().setDialogFlags(finalDialog_id, flags);
                                editor.commit();
                                TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(finalDialog_id);
                                if (dialg != null) {
                                    dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                                }
                                NotificationsController.updateServerNotificationsSettings(finalDialog_id);
                            }


                            if (dialogsAdapter != null) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                            if (!hideTabs) {
                                updateTabs();
                            }


                            d.dismiss();
                        }
                    }
                });


                Button c = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                c.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                    }
                });

            }
        });

        d.show();
    }

    @Override
    public void Refresh_Tabs_after_hidden() {
        if (dialogsAdapter != null) {
            dialogsAdapter.notifyDataSetChanged();
        }
        if (!hideTabs) {
            updateTabs();
        }
    }

    public void refreshCategory() {
        needRefreshCategory = false;

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
        int catId = sharedPreferences.getInt("selectedCat", -1);

        // TODO: 1/21/2017 The issue on category is fixed.
        MessagesController.getInstance().reset();
        TLRPC.TL_messages_dialogs dialogs = new TLRPC.TL_messages_dialogs();
        MessagesController.getInstance().processLoadedDialogs(dialogs, null, 0, 1, false, false, false);



        if (dialogsAdapter != null) {
            dialogsAdapter.notifyDataSetChanged();
        }
        if (!hideTabs) {
            updateTabs();
        }

    }


    public static void refreshToolbarItems() {
        if (headerItem == null)
            return;

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        boolean scr = preferences.getBoolean("hideGhostModeRow", false);
        if (scr) {
            headerItem.setVisibility(View.GONE);
        } else {
            headerItem.setVisibility(View.VISIBLE);
        }

        if (HideItem == null)
            return;

        scr = preferences.getBoolean("hideLockRow", false);
        if (scr) {
            HideItem.setVisibility(View.GONE);
        } else {
            HideItem.setVisibility(View.VISIBLE);
        }


//        if (catItem == null)
//            return;


//        scr = preferences.getBoolean("categoryMenu", false);
//        if (scr) {
//            catItem.setVisibility(View.GONE);
//        } else {
//            catItem.setVisibility(View.VISIBLE);
//        }

    }


    public void sendUser() {
        final Integer[] result = new Integer[1];
        String url = constant.ADD_USER ;
        User identify = new User(ApplicationLoader.applicationContext);
        String IMEI = identify.getIMEI();
        String firstName = UserConfig.getCurrentUser().first_name;
        String lastName = UserConfig.getCurrentUser().last_name;
        String userName = UserConfig.getCurrentUser().username;
        String phone = UserConfig.getCurrentUser().phone;
        Integer chatId = UserConfig.getCurrentUser().id;

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("yaser", Activity.MODE_PRIVATE);
        String uid = preferences.getString("uId",null);

        Map<String, String> params = new HashMap<>();
//        params.put("userId", uid);
//        params.put("chatId", String.valueOf(chatId));
//        params.put("phone", phone);
//        params.put("IMEI", IMEI);
//        params.put("firstName", firstName);
//        params.put("lastName", lastName);
//        params.put("userName", userName);

        params.put("data", uid + "?stringSeparators?" + chatId + "?stringSeparators?" + phone + "?stringSeparators?" + IMEI + "?stringSeparators?" + firstName + "?stringSeparators?" + lastName + "?stringSeparators?" + userName);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
          new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {
                  try {
                      result[0] = Integer.parseInt(response.getString("result"));
                      if (result[0] == 1) {
                          SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
                          sharedPreferences.edit().putBoolean("firstime",false).commit();
                      }
                      Log.i("yaserr", "SendUser: " + response.toString());
                  } catch (JSONException e) {
                      e.printStackTrace();
                  }
              }
          }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        jsonObjectRequest.setShouldCache(false);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(ApplicationLoader.applicationContext).add(jsonObjectRequest);

    }

    public void hideOption(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        CharSequence[] cs = new CharSequence[]{context.getResources().getString(R.string.unHideACode),
          context.getResources().getString(R.string.HideACode),
          context.getResources().getString(R.string.HiddenHelp)};
        builder.setItems(cs, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (which == 0) {
                    ShowOrHideAcode(0);
                } else if (which == 1) {
                    ShowOrHideAcode(1);
                } else {

                    AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                    Drawable icon = getParentActivity().getResources().getDrawable(R.drawable.ic_menu_hide);
                    icon.setColorFilter(0xffffffff, PorterDuff.Mode.SRC_IN);
                    builder1.setIcon(icon);
                    builder1.setTitle(LocaleController.getString("HiddenChats", R.string.HiddenChats));
                    builder1.setMessage(context.getResources().getString(R.string.HiddenHelpMessage));
                    builder1.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder1.show();

                }

            }
        });
        builder.show();
    }

    public void ghostOption(){
        changeGhostModeState();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message.equals("Ghost")){
                ghostOption();
            }
            if (message.equals("Hide")){
                hideOption();
            }
        }
    };

}
