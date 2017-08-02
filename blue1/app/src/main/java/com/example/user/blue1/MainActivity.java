package com.example.user.blue1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
//import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 0;
    private TextView txt;
    private Button btn;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int REQ_CODE_SETTING = 200;

    Set<BluetoothDevice> mDevices;
    ArrayList<String> result;

    int mPairedDeviceCount = 0;
    BluetoothAdapter mBluetoothAdapter;


    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;
    //String mDelimiter = "\n";
    //char mCharDelimiter = '\n';
    char mDelimiter = '\n';

    Thread mWorkerThread = null;
    byte[] readBuffer;
    int readBufferPosition;

    BluetoothDevice mRemoteDevice;
   int item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt = (TextView) findViewById(R.id.text);
        btn = (Button) findViewById(R.id.button);

        // hide the action bar
        //getActionBar().hide();

        //마이크 클릭시 실행된다.
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
                //sendData(txt.getText().toString());
                //txt.setText("");
            }
        });

        checkBlueTooth();
    }

    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;

        for (BluetoothDevice device : mDevices){
            if (name.equals(device.getName())){
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    void sendData(String msg){
        msg += mDelimiter;
        try{
            //mOutputStream.write(msg.getBytes());
            mOutputStream.write('1');
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "데이터 전송중 오류가 발생",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void connectToSelectedDevices(String selectedDeviceName){
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try{
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();

            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            beginListenForData();
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void beginListenForData() {
        final Handler handler = new Handler();

        readBuffer = new byte[1024];
        readBufferPosition = 0;

        mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()){

                    try{
                        int bytesAvailable = mInputStream.available();
                        if (bytesAvailable >0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i=0; i<bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == mDelimiter){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer,0,encodedBytes,0,encodedBytes.length);
                                    final String data = new String(encodedBytes,"US-ASCII");
                                    readBufferPosition = 0;

                                        handler.post(new Runnable(){
                                        public void run(){

                                        }
                                    });
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex){
                        finish();
                    }
                }
            }
        });

        mWorkerThread.start();
    }

    void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();

        if(mPairedDeviceCount == 0){
            Toast.makeText(getApplicationContext(),"페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            finish();
        }

        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        listItems.toArray(new CharSequence[listItems.size()]);

        if(result == listItems){
            connectToSelectedDevices(items[item].toString());
        }
    }

    void checkBlueTooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            finish();
        }
        else {
            if (!mBluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else{
                selectDevice();
            }
        }
    }

    //@Override
    protected  void onDestory() {
        try {
            mWorkerThread.interrupt();
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        }catch (Exception e) {}
        
        super.onDestroy();
    }

    /**
     * 음성 청취
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());  //잘 안되면
        //intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
               // getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            //Toast.makeText(getApplicationContext(),
                    //getString(R.string.speech_not_supported),
                   //Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 결과 처리
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            //마이크 음성 청취 동작 후 텍스트를 화면에 출력
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    //Toast.makeText(this, "음성인식 결과가 있습니다.", Toast.LENGTH_SHORT).show();
                    result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txt.setText(result.get(0));

                    selectDevice();

                    //여기서 블루투스나 소켓으로 해당 명령어를 아두이노로 보내면 되나?

                    List<String> listItems = new ArrayList<String>();
                    for (BluetoothDevice device : mDevices) {
                        listItems.add(device.getName());
                    }
                    /*if(result == listItems){
                        sendData("1");
                        //txt.setText("");
                    }*/



                }
                break;
            }
            //연결 설정 액티비티 갔다와서(아두이노 연결인지 라즈베리파이 연결인지에 따라 액션
           /* case REQ_CODE_SETTING: {
                String settingResult = data.getStringExtra("device");
                txt.setText(settingResult + "와 연결합니다.");
                Toast.makeText(this, "설정이 완료 되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            }
        }*/
    }

    //상단 설정메뉴 아이콘 클릭시
   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_button) {
            //수행할 액션 넣을것. 새 액티비티를 열어서 접속세팅을 하도록 한다.
            //Toast.makeText(this, "설정 액티비티를 열겠습니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, SubSettings.class);
            startActivityForResult(intent, REQ_CODE_SETTING);
            overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);
            return true;
        }
        return super.onOptionsItemSelected(item);*/
    }

}