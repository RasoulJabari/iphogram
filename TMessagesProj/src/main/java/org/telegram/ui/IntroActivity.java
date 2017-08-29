/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.Theme;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Utility.DbHelper;
import ir.radius.iphogram.R;
import telegramplus.CustomLanguageSelectActivity;

public class IntroActivity extends Activity {

    private static final String MY_TAG = "Alireza";
    private ViewGroup bottomPages;
    DbHelper dbase;
    private int[] icons;
    private boolean justCreated = false;
    private int lastPage = 0;
    private int[] messages;
    private boolean startPressed = false;
    long startTime;
    Timer timer;
    TimerTask_D timerTaskD;
    private int[] titles;
    private ImageView topImage1;
    private ImageView topImage2;
    private ViewPager viewPager;

    // Tasks
    class TimerTask_D extends TimerTask {
        @Override
        public void run() {
            if( !dbase.IsBusy()) {//ok
                timer.cancel();
                timer.purge();
                timer = null;
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        Log.d(MY_TAG, "DB Installed in " + (System.currentTimeMillis() - startTime) + " ms.");
                        Step_Lock();
                    }
                });
            }
        }

    }

    //PageAdapter
    private class IntroAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 7;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = View.inflate(container.getContext(), R.layout.intro_view_layout, null);
            TextView headerTextView = (TextView) view.findViewById(R.id.header_text);
            headerTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            TextView messageTextView = (TextView) view.findViewById(R.id.message_text);
            messageTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            container.addView(view, 0);

            headerTextView.setText(getString(titles[position]));
            messageTextView.setText(AndroidUtilities.replaceTags(getString(messages[position])));

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            int count = bottomPages.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = bottomPages.getChildAt(a);
                if (a == position) {
                    //child.setBackgroundColor(0xff2ca5e0);
                    child.setBackgroundColor(AndroidUtilities.defColor);
                } else {
                    child.setBackgroundColor(0xffbbbbbb);
                }
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TMessages);
        super.onCreate(savedInstanceState);
        Theme.loadResources(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (AndroidUtilities.isTablet()) {
            setContentView(R.layout.intro_layout_tablet);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.intro_layout);
        }

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("yaser", Activity.MODE_PRIVATE);
        String uid = preferences.getString("uId",null);
        if (uid == null) {
            firstInstall();
        }

        Boolean ac = preferences.getBoolean("isActive",false);
        if (uid != null && ac) {
            DeActivation();
        }

        InitObjects();
        Step_DBase(); // Start with checking DBase...

    }

    // Steps
    private void Step_DBase() {
        dbase.openDB();//ok
        if(dbase.IsBusy()) {//ok
            Log.d(MY_TAG, "DBase is Already Installed.");
            Step_Lock();
            return;
        }
        Log.d(MY_TAG, "Installing DBase...");
        startTime = System.currentTimeMillis(); // -Remove
        timerTaskD = new TimerTask_D();
        timer = new Timer();
        timer.schedule(timerTaskD, 100, 100);
    }
    private void Step_Lock() {
        startActivityForResult(new Intent(this, CustomLanguageSelectActivity.class) , 1);
    }

    public void InitObjects() {
        dbase = DbHelper.getInstance(IntroActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        start();
    }

    private void start(){

        if (LocaleController.isRTL) {
            icons = new int[]{
                    R.drawable.intro7,
                    R.drawable.intro6,
                    R.drawable.intro5,
                    R.drawable.intro4,
                    R.drawable.intro3,
                    R.drawable.intro2,
                    R.drawable.intro1
            };
            titles = new int[]{
                    R.string.Page7Title,
                    R.string.Page6Title,
                    R.string.Page5Title,
                    R.string.Page4Title,
                    R.string.Page3Title,
                    R.string.Page2Title,
                    R.string.Page1Title
            };
            messages = new int[]{
                    R.string.Page7Message,
                    R.string.Page6Message,
                    R.string.Page5Message,
                    R.string.Page4Message,
                    R.string.Page3Message,
                    R.string.Page2Message,
                    R.string.Page1Message
            };
        } else {
            icons = new int[]{
                    R.drawable.intro1,
                    R.drawable.intro2,
                    R.drawable.intro3,
                    R.drawable.intro4,
                    R.drawable.intro5,
                    R.drawable.intro6,
                    R.drawable.intro7
            };
            titles = new int[]{
                    R.string.Page1Title,
                    R.string.Page2Title,
                    R.string.Page3Title,
                    R.string.Page4Title,
                    R.string.Page5Title,
                    R.string.Page6Title,
                    R.string.Page7Title
            };
            messages = new int[]{
                    R.string.Page1Message,
                    R.string.Page2Message,
                    R.string.Page3Message,
                    R.string.Page4Message,
                    R.string.Page5Message,
                    R.string.Page6Message,
                    R.string.Page7Message
            };
        }
        viewPager = (ViewPager) findViewById(R.id.intro_view_pager);
        TextView startMessagingButton = (TextView) findViewById(R.id.start_messaging_button);
        startMessagingButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        startMessagingButton.setText(LocaleController.getString("StartMessaging", R.string.StartMessaging).toUpperCase());
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(startMessagingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[0], ObjectAnimator.ofFloat(startMessagingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            startMessagingButton.setStateListAnimator(animator);
        }
        topImage1 = (ImageView) findViewById(R.id.icon_image1);
        topImage2 = (ImageView) findViewById(R.id.icon_image2);
        bottomPages = (ViewGroup) findViewById(R.id.bottom_pages);
        topImage2.setVisibility(View.GONE);
        viewPager.setAdapter(new IntroAdapter());
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {
                if (i == ViewPager.SCROLL_STATE_IDLE || i == ViewPager.SCROLL_STATE_SETTLING) {
                    if (lastPage != viewPager.getCurrentItem()) {
                        lastPage = viewPager.getCurrentItem();

                        final ImageView fadeoutImage;
                        final ImageView fadeinImage;
                        if (topImage1.getVisibility() == View.VISIBLE) {
                            fadeoutImage = topImage1;
                            fadeinImage = topImage2;

                        } else {
                            fadeoutImage = topImage2;
                            fadeinImage = topImage1;
                        }

                        fadeinImage.bringToFront();
                        fadeinImage.setImageResource(icons[lastPage]);
                        fadeinImage.clearAnimation();
                        fadeoutImage.clearAnimation();


                        Animation outAnimation = AnimationUtils.loadAnimation(IntroActivity.this, R.anim.icon_anim_fade_out);
                        outAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                fadeoutImage.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        Animation inAnimation = AnimationUtils.loadAnimation(IntroActivity.this, R.anim.icon_anim_fade_in);
                        inAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                fadeinImage.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });


                        fadeoutImage.startAnimation(outAnimation);
                        fadeinImage.startAnimation(inAnimation);
                    }
                }
            }
        });

        startMessagingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!startPressed) {
                    startPressed = true;
                    Intent intent2 = new Intent(IntroActivity.this, LaunchActivity.class);
                    intent2.putExtra("fromIntro", true);
                    startActivity(intent2);
                    finish();
                }
            }
        });
        if (BuildVars.DEBUG_VERSION) {
            startMessagingButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ConnectionsManager.getInstance().switchBackend();
                    return true;
                }
            });
        }

        justCreated = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (justCreated) {
            if (LocaleController.isRTL) {
                viewPager.setCurrentItem(6);
                lastPage = 6;
            } else {
                viewPager.setCurrentItem(0);
                lastPage = 0;
            }
            justCreated = false;
        }
        AndroidUtilities.checkForCrashes(this);
        AndroidUtilities.checkForUpdates(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AndroidUtilities.unregisterUpdates();
    }


    public void firstInstall() {
        final Integer[] result = new Integer[1];
        final String[] uId = new String[1];
        String url = "http://iphogram.herocomco.com//UserLog/ExistApp";
        Map<String, String> params = new HashMap<>();
        params.put("ex", "true");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
          new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {
                  try {
                      result[0] = Integer.parseInt(response.getString("result"));
                      if (result[0] == 1) {
                          uId[0] = response.getString("userId");
                          SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("yaser", Activity.MODE_PRIVATE);
                          SharedPreferences.Editor editor = preferences.edit();
                          editor.putString("uId" , uId[0]);
                          editor.commit();
                      }
                      else{
                          firstInstall();
                      }
                      Log.i("yaserr", "onResponse: " + response.toString());
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
        Volley.newRequestQueue(IntroActivity.this).add(jsonObjectRequest);
    }



    public void DeActivation() {
        final Integer[] result = new Integer[1];
        String url = "http://iphogram.herocomco.com//UserLog/UserAc";

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("yaser", Activity.MODE_PRIVATE);
        String uid = preferences.getString("uId",null);

        Map<String, String> params = new HashMap<>();
        params.put("userId", uid);
        params.put("isActive", "false" );
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
          new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {
                  try {
                      result[0] = Integer.parseInt(response.getString("result"));
                      if (result[0] == 1) {
                          SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("yaser", Activity.MODE_PRIVATE);
                          SharedPreferences.Editor editor = preferences.edit();
                          editor.putBoolean("isActive" , false);
                          editor.commit();
                      }
                      else{
                          DeActivation();
                      }
                      Log.i("yaserr", "onResponseIsDeActive: " + response.toString());
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
        Volley.newRequestQueue(IntroActivity.this).add(jsonObjectRequest);

    }
}
