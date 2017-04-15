package developer.rajatjain.multicast_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import developer.rajatjain.multicast_direct.Handler.ReceiverHandler;
import developer.rajatjain.multicast_direct.Handler.SenderHandler;
import developer.rajatjain.multicast_direct.Interfaces.Communicate;
import developer.rajatjain.multicast_direct.Service.ReceivingService;
import developer.rajatjain.multicast_direct.Service.SenderService;


public class MainActivity extends AppCompatActivity implements Communicate,WifiP2pManager.ConnectionInfoListener, AdapterView.OnItemSelectedListener {

    public static final String TAG ="MainActivity" ;
    public static String P2pDeviceName;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    public IntentFilter mIntentFilter;
    private boolean isWifiP2pEnabled = false;
    ListView listView;
    LinearLayout testinglayout;
    TextView mDeviceName,status,mNetworkInfo,mRole,mAlert,mLog,mTab;
    String logtext="",sendtxt="";
    Spinner spinner;
    ArrayList devicename;
    ArrayAdapter adapter;
    public List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    Button mDiscover, mCreate_group,mSend,mConfig;
    int task=0;
    ToggleButton toggleButton;
    int isSender=0,isConnected=0,testnumber=0;
    RelativeLayout mSendingLayout;
    Intent mServiceIntent;
    WifiP2pDevice LastConnectedDevice;
    int NumberOfConnectedDevice;
    WifiP2pDeviceList ConnectedDeviceList;
    int Acks=0;//Todo Remove this var when class packet is implemented properly also make it zero while sending testcase
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createAndAcquireMulticastLock();

