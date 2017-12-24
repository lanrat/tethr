package com.vorsk.tethr;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.support.v7.widget.CardView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static PhoneStateListener psListener;
    private static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CardView refresh_button = (CardView) findViewById(R.id.card_refresh);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.v(TAG, "card_refresh button clicked!");
                refresh();
            }
        });

        final CardView wifi_button = (CardView) findViewById(R.id.card_wifi);
        wifi_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.v(TAG, "card_wifi button clicked!");
                enableTethering(TetherAccessibilityService.TETHER_WIFI);
            }
        });

        final CardView usb_button = (CardView) findViewById(R.id.card_usb);
        usb_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.v(TAG, "card_usb button clicked!");
                enableTethering(TetherAccessibilityService.TETHER_USB);
            }
        });

        final CardView bt_button = (CardView) findViewById(R.id.card_bluetooth);
        bt_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.v(TAG, "card_bluetooth button clicked!");
                enableTethering(TetherAccessibilityService.TETHER_BLUETOOTH);
            }
        });

        final CardView settings_button = (CardView) findViewById(R.id.card_settings);
        settings_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.v(TAG, "card_settings button clicked!");
                startTetherSettingsActivity(v.getContext());
            }
        });
    }

    private void refresh() {
        startLoading(this);
        CellRefresh nr = new CellRefresh(this);
        nr.Refresh();
        //stopLoading();
    }

    private void enableTethering(int mode) {
        if (!serviceIsRunning()) {
            // accessibility service is disabled, show prompt to enable it
            showServiceDialog();
            return;
        }
        refresh();

        TetherAccessibilityService.enableService(mode);

        // start service to listen for cell events
        psListener = new TetherPhoneStateListener(this);
        TelephonyManager tManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        tManager.listen(psListener, PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_DATA_ACTIVITY);
    }

    private void showServiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.service_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.service_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        openServiceSettings();
                    }
                })
                .setNegativeButton(R.string.service_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        //MainActivity.this.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void openServiceSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_about:
                showAbout();
                break;
            case R.id.menu_action_service:
                openServiceSettings();
                break;
            default:
                break;
        }
        return true;
    }


    public static void startTetherSettingsActivity(Context ctx) {
        if (psListener != null) {
            TelephonyManager tManager = (TelephonyManager) ctx.getSystemService(TELEPHONY_SERVICE);
            tManager.listen(psListener, PhoneStateListener.LISTEN_NONE);
            psListener = null;
            Log.v(TAG, "disabling listener ");
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setComponent(cn);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // enable for split windows
        ctx.startActivity(intent);
    }

    private boolean serviceIsRunning() {
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> infos = am.getRunningServices(Short.MAX_VALUE);
        String serviceName = getPackageName() + "." + TetherAccessibilityService.class.getSimpleName();
        for (ActivityManager.RunningServiceInfo info : infos) {
            if (info.service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        //TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
        //int defaultColor = textView.getTextColors().getDefaultColor();
        //textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    public static void LogException(Exception e) {
        Log.e("", "Exception "+e.toString());
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter( writer );
        e.printStackTrace( printWriter );
        printWriter.flush();

        String stackTrace = writer.toString();
        Log.e("", stackTrace);
    }


    public static void startLoading(Context ctx) {
        if (progressDialog != null) {
            stopLoading();
        }
        progressDialog = new ProgressDialog(ctx);
        progressDialog.setTitle(R.string.loading_title);
        progressDialog.setMessage(ctx.getString(R.string.loading_message));
        progressDialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progressDialog.show();
    }

    public static void stopLoading() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }
}