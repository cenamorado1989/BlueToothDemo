package enamorado.mydemo.bluetoothdemo3;

import android.Manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button scanButton;
    ListView scanListView;
    TextView scanText;
    ImageView mBlueIv;
    Button enableButton;
    Button btButton;
    IntentFilter scanIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);

    ArrayList<String> stringArrayList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice btArray[];

    SendReceive sendReceive;

    Button listen,send, listDevices;
    ListView listView;
    TextView msg_box, status;
    EditText writeMsg;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    private static final String APP_NAME = "BTCHAT";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    //CONNECTIN DEVICES PART
    TextView textView;//Handler text view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanButton = (Button) findViewById(R.id.scanButton);
        scanListView = (ListView) findViewById(R.id.list);
        enableButton = (Button) findViewById(R.id.enableButton);
        scanText = (TextView) findViewById(R.id.textView);
        textView = (TextView) findViewById(R.id.textView2);
        btButton = (Button) findViewById(R.id.btButton);
        mBlueIv = (ImageView) findViewById(R.id.imageView);

        findViewByIdes();
        

        if(!adapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
            mBlueIv.setImageResource(R.drawable.ic_action_on);
        }
        ImplementListeners();


        //Thread2 t = new Thread2();
        //t.start();

        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override // ENABLE DISCOVERABILITY
            public void onClick(View v) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
                startActivity(discoverableIntent);
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override //SCAN FOR DEVICES
            public void onClick(View v) {
                permissionCheck();
                adapter.startDiscovery();
            }
        });

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, intentFilter);
        registerReceiver(scanModeReceiver, scanIntentFilter);

        arrayAdapter = new ArrayAdapter<String>(getApplication(), android.R.layout.simple_list_item_1, stringArrayList);
        scanListView.setAdapter(arrayAdapter);

    }       //END ON CREATE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private void ImplementListeners() {

        btButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!adapter.isEnabled()){
                    adapter.enable();
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                } else {
                    adapter.disable();
                    mBlueIv.setImageResource(R.drawable.ic_action_off);
                }
            }
        });

        listDevices.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt = adapter.getBondedDevices();
                String[] strings = new String[bt.size()];
                btArray = new BluetoothDevice[bt.size()];
                int index = 0;

                if(bt.size()>0){

                    for(BluetoothDevice device : bt){
                        btArray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplication(), android.R.layout.simple_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        listen.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                ClientClass clientClass = new ClientClass(btArray[i]);
                clientClass.start();

                status.setText("Connecting");
            }
        });

        send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String string = String.valueOf(writeMsg.getText());
                sendReceive.write(string.getBytes());
            }
        });

    }

    private void findViewByIdes(){
        listen = (Button) findViewById(R.id.listenButton);
        send = (Button) findViewById(R.id.sendButton);
        listView = (ListView) findViewById(R.id.deviceList);
        msg_box = (TextView) findViewById(R.id.textView2);
        status = (TextView) findViewById(R.id.textView4);
        writeMsg = (EditText) findViewById(R.id.editText);
        listDevices = (Button) findViewById(R.id.listButton);
    }

    Handler handler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){

                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0,msg.arg1);
                    msg_box.setText(tempMsg);
                    break;
            }

            return true;
        }
    });

    private class Thread2 extends Thread {

        public void run(){
            for(int i = 0; i < 50; i++){
                Message message = Message.obtain();
                message.arg1 = i;
                handler.sendMessage(message);

                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)){

                int modeValue = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                if(modeValue == BluetoothAdapter.SCAN_MODE_CONNECTABLE){
                    scanText.setText("The device is not in discoverable mode but can still receive connections.");
                } else if (modeValue == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                    scanText.setText("The device is in discoverable mode.");
                } else if (modeValue == BluetoothAdapter.SCAN_MODE_NONE){
                    scanText.setText("The device is not in discoverable mode and cannot receive connections.");
                } else {
                    scanText.setText("Error.");
                }
            }
        }
    };
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                stringArrayList.add(device.getName());
                arrayAdapter.notifyDataSetChanged();
            }
        }
    };
    public void permissionCheck() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {

            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }
    }
    private class ServerClass extends Thread{

        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket = null;

            while(socket == null){
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket != null){
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();

                    break;
                }
            }
        }
    }   // END SERVER CLASS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private class ClientClass extends Thread {

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1){

            device = device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run(){

            try {
                adapter.cancelDiscovery();
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread{

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;

        }
        public void run(){

            byte buffer[] = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){

            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}//END CLASS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