        mManager = (WifiP2pManager) getSystemService(this.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mDiscover=(Button)findViewById(R.id.discover);
        testinglayout=(LinearLayout)findViewById(R.id.testingLayout);
        mCreate_group =(Button)findViewById(R.id.create_group);
        mSend=(Button)findViewById(R.id.send);
        mLog=(TextView)findViewById(R.id.tvlog);
        mTab=(TextView)findViewById(R.id.tab2);
        listView=(ListView)findViewById(R.id.devicelist);
        status=(TextView)findViewById(R.id.device_status);
        spinner = (Spinner) findViewById(R.id.spinner_testcase);
        toggleButton=(ToggleButton)findViewById(R.id.togglestate);
        mConfig=(Button)findViewById(R.id.bconfig);
        mDeviceName=(TextView)findViewById(R.id.device_name);
        mNetworkInfo=(TextView)findViewById(R.id.network_info);
        mRole=(TextView)findViewById(R.id.role);
        mAlert=(TextView)findViewById(R.id.send_layout_text_view);
        mSendingLayout=(RelativeLayout)findViewById(R.id.sending_layout);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.Testcase_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        mConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  TODO add configuration mechanism
            }
        });

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    isSender=1;
                } else {
                    // The toggle is disabled
                    isSender=0;
                }
            }

        });
        mDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected==0) {
                    discover();
                }else {
                    disconnect();
                    killTestcase();
                }
            }
        });
        mCreate_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateGroup();
            }
        });
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Acks==0) {
                    task = 1;
                    StartSendingService(CreateSendingServiceIntent(task));

                }
                // 5 sec delay waiting for Acks
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(Acks==1){//todo number of connected device
                            task=2;
                            StartSendingService(CreateSendingServiceIntent(task));
                            StopReceivingService(CreateRecievingServiceIntent());
                            Acks=0;
                        }
                    }
                }, 5000);


            }
        });
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        long unixTime = System.currentTimeMillis();
        generateLog(testnumber+"_"+Unix_to_DateAndTime(unixTime),logtext);
        StopReceivingService(CreateRecievingServiceIntent());
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
        if(isWifiP2pEnabled==false){
            if (mManager != null && mChannel != null) {
                Toast.makeText(MainActivity.this,"Please enable Wi-Fi",Toast.LENGTH_LONG).show();
            } else {
                Logger(TAG, "channel or manager is null");
            }
        }
    }
    public  void  discover(){
        if (!isWifiP2pEnabled) {
            Toast.makeText(MainActivity.this, "P2P is off",
                    Toast.LENGTH_SHORT).show();
        }
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Discovering",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(MainActivity.this, "Error : "+reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onGetPeerList(WifiP2pDeviceList peerList) {
        List<WifiP2pDevice> refreshedPeers = new ArrayList<WifiP2pDevice>(peerList.getDeviceList());
        if (!refreshedPeers.equals(peers)) {
            peers.clear();
            peers.addAll(refreshedPeers);
            devicename=new ArrayList();
            for(int i=0;i<peers.size();i++){
                devicename.add(peers.get(i).deviceName);
            }
            ArrayAdapter arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,devicename);
            listView.setAdapter(arrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    connect(position,devicename.get(position).toString());
                }
            });
        }
    }

    @Override
    public void connection(String list) {
        //Todo add a list to ConnectedDevicelist for ACKS and all
        Toast.makeText(getBaseContext(),"connected ",Toast.LENGTH_LONG).show();
        isConnected=1;
        mDiscover.setText("disconnect");
        //status.setText("connected "+list);

    }

    @Override
    public void notifyThisDeviceChanged(Intent intent) {
        WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        P2pDeviceName=wifiP2pDevice.deviceName;
        mDeviceName.setText(wifiP2pDevice.deviceName);
        status.setText(getDeviceStatus(wifiP2pDevice.status));
        //mNetworkInfo.setText("ip:"+ NetworkUtil.getMyWifiP2pIpAddress());
    }

    @Override
    public void getRecievedText(String msg) {
        //ToDO java.lang.NullPointerException:
        // Attempt to invoke virtual method 'boolean java.util.ArrayList.contains(java.lang.Object)' on a null object reference
        if(msg.equals("ack")){
            Acks++; //Todo ISSUE if same device send multiple acks can be solved when we introduce packet class
        }

        if(msg.equals("0.0")) {
            // TODO
            Logger(TAG,"in test case 0.0");
            task=3;
            testnumber=0;
            StartSendingService(CreateSendingServiceIntent(task));
        }
        if(msg.equals("0.1")){
            // TODO
            task=3;
            testnumber=1;
            StartSendingService(CreateSendingServiceIntent(task));
        }

        Logger(TAG,"recieve2d:"+msg);
    }

    @Override
    public void notifyAboutSenderAction(String msg) {
        if (msg.equals("0")) {
            Logger(TAG,"sending...");
        }else if (msg.equals("1")){//sending stops on sent
            Logger(TAG,"sent");
            task=0;
            StopSendingService(CreateSendingServiceIntent(task));
        }else if (msg.equals("2")){//sending stops on sent
            Logger(TAG,"sent");
            task=0;
            StopSendingService(CreateSendingServiceIntent(task));
            StartReceiveService(CreateRecievingServiceIntent());
        }else{
            sendtxt=msg;
            generateTestResultfile(testnumber+"_sender",msg);
        }
    }

    private void connect(int position, final String name) {
        Toast.makeText(this,"connecting "+name,Toast.LENGTH_LONG).show();
        final WifiP2pDevice device=peers.get(position);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getBaseContext(),"connected to "+ device.deviceName,Toast.LENGTH_LONG).show();
                isConnected=1;
                LastConnectedDevice=device;
                onConnected();
                mDiscover.setText("disconnect");
                //success logic
            }


            @Override
            public void onFailure(int reason) {
                Toast.makeText(getBaseContext(),"Error :"+ reason,Toast.LENGTH_LONG).show();
                //failure logic
            }
        });
    }
    private void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }
                @Override
                public void onSuccess() {
                    Toast.makeText(getBaseContext(),"Succesfully disconnected",Toast.LENGTH_LONG).show();
                    mDiscover.setText("discover");
                    onDisconnected();
                    isConnected=0;

                }
            });
        }else {
            Logger(MainActivity.TAG,"something is null");
        }
    }
    private String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                onDisconnected();
                return getString(R.string.available);
            case WifiP2pDevice.INVITED:
                return getString(R.string.invited);
            case WifiP2pDevice.CONNECTED:
                onConnected();
                return getString(R.string.connected);
            case WifiP2pDevice.FAILED:
                return getString(R.string.failed);
            case WifiP2pDevice.UNAVAILABLE:
                return getString(R.string.unavailable);
            default:
                return getString(R.string.unknown);
        }
    }
    private void createAndAcquireMulticastLock() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock(TAG);
            multicastLock.acquire();
        }
    }
    public void onCreateGroup() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this,"Group created",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this,"Error creating group",Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null && wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            mRole.setText("Host");
            mNetworkInfo.setText("IP" + ": " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        } else if (wifiP2pInfo != null && wifiP2pInfo.groupFormed) {
            mRole.setText("Client");
        } else {
            mRole.setText("");
        }
    }

    public Intent CreateRecievingServiceIntent(){
        mServiceIntent = new Intent(MainActivity.this, ReceivingService.class);
        mServiceIntent.setAction(ReceivingService.ON_LISTEN);
        ReceiverHandler receiverHandler=new ReceiverHandler(this);
        mServiceIntent.putExtra(ReceivingService.COMMUNICATE,new Messenger(receiverHandler));
        return  mServiceIntent;
    }

    public void StartReceiveService(Intent intent){
        if(!ReceivingService.isRunning) {
            (MainActivity.this).startService(mServiceIntent);
            Logger(TAG,"multicast_receiver_service_started");
        }else {
            Logger("status", "already running");
        }
    }

    public void StopReceivingService(Intent intent){
        if(ReceivingService.isRunning){
            (MainActivity.this).stopService(mServiceIntent);
            Logger(TAG,"multicast_receiver_service_stopped");
            ReceivingService.isRunning=false;
        }
    }

    public Intent CreateSendingServiceIntent(int task){
        mServiceIntent = new Intent(MainActivity.this,SenderService.class);
        mServiceIntent.setAction(SenderService.ON_SEND);
        SenderHandler senderHandler=new SenderHandler(this);
        mServiceIntent.putExtra(SenderService.COMMUNICATE,new Messenger(senderHandler));
        mServiceIntent.putExtra(SenderService.TESTCASE,testnumber);
        mServiceIntent.putExtra(SenderService.TASK,task);
        return  mServiceIntent;
    }

    public void StartSendingService(Intent intent){

        if(!SenderService.isRunning) {
            SenderService.shouldContinue=true;
            (MainActivity.this).startService(mServiceIntent);
            Logger(TAG,"multicast_Sender_service_started");
        }else {
            Logger("status", "already running");
        }
    }

    public void StopSendingService(Intent intent){
        if(SenderService.isRunning){
            (MainActivity.this).stopService(mServiceIntent);
            Logger(TAG,"multicast_Sender_service_stopped");
            SenderService.isRunning=false;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this,parent.getItemAtPosition(position).toString(),Toast.LENGTH_LONG).show();
        testnumber=position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    public void Logger(String head,String s){
        Log.e(head,s);
        long unixTime = System.currentTimeMillis();
        logtext=logtext+"\n"+Unix_to_DateAndTime(unixTime)+"  "+s;
        mLog.setText(logtext);
    }
    public  void generateLog(String filename,String s){
        //TODO generating log file at the end of the test
        generateNoteOnSD(this,filename+"_log",s);

    }
    public void generateTestResultfile(String filename,String s){
        //ToDO generating test result file
        generateNoteOnSD(this,filename+"_txt",s);
    }
    public void onConnected(){
        listView.setVisibility(View.GONE);
        testinglayout.setVisibility(View.VISIBLE);
        StartReceiveService(CreateRecievingServiceIntent());
        mTab.setText("Testing");
    }
    public void onDisconnected(){
        listView.setVisibility(View.VISIBLE);
        testinglayout.setVisibility(View.GONE);
        StopReceivingService(CreateRecievingServiceIntent());
        mTab.setText("Availale devices");
    }
    private void killTestcase(){
        StopReceivingService(CreateRecievingServiceIntent());
        task=0;
        StopSendingService(CreateSendingServiceIntent(task));
        SenderService.shouldContinue=false;
    }
    public void  generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Multicast");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            String PATH_open = gpxfile.getAbsolutePath();
            Logger("MainActivity", "Path saved at " + PATH_open);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // TODO: 20-03-2017 network interface detection

    public String Unix_to_DateAndTime(long unix ){
        long miliseconds = (long)unix*1000;// its need to be in milisecond
        Date date = new java.util.Date(miliseconds);
        String result = new SimpleDateFormat("MM dd, yyyy hh:mma").format(date);
        return result;
    }
}
