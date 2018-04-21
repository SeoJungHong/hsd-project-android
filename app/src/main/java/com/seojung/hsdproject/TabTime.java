package com.seojung.hsdproject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

/**
 * Created by SeoJung on 16. 5. 19..
 */
public class TabTime extends Fragment {

    final String TAG = "TabTime";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.tab_time, container, false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        Calendar time = Calendar.getInstance();
        String formattedTime = String.format("|%1$tI%1$tM%1$tS", time);
        // Bluetooth
        Log.d(TAG, "[send]" + formattedTime);
        BluetoothService.sendData(formattedTime);
        Log.d(TAG, "[send]~1");
        BluetoothService.sendData("~1");
    }
}
