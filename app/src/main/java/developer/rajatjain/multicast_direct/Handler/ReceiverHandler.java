package developer.rajatjain.multicast_direct.Handler;

import android.os.Handler;
import android.os.Message;

import developer.rajatjain.multicast_direct.Interfaces.Communicate;


/**
 * Created by Rajat Jain on 06-03-2017.
 */

public class ReceiverHandler extends Handler {
    public static final String RECEIVED_TEXT = "RECEIVED_TEXT";
    private Communicate communicate;

    public ReceiverHandler(Communicate communicate) {
        this.communicate=communicate;
    }

    @Override
    public void handleMessage(Message messageFromMulticastMessageReceiverService) {
        String multicastMessage = createMulticastMessage(messageFromMulticastMessageReceiverService);
        communicate.getRecievedText(multicastMessage);
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

