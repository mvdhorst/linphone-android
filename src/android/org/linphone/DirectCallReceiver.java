package org.linphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import org.linphone.compatibility.Compatibility;
import org.linphone.mediastream.Version;

import static android.content.Intent.ACTION_MAIN;

/**
 * Created by mvdhorst on 3-1-18.
 * Starts a call without showing the UI.
 */

public class DirectCallReceiver extends BroadcastReceiver {
    private String addressToCall;

    private Handler mHandler;
    private ServiceWaitThread mServiceThread;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isOrderedBroadcast())
            abortBroadcast();
/*
LinphoneCore lc = LinphoneManager.getLc();
LinphoneCall currentCall = lc.getCurrentCall();
*/
        if (intent.hasExtra(("uri"))) {
            addressToCall = intent.getStringExtra("uri");
            addressToCall = addressToCall.replace("%40", "@");
            addressToCall = addressToCall.replace("%3A", ":");
            if (addressToCall.startsWith("sip:")) {
                addressToCall = addressToCall.substring("sip:".length());
            }
        }

        mHandler = new Handler();
        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            // start linphone as background
            Compatibility.startService(context, new Intent(ACTION_MAIN).setClass(context, LinphoneService.class));
            mServiceThread = new ServiceWaitThread();
            mServiceThread.start();
        }
    }


    protected void onServiceReady() {
        // We need LinphoneService to start bluetoothManager
        if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
            BluetoothManager.getInstance().initBluetooth();
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LinphoneManager.getInstance().newOutgoingCall(addressToCall, null);
            }
        }, 100);
    }

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mServiceThread = null;
        }
    }
}
