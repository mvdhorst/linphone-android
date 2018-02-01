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

        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();

        // Find Sip Nr to hang up
        String hangUpNr = null;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("uri")) {
                hangUpNr = extras.getString("uri");
            }
        }

        // Find Sip Nr of current call
        String contact = null;
        if (currentCall != null) {
            contact = FormatUri(currentCall.getRemoteContact());
        }

        // If HangUpNr added and not Equals current call, do not hang up current call, try terminate from list
        if (contact != null && hangUpNr != null && !contact.equalsIgnoreCase(hangUpNr))  {
            LinphoneCall[] calls= lc.getCalls();
            for (LinphoneCall call : calls) {
                String uri= FormatUri(call.getRemoteContact());
                if (uri != null && uri.equalsIgnoreCase(hangUpNr)) {
                    lc.terminateCall(call);
                }
            }
            return;
        }
        // <= Validation on sip HangUp nr

        // Default hangup behaviour
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
}
