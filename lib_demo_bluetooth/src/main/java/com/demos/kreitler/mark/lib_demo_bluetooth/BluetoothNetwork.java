
package com.demos.kreitler.mark.lib_demo_bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Mark on 10/30/2016.
 */

public class BluetoothNetwork {
    private final String SPHERO_MULITPLAYER_UUID= "a44961a4-4b01-413d-be7b-6d6105a095cc";
    private final String SEPERATOR              = "~";

    private BluetoothAdapter BA                 = null;
    private Set<BluetoothDevice> pairedDevices  = null;
    private Activity mainActivity               = null;
    private ArrayList<String> arrayAdapter      = null;
    private boolean bDiscovering                = false;

    public static BluetoothNetwork Instance = null;

    public BluetoothNetwork(Activity owningActivity) {
        BA = BluetoothAdapter.getDefaultAdapter();
        mainActivity = owningActivity;

        Instance = this;
    }

    public void Destroy() {
        StopDiscovery();
    }

    // Activate standard Bluetooth for the main activity.
    public void Enable() {
        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mainActivity.startActivityForResult(turnOn, 0);

        if (BA != null && !BA.isEnabled()) {
            BA.enable();
        }
    }

    public String AddressFromName(String name) {
        String address = null;
        String ucaseName = name.toUpperCase();

        for (int i=0; i<arrayAdapter.size(); ++i) {
            String[] btInfo = arrayAdapter.get(i).split(SEPERATOR);
            if (btInfo.length > 0) {
                if (btInfo[0].toUpperCase() == ucaseName) {
                    address = btInfo[1];
                    break;
                }
            }
        }

        return address;
    }

    // Search for new devices.
    public boolean DiscoverDevices() {
        if (BA != null && BA.isEnabled()) {
            BA.startDiscovery();
            arrayAdapter = new ArrayList<String>();

            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mainActivity.registerReceiver(discoveryReceiver, filter); // Don't forget to unregister during onDestroy
            bDiscovering = true;
        }

        return bDiscovering;
    }

    public void StopDiscovery() {
        if (BA != null && BA.isEnabled()) {
            BA.cancelDiscovery();

            if (mainActivity != null && bDiscovering) {
                mainActivity.unregisterReceiver(discoveryReceiver);
            }
        }
    }

    public boolean AllowDiscovery() {
        boolean bCanAllow = false;

        if (BA != null && BA.isEnabled() && mainActivity == null) {
            Intent makeDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            mainActivity.startActivityForResult(makeDiscoverable, 0);
            bCanAllow = true;
        }

        return bCanAllow;
    }

    // Retrieve a list of paired devices.
    public Set<BluetoothDevice> GetPairedDevices() {
        pairedDevices = BA != null ? BA.getBondedDevices() : null;

        return pairedDevices;
    }

    public ArrayList<String> GetDiscoveredDevices() {
        return arrayAdapter;
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add the name and address to an array adapter to show in a ListView
                arrayAdapter.add(device.getName() + SEPERATOR + device.getAddress());
            }
        }
    };
}
