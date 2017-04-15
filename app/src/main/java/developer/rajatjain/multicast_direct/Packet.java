package developer.rajatjain.multicast_direct;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by Rajat Jain on 06-04-2017.
 */

public class Packet {
    public int ACK; //Acknowledgment packet flag
    public int DATA;//DATA packet flag
    public int FIN; //FINISH packet flag
    public int RST; //Reset connection flag
    public String Data;
    public WifiP2pDevice Sender;
    public WifiP2pDevice Receiver;
    public long SenderStamp;
    public long ReceiverStamp;
}
