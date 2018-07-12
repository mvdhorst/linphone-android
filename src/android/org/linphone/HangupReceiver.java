package org.linphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;

/**
 * Created by mvdhorst on 18-12-17.
 * Hangs up a call.
 *
 * 12-07-18 rvdillen Fix HangUp for listen only mode
 * 01-02-18 rvdillen Add hangUp uri to hangup selected call
 * 18-12-17 mvdhorst Initial version
 */

public class HangupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(isOrderedBroadcast())
            abortBroadcast();


        // Find Sip Nr to hang up
        String uriExtra = null;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("uri")) {
                uriExtra = extras.getString("uri");
            }
        }
        // Sip Found, try hangup this number
        if (uriExtra != null){
            TerminatePhoneCall(uriExtra);
            return;
        }

        // Default hangup behaviour (no uri or terminate uri failed)
        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();

        if (currentCall != null) {
            lc.terminateCall(currentCall);
        } else if (lc.isInConference()) {
            lc.terminateConference();
        } else {
            lc.terminateAllCalls();
        }
    }

    private String FormatUri (String uri){
        uri = uri.toLowerCase();
        uri = uri.replace("%21speak", "!speak"); // Listen only connection (BG-6400)
        return uri;
    }

    private Boolean TerminatePhoneCall (String uriExtra){

        try{
            uriExtra = FormatUri(uriExtra);
            LinphoneCore lc = LinphoneManager.getLc();
            LinphoneCall currentCall = lc.getCurrentCall();

            // Find if HangUp Nr is current call, if so terminate
            if (currentCall != null) {
                LinphoneAddress remoteAddress = currentCall.getRemoteAddress();
                String remAddress = null;
                if(remoteAddress != null) {
                    remAddress = remoteAddress.asString();
                }
                Log.i("HangupReceiver", "TerminatePhoneCall: currentCall Remote address: " + remAddress);
                if (remAddress != null && remAddress.contains(uriExtra) == true) {
                    lc.terminateCall(currentCall);
                    TryTerminateActiveCall(true);
                    return true;
                }
            }

            // If HangUpNr not Equals current call, try terminate from list
            if (lc.isInConference()) {
                lc.terminateConference();
                TryTerminateActiveCall(true);
            }

            LinphoneCall[] calls= lc.getCalls();
            for (LinphoneCall call : calls) {

                LinphoneAddress remoteAddress = call.getRemoteAddress();
                String remAddress = null;
                if(remoteAddress != null) {
                    remAddress = remoteAddress.asString();
                }
                Log.i("HangupReceiver", "TerminatePhoneCall: other call Remote address: " + remAddress);
                if (remAddress != null && remAddress.contains(uriExtra) == true) {
                    lc.terminateCall(call);
                    TryTerminateActiveCall(true);
                    return true;
                }
            }
            TryTerminateActiveCall(true);
        }
        catch  (Exception e) {
            Log.e("HangupReceiver", "Exception TerminatePhoneCall: " + e.getMessage());
        }
        return false; // uri not found
    }

    private Boolean TryTerminateActiveCall (Boolean fromHangupReceiver){

        try{
            if (!LinphoneActivity.isInstanciated())
                return false;

            LinphoneActivity main = LinphoneActivity.instance();
            if (main == null)
                return false;

            main.resetClassicMenuLayoutAndGoBackToCallIfStillRunning(fromHangupReceiver);
            return true;

        }
        catch(Exception e){
            Log.e("HangupReceiver", "Exception TryTerminateActiveCall: " + e.getMessage());
        }
        return false;
    }
}
