package developer.rajatjain.multicast_direct.Interfaces;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;

/**
 * Created by Rajat Jain on 10-01-2017.
 */

public interface Communicate {
    void onGetPeerList(WifiP2pDeviceList peerList);
    void connection(String list);
    void notifyThisDeviceChanged(Intent intent);
    void getRecievedText(String msg);
    void notifyAboutSenderAction(String msg);
}
