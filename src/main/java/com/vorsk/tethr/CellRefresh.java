package com.vorsk.tethr;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class CellRefresh {
    private static final String TAG = CellRefresh.class.getSimpleName();

    private enum State {
        NONE, STOPPING, GETTING, FINDING, SUCCESS, FAIL1, FAIL2, SETTING
    }

    private TelephonyManager telManager;
    private ConnectivityManager conManager;
    private PhoneStateListener signalListener;

    private State state;

    private Handler cellHandler = new Handler();

    private Runnable toggleRef;
    private Runnable updateFindingTask;
    private Runnable updateGettingTask;
    private Runnable updateLastTask;
    private Runnable updateLastFailedTask;
    private Runnable updateLatTask;
    private Runnable updateSettingTask;
    private Runnable updateStopTask;

    CellRefresh(Context ctx) {
        this.telManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        this.conManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.signalListener = new AndroidPhoneStateListener();

        this.toggleRef = new ToggleRefRunner();
        this.updateStopTask = new UpdateStopTaskRunner();
        this.updateGettingTask = new UpdateGettingTaskRunner();
        this.updateFindingTask = new UpdateFindingTaskRunner();
        this.updateSettingTask = new UpdateSettingTaskRunner();
        this.updateLastFailedTask = new UpdateLastFailedTaskRunner();
        this.updateLatTask = new UpdateLatTaskRunner();
        this.updateLastTask = new UpdateLastTaskRunner();

        this.state = State.NONE;
    }

    private void toggleN(boolean delay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Method getITelephonyMethod = Class.forName(telManager.getClass().getName()).getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                Object telephony = getITelephonyMethod.invoke(telManager);
                getITelephonyMethod = Class.forName(telephony.getClass().getName()).getDeclaredMethod("setCellInfoListRate", Integer.TYPE);
                getITelephonyMethod.setAccessible(true);
                if (delay) {
                    getITelephonyMethod.invoke(telephony, 10);
                    return;
                }

                getITelephonyMethod.invoke(telephony, 0);
                return;
            } catch (Exception  e) {
                Log.e(TAG, e.toString());
                return;
            }
        }

        try {
            Field iConnectivityManagerField = Class.forName(conManager.getClass().getName()).getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            Object iConnectivityManager = iConnectivityManagerField.get(conManager);

            Method setRadioMethod = Class.forName(iConnectivityManager.getClass().getName()).getDeclaredMethod("setRadio", Integer.TYPE, Boolean.TYPE);
            setRadioMethod.setAccessible(true);
            Object[] args = new Object[2];
            args[0] = 0;
            args[1] = !delay;
            setRadioMethod.invoke(iConnectivityManager, args);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void removeAllCallbacks() {
        try {
            this.cellHandler.removeCallbacks(this.updateStopTask);
            this.cellHandler.removeCallbacks(this.updateGettingTask);
            this.cellHandler.removeCallbacks(this.updateFindingTask);
            this.cellHandler.removeCallbacks(this.updateSettingTask);
            this.cellHandler.removeCallbacks(this.updateLastFailedTask);
            this.cellHandler.removeCallbacks(this.updateLatTask);
            this.cellHandler.removeCallbacks(this.updateLastTask);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    void Refresh() {
        this.toggleAero(true);

        this.cellHandler.postDelayed(this.toggleRef, 9000);

        this.toggleData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
            this.telManager.listen(this.signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

        // reset callbacks
        this.cellHandler.removeCallbacks(this.updateStopTask);
        this.cellHandler.postDelayed(this.updateStopTask, 200);
        this.cellHandler.removeCallbacks(this.updateGettingTask);
        this.cellHandler.postDelayed(this.updateGettingTask, 5000);
        this.cellHandler.removeCallbacks(this.updateFindingTask);
        this.cellHandler.postDelayed(this.updateFindingTask, 12000);
        this.cellHandler.removeCallbacks(this.updateSettingTask);
        this.cellHandler.postDelayed(this.updateSettingTask, 27000);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR_MR1) {
            this.cellHandler.postDelayed(this.updateLastTask, 41000);
        } else {
            this.cellHandler.postDelayed(this.updateLastFailedTask, 70000);
        }
    }

    private void toggleData() {
        try {
            if ((conManager.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || (conManager.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTING)) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
                    setMobileDataEnabledGingerPlus(false);
                    try {
                        Thread.sleep(2000L);
                        setMobileDataEnabledGingerPlus(true);
                        return;
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }
                }
                switchMobileDataEnabledGingerLess(false);
                try {
                    Thread.sleep(2000L);
                    switchMobileDataEnabledGingerLess(true);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void switchMobileDataEnabledGingerLess(boolean enabled) {
        try {
            Method getITelephonyMethod = Class.forName(telManager.getClass().getName()).getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            Object telephony = getITelephonyMethod.invoke(telManager);
            if (enabled) {
                Method enableDataConnectivityMethod = Class.forName(telephony.getClass().getName()).getDeclaredMethod("enableDataConnectivity");
                enableDataConnectivityMethod.setAccessible(true);
                enableDataConnectivityMethod.invoke(telephony);
            } else {
                Method disableDataConnectivityMethod = Class.forName(telephony.getClass().getName()).getDeclaredMethod("disableDataConnectivity");
                disableDataConnectivityMethod.setAccessible(true);
                disableDataConnectivityMethod.invoke(telephony);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void setMobileDataEnabledGingerPlus(boolean enabled) {
        try {
            Object iConnectivityManager;
            Field mServiceMethod = Class.forName(conManager.getClass().getName()).getDeclaredField("mService");
            mServiceMethod.setAccessible(true);
            iConnectivityManager = mServiceMethod.get(conManager);
            Method setMobileDataEnabledMethod = Class.forName(iConnectivityManager.getClass().getName()).getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void toggleAero(boolean delay) {
        try {
            if (!delay) {
                toggleN(false);
            }

            toggleN(delay);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private class ToggleRefRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "ToggleRefRunner.run");
            CellRefresh.this.toggleAero(false);
            CellRefresh.this.cellHandler.removeCallbacks(toggleRef);
        }
    }

    private class UpdateStopTaskRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "UpdateStopTaskRunner.run");
            state = State.STOPPING;
            CellRefresh.this.cellHandler.removeCallbacks(updateStopTask);
        }
    }

    private class UpdateGettingTaskRunner implements Runnable {
        public void run() {
            Log.v(TAG, "UpdateGettingTaskRunner.run");
            state = State.GETTING;
            CellRefresh.this.cellHandler.removeCallbacks(updateGettingTask);
        }
    }

    private class UpdateFindingTaskRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "UpdateFindingTaskRunner.run");
            state = State.FINDING;
            CellRefresh.this.cellHandler.removeCallbacks(updateFindingTask);
        }
    }

    private class UpdateLastTaskRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "UpdateLastTaskRunner.run");
            state = State.SUCCESS;
            CellRefresh.this.cellHandler.removeCallbacks(updateLastTask);
            CellRefresh.this.removeAllCallbacks();
            CellRefresh.this.telManager.listen(CellRefresh.this.signalListener, 0);
        }
    }

    private class UpdateLastFailedTaskRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "UpdateLastFailedTaskRunner.run");
            if (state != State.SUCCESS) {
                CellRefresh.this.cellHandler.removeCallbacks(updateLatTask);
                CellRefresh.this.telManager.listen(CellRefresh.this.signalListener, 0);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    state = State.FAIL1;
                } else {
                    state = State.FAIL2;
                }
            }

            CellRefresh.this.cellHandler.removeCallbacks(updateLastFailedTask);
            CellRefresh.this.removeAllCallbacks();
        }
    }

    private class UpdateLatTaskRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "UpdateLatTaskRunner.run");
            state = State.SUCCESS;
            CellRefresh.this.cellHandler.removeCallbacks(updateLatTask);
        }
    }

    private class UpdateSettingTaskRunner implements Runnable {
        public void run() {
            //Log.v(TAG, "UpdateSettingTaskRunner.run");
            state = State.SETTING;
            CellRefresh.this.cellHandler.removeCallbacks(updateSettingTask);
        }
    }

    private class AndroidPhoneStateListener extends PhoneStateListener {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            //Log.v(TAG, "AndroidPhoneStateListener.onSignalStrengthsChanged()");
            //Log.v(TAG, "AndroidPhoneStateListener state: "+state);
            int state_int;
            if (state == State.FINDING) {
                // best guess...
                MainActivity.stopLoading();
            }
            try {
                if (!signalStrength.isGsm()) {
                    // CDMA signal

                    if (signalStrength.getCdmaDbm() < -95) {
                        if ( signalStrength.getCdmaEcio() < -130) {
                            return;
                        }
                    }

                    CellRefresh.this.removeAllCallbacks();
                    state_int = 0;
                    if (state == State.GETTING) {
                        state_int = 2;
                    } else if (state == State.FINDING) {
                        state_int = 3;
                    } else if (state == State.SETTING) {
                        state_int = 4;
                    }

                    for (int i = state_int; i < 4; i++) {
                        if (i == 0) {
                            CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateStopTask);
                            CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateStopTask, 100);
                        } else if (i == 1) {
                            CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateGettingTask);
                            CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateGettingTask, 2500);
                        } else if (i == 2) {
                            CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateFindingTask);
                            CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateFindingTask, 7500);
                        } else if (i == 3) {
                            CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateSettingTask);
                            CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateSettingTask, 12500);
                        }
                    }

                    if (state_int == 0) {
                        CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateLatTask);
                        CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateLatTask, 25000);
                    } else {
                        CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateLatTask);
                        CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateLatTask, 7000);
                    }

                    CellRefresh.this.telManager.listen(CellRefresh.this.signalListener, 0);
                    return;
                }
                int signalStrengthValue = signalStrength.getGsmSignalStrength();
                if (signalStrengthValue < 5) {
                    return;
                }
                if (signalStrengthValue == 99) {
                    return;
                }

                CellRefresh.this.removeAllCallbacks();
                state_int = 0;
                if (state == State.GETTING) {
                    state_int = 2;
                } else if (state == State.FINDING) {
                    state_int = 3;
                } else if (state == State.SETTING) {
                    state_int = 4;
                }

                for (int i = state_int; i < 4; i++) {
                    if (i == 0) {
                        CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateStopTask);
                        CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateStopTask, 100);
                    } else if (i == 1) {
                        CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateGettingTask);
                        CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateGettingTask, 6500);
                    } else if (i == 2) {
                        CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateFindingTask);
                        CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateFindingTask, 12500);
                    } else if (i == 3) {
                        CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateSettingTask);
                        CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateSettingTask, 17500);
                    }
                }
                if (state_int == 0) {
                    CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateLatTask);
                    CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateLatTask, 25000);
                } else {
                    CellRefresh.this.cellHandler.removeCallbacks(CellRefresh.this.updateLatTask);
                    CellRefresh.this.cellHandler.postDelayed(CellRefresh.this.updateLatTask, 7000);
                }
                CellRefresh.this.telManager.listen(CellRefresh.this.signalListener, 0);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
