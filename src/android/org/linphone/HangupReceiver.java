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
 * 16-10-18 rvdillen tweaked hangup current call (BG-7267)
 * 10-10-18 rvdillen Activate next 'in pause call' after hangup
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
        uri = uri.replace("%20speak", " speak"); // Listen only connection (BG-6400)
        uri = uri.replace("%21speak", "!speak"); // old way as back up)
        return uri;
    }

    private Boolean TerminatePhoneCall (String uriExtra){

        try{
            uriExtra = FormatUri(uriExtra);
            LinphoneCore lc = LinphoneManager.getLc();

            // Find if HangUp Nr is current call, if so terminate
            LinphoneCall currentCall = lc.getCurrentCall();
            if (currentCall != null) {
                // Current call found
                LinphoneAddress remoteAddress = currentCall.getRemoteAddress();
                String remAddress = null;
                if(remoteAddress != null) {
                    remAddress = remoteAddress.asString();
                }
                Log.i("HangupReceiver", "TerminatePhoneCall started: Remote address: " + remAddress);

                // Check if any (other) calls in pause state
                boolean anyCallInPause = false;
                LinphoneCall[] calls = lc.getCalls();
                if(calls != null && calls.length > 0) {
                    for (LinphoneCall call: calls) {
                        anyCallInPause = anyCallInPause || call.getState() == LinphoneCall.State.Paused;
                    }
                }

                // Check if Current call has same uri as from hangup uri
                if (remAddress != null && remAddress.contains(uriExtra) == true) {

                    // Current call is from hangup uri. => terminateCall now && if no calls in pause resetClassicMenuLayoutAndGoBackToCallIfStillRunning
                    Log.i("HangupReceiver", "TerminatePhoneCall: terminate currentCall Remote address: " + remAddress);
                    lc.terminateCall(currentCall);
                    if (anyCallInPause == false)
                        TryTerminateActiveCall(true);
                    return true;
                }
                else if (anyCallInPause == true) {
                    Log.i("HangupReceiver", "TerminatePhoneCall: terminate currentCall, cause calls in pause");
                    lc.terminateCall(currentCall);
                    return true;
                }
                else {
                    Log.i("HangupReceiver", "TerminatePhoneCall: terminate currentCall ignored, cause already gone");
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
