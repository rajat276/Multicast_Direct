package developer.rajatjain.multicast_direct.Handler;

import android.os.Handler;
import android.os.Message;

import developer.rajatjain.multicast_direct.Interfaces.Communicate;

/**
 * Created by Rajat Jain on 09-03-2017.
 */

public class SenderHandler extends Handler {
    public static final String RECEIVED_TEXT = "RECEIVED_TEXT";
    //private MulticastMessageReceivedListener multicastMessageReceivedListener;
    private Communicate communicate;

    public SenderHandler(Communicate communicate) {
        //this.multicastMessageReceivedListener = multicastMessageReceivedListener;
        this.communicate=communicate;
    }

    @Override
    public void handleMessage(Message messageFromMulticastMessageReceiverService) {
        String multicastMessage = createMulticastMessage(messageFromMulticastMessageReceiverService);
        //multicastMessageReceivedListener.onMulticastMessageReceived(multicastMessage);
        communicate.notifyAboutSenderAction(multicastMessage);
    }

    private String createMulticastMessage(Message messageFromMulticastMessageReceiverService) {
        String receivedText = getReceivedText(messageFromMulticastMessageReceiverService);
        String multicastMessage = receivedText;
        return multicastMessage;
    }

    private String getReceivedText(Message messageFromReceiverService) {
        return messageFromReceiverService.getData().getString(RECEIVED_TEXT);
    }
}
