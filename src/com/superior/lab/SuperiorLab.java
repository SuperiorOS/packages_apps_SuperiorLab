/*
 * Copyright (C) 2022-26 SuperiorOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.superior.lab;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.LayoutPreference;

import lineageos.providers.LineageSettings;

public class SuperiorLab extends SettingsPreferenceFragment {

    private static final String KEY_DASHBOARD_PREF = "superior_lab_dashboard";

    private static final String SETTING_WINDOW_IGNORE_SECURE = "window_ignore_secure";
    private static final String SETTING_HIDE_SCREEN_CAPTURE = "hide_screen_capture_status";
    private static final String SETTING_HEADS_UP_ENABLED = "heads_up_notifications_enabled";
    private static final String SETTING_LESS_BORING_HEADS_UP = "less_boring_heads_up";
    private static final String SETTING_LOCKSCREEN_PULSE = "lockscreen_pulse_enabled";
    private static final String SETTING_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String SETTING_MEDIA_ART = "ls_media_art_enabled";

    // Bento Cards (8 Categories)
    private View mCardStatusbar;
    private View mCardNotifications;
    private View mCardLockscreen;
    private View mCardPowerMenu;
    private View mCardButtons;
    private View mCardGestures;
    private View mCardMisc;
    private View mCardTeam;

    // Live Summary Views
    private TextView mSummaryStatusbar;
    private TextView mSummaryNotifications;
    private TextView mSummaryLockscreen;
    private TextView mSummaryPowerMenu;
    private TextView mSummaryMisc;

    // 2x2 M3 Expressive Quick Toggle Tiles
    private View mToggleSecureWindow;
    private View mToggleHideCapture;
    private View mToggleHeadsUp;
    private View mTogglePulse;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.superior_lab);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LayoutPreference dashboardPref = findPreference(KEY_DASHBOARD_PREF);
        if (dashboardPref == null) {
            return;
        }

        setupCards(dashboardPref);
        setupQuickToggles(dashboardPref);
        refreshDashboardStates();
        startEntranceAnimations(dashboardPref);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboardStates();
    }

    private void setupCards(@NonNull LayoutPreference pref) {
        mCardStatusbar = pref.findViewById(R.id.card_statusbar);
        mCardNotifications = pref.findViewById(R.id.card_notifications);
        mCardLockscreen = pref.findViewById(R.id.card_lockscreen);
        mCardPowerMenu = pref.findViewById(R.id.card_powermenu);
        mCardButtons = pref.findViewById(R.id.card_buttons);
        mCardGestures = pref.findViewById(R.id.card_gestures);
        mCardMisc = pref.findViewById(R.id.card_misc);
        mCardTeam = pref.findViewById(R.id.card_team);

        mSummaryStatusbar = pref.findViewById(R.id.summary_statusbar);
        mSummaryNotifications = pref.findViewById(R.id.summary_notifications);
        mSummaryLockscreen = pref.findViewById(R.id.summary_lockscreen);
        mSummaryPowerMenu = pref.findViewById(R.id.summary_powermenu);
        mSummaryMisc = pref.findViewById(R.id.summary_misc);

        // Bind M3 Expressive spring physics and fragment routing to all 8 cards
        attachSpringTouch(mCardStatusbar, () ->
                launchFragment("com.superior.lab.fragments.StatusBarSettings", R.string.statusbar_title));

        attachSpringTouch(mCardNotifications, () ->
                launchFragment("com.superior.lab.fragments.NotificationSettings", R.string.notifications_title));

        attachSpringTouch(mCardLockscreen, () ->
                launchFragment("com.superior.lab.fragments.LockScreenSettings", R.string.lockscreen_title));

        attachSpringTouch(mCardPowerMenu, () ->
                launchFragment("com.superior.lab.fragments.PowerMenuSettings", R.string.powermenu_title));

        attachSpringTouch(mCardButtons, () ->
                launchFragment("com.superior.lab.fragments.ButtonSettings", R.string.button_title));

        attachSpringTouch(mCardGestures, () ->
                launchFragment("com.android.settings.gestures.GestureSettings", R.string.gestures_title));

        attachSpringTouch(mCardMisc, () ->
                launchFragment("com.superior.lab.fragments.MiscSettings", R.string.misc_title));

        attachSpringTouch(mCardTeam, () ->
                launchFragment("com.superior.lab.fragments.SuperiorTeam", R.string.team_title));
    }
    private void setupQuickToggles(@NonNull LayoutPreference pref) {
        mToggleSecureWindow = pref.findViewById(R.id.toggle_secure_window);
        mToggleHideCapture = pref.findViewById(R.id.toggle_hide_capture);
        mToggleHeadsUp = pref.findViewById(R.id.toggle_heads_up);
        mTogglePulse = pref.findViewById(R.id.toggle_pulse);

        attachSpringTouch(mToggleSecureWindow, () -> {
            final Context ctx = getContext();
            if (ctx == null) return;
            final ContentResolver cr = ctx.getContentResolver();
            final boolean enabled = Settings.Global.getInt(cr, SETTING_WINDOW_IGNORE_SECURE, 0) == 1;
            Settings.Global.putInt(cr, SETTING_WINDOW_IGNORE_SECURE, enabled ? 0 : 1);
            refreshDashboardStates();
        });

        attachSpringTouch(mToggleHideCapture, () -> {
            final Context ctx = getContext();
            if (ctx == null) return;
            final ContentResolver cr = ctx.getContentResolver();
            final boolean enabled = Settings.Global.getInt(cr, SETTING_HIDE_SCREEN_CAPTURE, 0) == 1;
            Settings.Global.putInt(cr, SETTING_HIDE_SCREEN_CAPTURE, enabled ? 0 : 1);
            refreshDashboardStates();
        });

        attachSpringTouch(mToggleHeadsUp, () -> {
            final Context ctx = getContext();
            if (ctx == null) return;
            final ContentResolver cr = ctx.getContentResolver();
            final boolean enabled = Settings.Global.getInt(cr, SETTING_HEADS_UP_ENABLED, 1) == 1;
            Settings.Global.putInt(cr, SETTING_HEADS_UP_ENABLED, enabled ? 0 : 1);
            refreshDashboardStates();
        });

        attachSpringTouch(mTogglePulse, () -> {
            final Context ctx = getContext();
            if (ctx == null) return;
            final ContentResolver cr = ctx.getContentResolver();
            final boolean enabled = Settings.Secure.getIntForUser(
                    cr, SETTING_LOCKSCREEN_PULSE, 0, UserHandle.USER_CURRENT) == 1;
            Settings.Secure.putIntForUser(
                    cr, SETTING_LOCKSCREEN_PULSE, enabled ? 0 : 1, UserHandle.USER_CURRENT);
            refreshDashboardStates();
        });
    }

    private void refreshDashboardStates() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final ContentResolver resolver = context.getContentResolver();

        // 1. Sync 2x2 Tonal Color-Matched Quick Toggle Tiles
        final boolean ignoreSecure = Settings.Global.getInt(resolver, SETTING_WINDOW_IGNORE_SECURE, 0) == 1;
        final boolean hideCapture = Settings.Global.getInt(resolver, SETTING_HIDE_SCREEN_CAPTURE, 0) == 1;
        final boolean headsUp = Settings.Global.getInt(resolver, SETTING_HEADS_UP_ENABLED, 1) == 1;
        final boolean pulse = Settings.Secure.getIntForUser(
                resolver, SETTING_LOCKSCREEN_PULSE, 0, UserHandle.USER_CURRENT) == 1;

        updateTonalPillVisual(mToggleSecureWindow,
                R.id.icon_box_secure_window, R.id.icon_secure_window,
                R.id.label_secure_window, R.id.dot_secure_window,
                ignoreSecure, R.string.lab_quick_secure_on, R.string.lab_quick_secure_off,
                R.drawable.bg_lab_tag_pill_misc,
                R.color.lab_icon_misc, R.color.lab_text_misc);

        updateTonalPillVisual(mToggleHideCapture,
                R.id.icon_box_hide_capture, R.id.icon_hide_capture,
                R.id.label_hide_capture, R.id.dot_hide_capture,
                hideCapture, R.string.lab_quick_capture_on, R.string.lab_quick_capture_off,
                R.drawable.bg_lab_tag_pill_statusbar,
                R.color.lab_icon_statusbar, R.color.lab_text_statusbar);

        updateTonalPillVisual(mToggleHeadsUp,
                R.id.icon_box_heads_up, R.id.icon_heads_up,
                R.id.label_heads_up, R.id.dot_heads_up,
                headsUp, R.string.lab_quick_headsup_on, R.string.lab_quick_headsup_off,
                R.drawable.bg_lab_tag_pill_notifications,
                R.color.lab_icon_notifications, R.color.lab_text_notifications);

        updateTonalPillVisual(mTogglePulse,
                R.id.icon_box_pulse, R.id.icon_pulse,
                R.id.label_pulse, R.id.dot_pulse,
                pulse, R.string.lab_quick_pulse_on, R.string.lab_quick_pulse_off,
                R.drawable.bg_lab_tag_pill_lockscreen,
                R.color.lab_icon_lockscreen, R.color.lab_text_lockscreen);

        // 2. Sync Live Card Subtitles
        if (mSummaryStatusbar != null) {
            final int trafficMode = LineageSettings.Secure.getInt(
                    resolver, LineageSettings.Secure.NETWORK_TRAFFIC_MODE, 0);
            final boolean clockSeconds = Settings.Secure.getInt(resolver, "clock_seconds", 0) == 1;
            if (trafficMode != 0 && clockSeconds) {
                mSummaryStatusbar.setText(R.string.lab_statusbar_live_traffic_sec);
            } else if (trafficMode != 0) {
                mSummaryStatusbar.setText(R.string.lab_statusbar_live_traffic);
            } else {
                mSummaryStatusbar.setText(R.string.lab_statusbar_desc);
            }
        }

        if (mSummaryNotifications != null) {
            final boolean lessBoring = Settings.System.getInt(resolver, SETTING_LESS_BORING_HEADS_UP, 0) == 1;
            if (!headsUp) {
                mSummaryNotifications.setText(R.string.lab_notifications_live_off);
            } else if (lessBoring) {
                mSummaryNotifications.setText(R.string.lab_notifications_live_less_boring);
            } else {
                mSummaryNotifications.setText(R.string.lab_notifications_live_on);
            }
        }

        if (mSummaryLockscreen != null) {
            final boolean ripple = Settings.System.getInt(resolver, SETTING_RIPPLE_EFFECT, 1) == 1;
            final boolean mediaArt = Settings.System.getInt(resolver, SETTING_MEDIA_ART, 0) == 1;
            if (pulse && mediaArt) {
                mSummaryLockscreen.setText(R.string.lab_lockscreen_live_pulse_art);
            } else if (pulse) {
                mSummaryLockscreen.setText(R.string.lab_lockscreen_live_pulse);
            } else if (ripple) {
                mSummaryLockscreen.setText(R.string.lab_lockscreen_live_ripple);
            } else {
                mSummaryLockscreen.setText(R.string.lab_lockscreen_desc);
            }
        }

        if (mSummaryPowerMenu != null) {
            final boolean advReboot = LineageSettings.Secure.getInt(
                    resolver, LineageSettings.Secure.ADVANCED_REBOOT, 0) == 1;
            mSummaryPowerMenu.setText(advReboot
                    ? R.string.lab_powermenu_live_adv_on
                    : R.string.lab_powermenu_desc);
        }
        // 3. Sync Misc Subtitle
        if (mSummaryMisc != null) {
            if (ignoreSecure && hideCapture) {
                mSummaryMisc.setText(R.string.lab_misc_live_privacy_both);
            } else if (ignoreSecure) {
                mSummaryMisc.setText(R.string.lab_misc_live_secure_ignored);
            } else if (hideCapture) {
                mSummaryMisc.setText(R.string.lab_misc_live_capture_hidden);
            } else {
                mSummaryMisc.setText(R.string.lab_misc_desc);
            }
        }
    }

    private void updateTonalPillVisual(@Nullable View pill,
            int iconBoxId, int iconId, int labelId, int dotId,
            boolean active, int textOnRes, int textOffRes,
            int activeBgRes, int activeIconBgColorRes, int activeTextColorRes) {
        final Context context = getContext();
        if (pill == null || context == null) {
            return;
        }

        final View iconBox = pill.findViewById(iconBoxId);
        final ImageView icon = pill.findViewById(iconId);
        final TextView label = pill.findViewById(labelId);
        final ImageView dot = pill.findViewById(dotId);

        if (active) {
            final int accentColor = ContextCompat.getColor(context, activeIconBgColorRes);
            final int textColor = ContextCompat.getColor(context, activeTextColorRes);
            pill.setBackgroundResource(activeBgRes);
            if (iconBox != null) {
                iconBox.setBackgroundTintList(ColorStateList.valueOf(accentColor));
            }
            if (icon != null) {
                icon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.lab_tag_text_selected)));
            }
            if (label != null) {
                label.setText(textOnRes);
                label.setTextColor(textColor);
            }
            if (dot != null) {
                dot.setImageResource(R.drawable.ic_lab_dot_on);
                dot.setImageTintList(ColorStateList.valueOf(accentColor));
            }
        } else {
            final int unselectedText = ContextCompat.getColor(context, R.color.lab_tag_text_unselected);
            final int unselectedIconBg = ContextCompat.getColor(context, R.color.lab_tag_icon_bg_unselected);
            pill.setBackgroundResource(R.drawable.bg_lab_tag_pill_unselected);
            if (iconBox != null) {
                iconBox.setBackgroundTintList(ColorStateList.valueOf(unselectedIconBg));
            }
            if (icon != null) {
                icon.setImageTintList(ColorStateList.valueOf(unselectedText));
            }
            if (label != null) {
                label.setText(textOffRes);
                label.setTextColor(unselectedText);
            }
            if (dot != null) {
                dot.setImageResource(R.drawable.ic_lab_dot_off);
                dot.setImageTintList(null);
            }
        }
    }

    private void attachSpringTouch(@Nullable View view, @NonNull Runnable onClick) {
        if (view == null) return;

        view.setOnTouchListener(new View.OnTouchListener() {
            private final Rect rect = new Rect();
            private boolean isDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDown = true;
                        v.getDrawingRect(rect);
                        v.animate()
                                .scaleX(0.95f)
                                .scaleY(0.95f)
                                .setDuration(110)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (isDown && !rect.contains((int) event.getX(), (int) event.getY())) {
                            isDown = false;
                            bounceBack(v);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (isDown) {
                            isDown = false;
                            bounceBack(v);
                            v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                            onClick.run();
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        if (isDown) {
                            isDown = false;
                            bounceBack(v);
                        }
                        return true;
                }
                return false;
            }

            private void bounceBack(View v) {
                v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(260)
                        .setInterpolator(new OvershootInterpolator(2.4f))
                        .start();
            }
        });
    }

    private void startEntranceAnimations(@NonNull LayoutPreference pref) {
        final View tags = pref.findViewById(R.id.lab_tags_scrollview);
        final View row1 = pref.findViewById(R.id.row_statusbar_notifications);
        final View row2 = pref.findViewById(R.id.row_lockscreen_power);
        final View row3 = pref.findViewById(R.id.row_buttons_gestures);
        final View row4 = pref.findViewById(R.id.card_misc);
        final View row5 = pref.findViewById(R.id.card_team);

        final View[] entranceViews = new View[]{tags, row1, row2, row3, row4, row5};
        for (int i = 0; i < entranceViews.length; i++) {
            final View v = entranceViews[i];
            if (v != null) {
                v.setTranslationY(48f);
                v.setAlpha(0f);
                v.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(340)
                        .setStartDelay(i * 45L)
                        .setInterpolator(new DecelerateInterpolator(1.5f))
                        .start();
            }
        }
    }

    private void launchFragment(String fragmentDestination, int titleRes) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        new SubSettingLauncher(context)
                .setDestination(fragmentDestination)
                .setTitleRes(titleRes)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SUPERIOR;
    }

    public static void lockCurrentOrientation(Activity activity) {
        int currentRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        int frozenRotation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        switch (currentRotation) {
            case Surface.ROTATION_0:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case Surface.ROTATION_270:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        activity.setRequestedOrientation(frozenRotation);
    }
}
