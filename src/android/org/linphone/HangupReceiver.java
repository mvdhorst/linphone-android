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

        if (uri != null) {
            int index = uri.indexOf("@");
            if (index > -1){
                uri = uri.substring(0, index);
            }
        }
        return uri;
    }

    private Boolean TerminatePhoneCall (String uriExtra){

        uriExtra = uriExtra.toLowerCase();
        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();
        //String hangUpNr = FormatUri(uriExtra);
        //Uri uri = Uri.parse(uriExtra);
        //String host = uri.getHost();
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
                LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning(true);
                return true;
            }
        }

        // If HangUpNr not Equals current call, try terminate from list
        if (lc.isInConference()) {
            lc.terminateConference();
            LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning(true);
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
                LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning(true);
                return true;
            }

        }
        LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning(true);
        return false; // uri not found
    }
}
