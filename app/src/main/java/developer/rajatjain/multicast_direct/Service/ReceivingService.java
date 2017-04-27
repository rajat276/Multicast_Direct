package developer.rajatjain.multicast_direct.Service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import developer.rajatjain.multicast_direct.Handler.ReceiverHandler;
import developer.rajatjain.multicast_direct.MainActivity;
import developer.rajatjain.multicast_direct.Utils.NetworkUtil;

/**
 * Created by Rajat Jain on 05-03-2017.
 */

public class ReceivingService extends IntentService {
    public final static String ON_LISTEN ="ON_LISTEN";
    public final static String COMMUNICATE="COMMUNICATE";
    public static boolean isRunning = false;
    //Class activity;

    public ReceivingService() {
        super(ReceivingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
//        activity=(Class) intent.getExtras().get(COMMUNICATE);
        if (action.equals(ON_LISTEN)) {
            isRunning = true;
            try {
                MulticastSocket multicastSocket = createMulticastSocket();
                while (isRunning) {
                    DatagramPacket datagramPacket = createDatagramPacket();
                    multicastSocket.receive(datagramPacket);
                    //Log.e("recieved",datagramPacket.toString());
                    String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    long unixTime = System.currentTimeMillis();
                    if (received.equals("0.0")) {

                    } else if (received.equals("0.1")) {

                    }else {
                        received+="," + unixTime;
                    }

                    sendReceivedDataToMulticastMessageReceivedHandler(getHandlerMessenger(intent), received);
                }
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    private MulticastSocket createMulticastSocket() throws IOException {
        MulticastSocket multicastSocket = new MulticastSocket(getPort());
        multicastSocket.setNetworkInterface(getNetworkInterface());
        multicastSocket.joinGroup(new InetSocketAddress(getMulticastGroupAddress(), getPort()), getNetworkInterface());
        return multicastSocket;
    }

    private NetworkInterface getNetworkInterface() throws SocketException {
        return NetworkUtil.getWifiP2pNetworkInterface();
    }

    private InetAddress getMulticastGroupAddress() throws UnknownHostException {
        return NetworkUtil.getMulticastGroupAddress();
    }

    private int getPort() {
        return NetworkUtil.getPort();
    }

    private DatagramPacket createDatagramPacket() {
        byte[] buffer = new byte[1024];
        return new DatagramPacket(buffer, buffer.length);
    }
 /*   protected void handleMessage(final String msg){
        //gets the main thread
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // run this code in the main thread
                Log.e(MainActivity.TAG,"recieved:"+msg);
                Communicate communicate=(Communicate)activity;
                communicate.getRecievedText(msg);
            }
        });
    }*/
    private void sendReceivedDataToMulticastMessageReceivedHandler(Messenger handlerMessenger, String datagramPacket) throws RemoteException {
        Message handlerMessage = createHandlerMessage(datagramPacket);
        handlerMessenger.send(handlerMessage);
    }

    private Message createHandlerMessage(String receivedMessage) {
        Bundle receivedData = new Bundle();
        receivedData.putString(ReceiverHandler.RECEIVED_TEXT, receivedMessage);
        Message handlerMessage = new Message();
        handlerMessage.setData(receivedData);
        return handlerMessage;
    }

    private Messenger getHandlerMessenger(Intent intent) {
        return (Messenger) intent.getExtras().get(COMMUNICATE);
    }
}
