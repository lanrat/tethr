package com.vorsk.tethr;


import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

class TetherPhoneStateListener extends PhoneStateListener {
    private static String TAG = TetherPhoneStateListener.class.getSimpleName();
    private Context mContext;

    TetherPhoneStateListener(Context context) {
        mContext = context;
    }

    @Override
    public void onDataActivity(int direction) {
        super.onDataActivity(direction);

        if (direction == TelephonyManager.DATA_ACTIVITY_NONE) {
            // start activity
            MainActivity.startTetherSettingsActivity(mContext);
        }
    }

}