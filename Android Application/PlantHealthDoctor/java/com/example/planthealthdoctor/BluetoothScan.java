package com.example.planthealthdoctor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BluetoothScan extends AppCompatActivity {

    //TODO put DeviceControlActivity in here!!

    private TextView plantText;
    private TextView lightDataField;
    private TextView pHDataField;
    private TextView moistDataField;
    private TextView humDataField;
    private TextView tempDataField;
    private ImageView checkMark;
    private ImageView noCheckMark;
    private ImageView checkMarkLight;
    private ImageView noCheckMarkLight;
    private ImageView checkMarkPH;
    private ImageView checkMarkHum;
    private ImageView checkMarkMoist;
    private ImageView checkMarkTemp;
    private ImageView noCheckMarkPH;
    private ImageView noCheckMarkHum;
    private ImageView noCheckMarkMoist;
    private ImageView noCheckMarkTemp;
    private final static String TAG = "SCAN";
    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ExpandableListView mGattServicesList;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private int numDataRetrieved = 0;
    private final int maxDataToRetrieve = 5;
    private static final long SCAN_PERIOD = 20000;
    UUID[] uuids = new UUID[]{UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")};

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize BluetoothControlActivity");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                if (mGattCharacteristics != null) {
                    final ArrayList<BluetoothGattCharacteristic> chars =
                            mGattCharacteristics.get(2);
                    for (BluetoothGattCharacteristic characteristic : chars) {
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(BluetoothLeService.LIGHT) != null) {
                    String value = intent.getStringExtra(BluetoothLeService.LIGHT);
                    String text = "Light: " + value;
                    lightDataField.setText(text);
                    if (Double.valueOf(value) > 50)
                        checkMarkLight.setVisibility(View.VISIBLE);
                    else {
                        noCheckMarkLight.setVisibility(View.VISIBLE);
                        lightDataField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
                    }
                    numDataRetrieved++;
                }
                if (intent.getStringExtra(BluetoothLeService.MOISTURE) != null) {
                    String value = intent.getStringExtra(BluetoothLeService.MOISTURE);
                    String text = "Moisture: " + value;
                    moistDataField.setText(text);
                    numDataRetrieved++;
                }
                if (intent.getStringExtra(BluetoothLeService.HUMIDITY) != null) {
                    String value = intent.getStringExtra(BluetoothLeService.HUMIDITY);
                    String text = "Humidity: " + value;
                    humDataField.setText(text);
                    if (Double.valueOf(value) > 50)
                        checkMarkHum.setVisibility(View.VISIBLE);
                    else {
                        noCheckMarkHum.setVisibility(View.VISIBLE);
                        humDataField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
                    }
                    numDataRetrieved++;
                }
                if (intent.getStringExtra(BluetoothLeService.TEMPERATURE) != null) {
                    String value = intent.getStringExtra(BluetoothLeService.TEMPERATURE);
                    String text = "Temperature: " + value;
                    tempDataField.setText(text);
                    if (Double.valueOf(value) > 50)
                        checkMarkTemp.setVisibility(View.VISIBLE);
                    else {
                        noCheckMarkTemp.setVisibility(View.VISIBLE);
                        tempDataField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
                    }
                    numDataRetrieved++;
                }
                if (numDataRetrieved == maxDataToRetrieve) {
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    if(determineMajorityGood()) {
                        checkMark.setVisibility(View.VISIBLE);
                        plantText.setText("Your plant is doing great!");
                        plantText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    }
                    else {
                        noCheckMark.setVisibility(View.VISIBLE);
                        plantText.setText("Uh-Oh, your plant needs help!");
                        plantText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
                    }
                    lightDataField.setVisibility(View.VISIBLE);
                    pHDataField.setVisibility(View.VISIBLE);
                    moistDataField.setVisibility(View.VISIBLE);
                    humDataField.setVisibility(View.VISIBLE);
                    tempDataField.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    private boolean determineMajorityGood() {
        if (noCheckMarkLight.getVisibility()==View.VISIBLE) return false;
        if (noCheckMarkPH.getVisibility()==View.VISIBLE) return false;
        if (noCheckMarkHum.getVisibility()==View.VISIBLE) return false;
        if (noCheckMarkTemp.getVisibility()==View.VISIBLE) return false;
        if (noCheckMarkMoist.getVisibility()==View.VISIBLE) return false;
        return true;
    }

    final Runnable r = new Runnable() {
        @Override
        public void run() {
            if (numDataRetrieved != maxDataToRetrieve) {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                noCheckMark.setVisibility(View.VISIBLE);
                plantText.setText("Something went wrong...");
                plantText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        mHandler = new Handler();
        plantText = findViewById(R.id.plantText);
        checkMark = findViewById(R.id.checkMark);
        noCheckMark = findViewById(R.id.noCheckMark);
        lightDataField  = findViewById(R.id.light_data);
        checkMarkLight = findViewById(R.id.checkMarkLight);
        noCheckMarkLight = findViewById(R.id.noCheckMarkLight);
        pHDataField = findViewById(R.id.ph_data);
        checkMarkPH = findViewById(R.id.checkMarkPH);
        noCheckMarkPH = findViewById(R.id.noCheckMarkPH);
        moistDataField = findViewById(R.id.moist_data);
        checkMarkMoist = findViewById(R.id.checkMarkMoist);
        noCheckMarkMoist = findViewById(R.id.noCheckMarkMoist);
        humDataField = findViewById(R.id.hum_data);
        checkMarkHum = findViewById(R.id.checkMarkHum);
        noCheckMarkHum = findViewById(R.id.noCheckMarkHum);
        tempDataField = findViewById(R.id.temp_data);
        checkMarkTemp = findViewById(R.id.checkMarkTemp);
        noCheckMarkTemp = findViewById(R.id.noCheckMarkTemp);
        Intent predictionIntent = getIntent();

        System.out.println("WOWOWOW: " + predictionIntent.getStringExtra("pHPrediction"));

        // Initializes BluetoothControlActivity adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if ("bad".equals(predictionIntent.getStringExtra("pHPrediction"))) {
            r.run();
            scanLeDevice(false);
        } else {
            String value = predictionIntent.getStringExtra("pHPrediction");
            String text = "pH: " + value;
            pHDataField.setText(text);
            numDataRetrieved++;

            mHandler.postDelayed(r, 10000);

            // Start scanning
            scanLeDevice(true);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(uuids, leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if (device == null) return;
                    mDeviceName = device.getName();
                    mDeviceAddress = device.getAddress();
                    if (mScanning) {
                        bluetoothAdapter.stopLeScan(leScanCallback);
                        mScanning = false;
                    }
                    Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                }
            };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        lightDataField.setText(R.string.no_data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanLeDevice(true);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
