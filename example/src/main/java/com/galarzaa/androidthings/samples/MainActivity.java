package com.galarzaa.androidthings.samples;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.galarzaa.androidthings.Rc522;
import com.galarzaa.androidthings.samples.MVVM.VM.NPNHomeViewModel;
import com.galarzaa.androidthings.samples.MVVM.View.NPNHomeView;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements NPNHomeView {
    private Rc522 mRc522;
    RfidTask mRfidTask;
    private TextView mTagDetectedView;
    private TextView mTagUidView;
    private TextView mTagResultsView;
    private Button button;
    private Button readButton;

    private SpiDevice spiDevice;
    private Gpio gpioReset;
    private Gpio gpioCheck;
    private Gpio gpioArlet;
    private Gpio gpioDefault;

    private int countLed = 0;
    private int state = 0;
    private boolean isRead = false;
    private boolean isWrite = false;
    private boolean checkIn = false;
    private boolean stateTemp = true;

    private Handler mHandler = new Handler();


    private static final String SPI_PORT = "SPI0.0";
    private static final String PIN_RESET = "BCM25";
    private static final String PIN_CHECK = "BCM26";
    private static final String PIN_ARLET = "BCM19";
    private static final String PIN_DEFAULT = "BCM13";

    private static final String url = "http://demo1.chipfc.com/SensorValue/update?sensorid=7&sensorvalue=";

    String resultsText = "";

    private NPNHomeViewModel mHomeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTagDetectedView = (TextView)findViewById(R.id.tag_read);
        mTagUidView = (TextView)findViewById(R.id.tag_uid);
        mTagResultsView = (TextView) findViewById(R.id.tag_results);

        //Initiate NPNHomeView Object
        mHomeViewModel = new NPNHomeViewModel();
        mHomeViewModel.attach(this, this);

        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWrite = true;
                mRfidTask = new RfidTask(mRc522);
                mRfidTask.execute();
                ((Button)v).setText(R.string.wait);
            }
        });

        readButton = (Button) findViewById(R.id.readButton);
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRead = true;
                mRfidTask = new RfidTask(mRc522);
                mRfidTask.execute();
                ((Button)v).setText(R.string.wait);
            }
        });

        PeripheralManager pioService = PeripheralManager.getInstance();
        try {
            spiDevice = pioService.openSpiDevice(SPI_PORT);
            gpioReset = pioService.openGpio(PIN_RESET);
            gpioCheck = pioService.openGpio(PIN_CHECK);
            gpioArlet = pioService.openGpio(PIN_ARLET);
            gpioDefault = pioService.openGpio(PIN_DEFAULT);

            gpioCheck.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            gpioArlet.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            gpioDefault.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            mRc522 = new Rc522(spiDevice, gpioReset);
            mRc522.setDebugging(true);
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if(spiDevice != null){
                spiDevice.close();
            }
            if(gpioReset != null){
                gpioReset.close();
            }
            if(gpioDefault != null){
                gpioDefault.close();
            }
            if(gpioCheck != null){
                gpioCheck.close();
            }
            if(gpioArlet != null){
                gpioArlet.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccessUpdateServer(String message) {
        if (message.indexOf("OK") >= 0 && message.indexOf("200") >= 0 ) {
            Log.d("Send", "success!!!");
            Toast.makeText(this,"Success!!!!!!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onErrorUpdateServer(String message) {
        Log.d(TAG,"Upload server failed!!!!");
        Toast.makeText(this,"Upload Failed!",Toast.LENGTH_SHORT).show();
    }

    private class RfidTask extends AsyncTask<Object, Object, Boolean> {
        private static final String TAG = "RfidTask";
        private Rc522 rc522;

        RfidTask(Rc522 rc522){
            this.rc522 = rc522;
        }

        @Override
        protected void onPreExecute() {
            if (state >= 9) state = 0;
            button.setEnabled(false);
            readButton.setEnabled(false);
            mTagResultsView.setVisibility(View.GONE);
            mTagDetectedView.setVisibility(View.GONE);
            mTagUidView.setVisibility(View.GONE);
            resultsText = "";
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            rc522.stopCrypto();
            while(true){
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                //Check if a RFID tag has been found
                if(!rc522.request()){
                    continue;
                }
                //Check for collision errors
                if(!rc522.antiCollisionDetect()){
                    continue;
                }
                byte[] uuid = rc522.getUid();
                return rc522.selectTag(uuid);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String urlResult = "";
            if(!success){
                mTagResultsView.setText(R.string.unknown_error);
                return;
            }
            // Try to avoid doing any non RC522 operations until you're done communicating with it.
            byte address = Rc522.getBlockAddress(2,1);
            // Mifare's card default key A and key B, the key may have been changed previously
            byte[] key = {(byte)0xFF, (byte)0xFf, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
            state++;
            //byte[] key = getKey(state).getBytes();
            // Each sector holds 16 bytes
            // Data that will be written to sector 2, block 1
            //byte[] newData = {0x0f,0x0e,0x0d,0x0c,0x0b,0x0a,0x08,0x07,0x06,0x05,0x04,0x03,0x02,0x01,0x00};
            byte[] info = "Nam 0907 1512067".getBytes();
            // In this case, Rc522.AUTH_A or Rc522.AUTH_B can be used
            try {
                //We need to authenticate the card, each sector can have a different key
                boolean result = rc522.authenticateCard(Rc522.AUTH_A, address, key);
                Log.d(TAG,"KeyA: " + Arrays.toString(key));
                if (!result) {
                    mTagResultsView.setText(R.string.authetication_error);
                    //mHandler.post(mRunnable);
                    return;
                }
                if(isWrite == true) {
                    result = rc522.writeBlock(address, info);
                    if (!result) {
                        mTagResultsView.setText(R.string.write_error);
                        //mHandler.post(mRunnable);
                        return;
                    }
                    resultsText += "Sector written successfully";
                }
                byte[] buffer = new byte[16];
                //Since we're still using the same block, we don't need to authenticate again
                result = rc522.readBlock(address, buffer);
                if(!result){
                    mTagResultsView.setText(R.string.read_error);
                    //mHandler.post(mRunnable);
                    return;
                }
                //resultsText += "\nSector read successfully: "+ Rc522.dataToHexString(buffer);
                resultsText += "\nSector read successfully: "+ new String(buffer) +"\n" + Rc522.dataToHexString(buffer);
                rc522.stopCrypto();
                mTagResultsView.setText(resultsText);
                if(rc522.getUidString().indexOf("147") >= 0 & result == true) checkIn = true;
                else checkIn = false;
            }finally{
                if (isRead) {
                    if (checkIn) {
                        //Toast.makeText(MainActivity.this,"Possible to get in!",Toast.LENGTH_SHORT);
                        try {
                            gpioDefault.setValue(true);
                            gpioCheck.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    gpioDefault.setValue(false);
                                    gpioCheck.setValue(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // Do something after 5s = 5000m
                            }
                        }, 3000);
                    } else {
                        mHandler.post(mRunnable);
                    }
                }

                button.setEnabled(true);
                readButton.setEnabled(true);
                button.setText(R.string.write);
                readButton.setText(R.string.read);

                mTagUidView.setText(getString(R.string.tag_uid,rc522.getUidString()));
                String uid = rc522.getUidString("");
                Log.d(TAG,"UID: " + uid);
                urlResult = url + uid;
                mHomeViewModel.updateToServer(urlResult);
                //Log.d("URL: ",urlResult);
                urlResult = "";
                mTagResultsView.setVisibility(View.VISIBLE);
                mTagDetectedView.setVisibility(View.VISIBLE);
                mTagUidView.setVisibility(View.VISIBLE);


                isRead = false;
                isWrite = false;
            }
        }
    }

    private String getKey(int state){
        String key = "";
        switch (state){
            case 1:
                key = "106366";
                break;
            case 2:
                key = "182559";
                break;
            case 3:
                key = "456134";
                break;
            case 4:
                key = "539632";
                break;
            case 5:
                key = "562878";
                break;
            case 6:
                key = "504134";
                break;
            case 7:
                key = "577520";
                break;
            case 8:
                key = "413500";
                break;
            case 9:
                key = "890420";
                break;
            default:
                key = new String(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
                break;
        }
        return key;
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(gpioArlet == null){
                // Exit Runnable if the GPIO is already closed
                return;
            }
            countLed++;
            if(countLed>10) {
                countLed = 0;
                try {
                    gpioDefault.setValue(false);
                    gpioArlet.setValue(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            } else {
                try {
                    gpioDefault.setValue(true);
                    stateTemp = !stateTemp;
                    gpioArlet.setValue(stateTemp);
                    mHandler.postDelayed(mRunnable, 200);
                } catch (IOException e) {
                    Log.e(TAG, "Error on PeripheralIO API", e);
                }
            }
        }
    };


}
