package developer.rajatjain.multicast_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import developer.rajatjain.multicast_direct.Interfaces.Communicate;


/**
 * Created by Rajat Jain on 09-01-2017.
 */

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG ="WiFiDirectBroadcastReceiver" ;
    public WifiP2pManager mManager;
    public WifiP2pManager.Channel mChannel;
    public MainActivity mActivity;
    Communicate communicate;
    public WifiP2pManager.GroupInfoListener groupInfoListener=new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            if(group!=null){
                Log.e(MainActivity.TAG,group.getClientList().toString());
                communicate.connection(group.getClientList().toString());
            }
        }

    };
    public WifiP2pManager.PeerListListener myPeerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (peerList.getDeviceList().size() == 0) {
                Log.e(MainActivity.TAG, "No devices found");
                return;
            }else{
                Log.e(MainActivity.TAG,peerList.getDeviceList().toString());
                communicate.onGetPeerList(peerList);
            }

        }
    };
    public WiFiDirectBroadcastReceiver(Context context){

    }
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.communicate=(Communicate)activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mActivity.setIsWifiP2pEnabled(true);
            } else {
                mActivity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if (mManager != null) {
                mManager.requestPeers(mChannel, myPeerListListener);
            }
            Log.e(MainActivity.TAG,"P2P Device list changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if(mManager !=null){
                mManager.requestGroupInfo(mChannel,groupInfoListener);
            }
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                communicate.notifyThisDeviceChanged(intent);

        }
    }


}
