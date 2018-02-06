package org.linphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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
        String hangUpNr = null;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("uri")) {
                hangUpNr = FormatUri(extras.getString("uri"));
            }
        }
        // Sip Found, try hangup this number
        //if (hangUpNr != null){
        //    if (TerminatePhoneCall(hangUpNr) == true)
        //       return;
        //}

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

    private Boolean TerminatePhoneCall (String hangUpNr){

        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();

        // Find if HangUp Nr is current call, if so terminate
        String contact = null;
        if (currentCall != null) {
            contact = FormatUri(currentCall.getRemoteContact());
            if (contact.contains(hangUpNr) == true) {
                lc.terminateCall(currentCall);
                //    return true;
            }
        }

        // If HangUpNr not Equals current call, try terminate from list
        if (lc.isInConference())
            lc.terminateConference();

        LinphoneCall[] calls= lc.getCalls();
        for (LinphoneCall call : calls) {

            String uri= FormatUri(call.getRemoteContact());
            if (uri.contains(hangUpNr)) {
                lc.terminateCall(call);
                return true;
            }

        }
        return false; // uri not found
    }
}
