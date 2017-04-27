package developer.rajatjain.multicast_direct.Service;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
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
 * Created by Rajat Jain on 09-03-2017.
 */

public class SenderService extends IntentService{

    private static final String TAG = "MainActivity";
    public static final String BROADCAST_ACTION="BROADCAST_ACTION";
    public static final String EXTENDED_DATA_STATUS = "STATUS";
    public static volatile boolean shouldContinue = true;
    public static volatile boolean shouldStartTest = false;
    public final static String ON_SEND ="ON_SEND";
    public final static String COMMUNICATE="COMMUNICATE";
    public final static String TESTCASE="TESTCASE";
    public final static String TASK="TASK";
    public static final String STATE_ACTION_STARTED = "0";
    public static final String STATE_ACTION_COMPLETE = "1";
    public static final String STATE_TEST_COMPLETE = "2";
    String sendtext="";
    public static boolean isRunning = false;
    public SenderService() {
        super("sender");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(ON_SEND)) {
            isRunning = true;
            int task=getTask(intent);
            if(task==1){
                SendTestcaseConfig(intent, getTestcase(intent));
            }else if(task==2){
                int testcase = getTestcase(intent);

                if (testcase == 0)
                    SendPackets(intent, 200, (float) 0.5); //200*0.5 = 100 Seconds
                else
                    SendPackets(intent, 2000, (float) 0.01); //2000*0.01= 20 Seconds but 2000 packets already reached

            }else if(task==3){
                SendAck(intent);
            }

        }
    }
    private void SendPackets(Intent intent,int packets,float timeInterval) {

        try {
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), STATE_ACTION_STARTED);
            MulticastSocket multicastSocket = createMulticastSocket();
            /*String messageToBeSent = "hi";
            Log.e("Kush", "sending");
            DatagramPacket datagramPacket = new DatagramPacket(messageToBeSent.getBytes(), messageToBeSent.length(), getMulticastGroupAddress(), getPort());
            multicastSocket.send(datagramPacket);
            Log.e("Kush", "sending2");
            Log.e("Kush", "sent");*/
            int count = 0;
            String msg;
            int t=packets;
            while (shouldContinue) {
                long unixTime = System.currentTimeMillis();
                count += 1;
                msg = unixTime + "," + count;
                if (count == packets){
                    msg = "STOP";
                    DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), getMulticastGroupAddress(), getPort());
                    multicastSocket.send(datagramPacket);
                    multicastSocket.send(datagramPacket);
                    multicastSocket.send(datagramPacket);
                    multicastSocket.send(datagramPacket);
                    multicastSocket.send(datagramPacket);
                    multicastSocket.send(datagramPacket);
                }

                DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), getMulticastGroupAddress(), getPort());
                multicastSocket.send(datagramPacket);
                sendtext=sendtext+"."+msg;
                final String finalMsg = msg;
                Log.d("SenderService","Sending" + msg);
                if (count == packets) {
                    break;
                }
                Thread.sleep((long) (timeInterval*1000));
            }
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent),sendtext);
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), STATE_TEST_COMPLETE);

            shouldStartTest=false;
        }catch (IOException |RemoteException ioException) {
            Log.e(TAG, ioException.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void SendTestcaseConfig(Intent intent,int testcase) {
        try {
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), STATE_ACTION_STARTED);
            MulticastSocket multicastSocket = createMulticastSocket();
            String msg = "0." + testcase;
            DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), getMulticastGroupAddress(), getPort());
            multicastSocket.send(datagramPacket);
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), sendtext);
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), STATE_ACTION_COMPLETE);


        } catch (IOException | RemoteException ioException) {
            Log.e(TAG, ioException.toString());
        }
    }
    public void SendAck(Intent intent) {
        try {
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), STATE_ACTION_STARTED);
            MulticastSocket multicastSocket = createMulticastSocket();
            //String msg = MainActivity.P2pDeviceName;
            String msg = "ack";
            DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), getMulticastGroupAddress(), getPort());
            multicastSocket.send(datagramPacket);
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), sendtext);
            sendDataToMulticastMessageSenderHandler(getHandlerMessenger(intent), STATE_ACTION_COMPLETE);


        } catch (IOException | RemoteException ioException) {
            Log.e(TAG, ioException.toString());
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
    private void sendDataToMulticastMessageSenderHandler(Messenger handlerMessenger, String status) throws RemoteException {
        Message handlerMessage = createHandlerMessage(getStatusText(status));
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
    private int getTestcase(Intent intent){
        return (int) intent.getExtras().get(TESTCASE);
    }
    private int getTask(Intent intent){
        return (int) intent.getExtras().get(TASK);
    }
    private String getStatusText(String status) {
        return status;
    }


}
