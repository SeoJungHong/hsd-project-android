package com.seojung.hsdproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends FragmentActivity {

    final String TAG = "MainActivity";

    private Button btConnect;
    private Button btDisconnect;

    public Mode CUR_MODE = Mode.TIME;
    public Fragment currentFragment;

    public enum Mode {
        TIME,
        HEART,
        WEATHER,
        MUSIC
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = new Intent(this, BluetoothService.class);
        startService(i);

        BluetoothService.setBluetoothListener(new BluetoothService.BluetoothListener() {
            @Override
            public void OnEnabled() {
                btConnect.setEnabled(false);
                btDisconnect.setEnabled(true);
            }

            @Override
            public void OnDisabled() {
                btConnect.setEnabled(true);
                btDisconnect.setEnabled(false);
            }
        });
        BluetoothService.setActivity(this);
        initView();
    }

    private void initView() {
        btConnect = findViewById(R.id.connect_bluetooth);
        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothService.enableBluetooth();
            }
        });
        btDisconnect = findViewById(R.id.disconnect_bluetooth);
        btDisconnect.setEnabled(false);
        btDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBluetooth();
            }
        });
        // Determine initial status
        if (BluetoothService.isEnabled()) {
            btConnect.setEnabled(false);
            btDisconnect.setEnabled(true);
        } else {
            btConnect.setEnabled(true);
            btDisconnect.setEnabled(false);
        }

        Button timeButton = findViewById(R.id.bt_oneFragment);
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CUR_MODE != Mode.TIME) {
                    fragmentReplace(Mode.TIME);
                }
            }
        });

        Button heartButton = findViewById(R.id.bt_twoFragment);
        heartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CUR_MODE != Mode.HEART) {
                    Log.d(TAG, "[send]~2");
                    BluetoothService.sendData("~2");
                    fragmentReplace(Mode.HEART);
                }
            }
        });

        Button weatherButton = findViewById(R.id.bt_threeFragment);
        weatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CUR_MODE != Mode.WEATHER) {
                    fragmentReplace(Mode.WEATHER);
                }
            }
        });

        findViewById(R.id.bt_fourFragment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CUR_MODE != Mode.MUSIC) {
                    fragmentReplace(Mode.MUSIC);
                }
            }
        });
        //Default : TIME
        fragmentReplace(Mode.TIME);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothService.REQUEST_ENABLE_BT:
                // When the request to enable BluetoothService returns
                if (resultCode == Activity.RESULT_OK) {
                    // 확인 눌렀을 때
                    BluetoothService.selectDevice();
                } else {
                    // 취소 눌렀을 때
                    Log.d(TAG, "BluetoothService is not enabled");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void fragmentReplace(Mode mode) {
        Fragment newFragment;
        Log.d(TAG, "fragmentReplace to " + mode.name());
        newFragment = getFragment(mode);

        // replace fragment
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();

        transaction.replace(R.id.ll_fragment, newFragment);
        // Commit the transaction
        transaction.commit();
    }

    private Fragment getFragment(Mode mode) {
        CUR_MODE = mode;
        Fragment newFragment = null;
        switch (mode) {
            case TIME:
                newFragment = new TabTime();
                break;
            case HEART:
                newFragment = new TabHeart();
                break;
            case WEATHER:
                newFragment = new TabWeather();
                break;
            case MUSIC:
                newFragment = new TabMusic();
                break;
            default:
                Log.d(TAG, "Unhandled case");
                break;
        }
        currentFragment = newFragment;
        return newFragment;
    }

    // For TabMusic
    public void songPicked(View view) {
        ((TabMusic) currentFragment).songPicked(view);
    }

    @Override
    protected void onResume() {
        if (BluetoothService.isEnabled()) {
            btConnect.setEnabled(false);
            btDisconnect.setEnabled(true);
        } else {
            btConnect.setEnabled(true);
            btDisconnect.setEnabled(false);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        // 명시적으로 없애줘야 leak 에러 안생김.
        if (CUR_MODE == Mode.MUSIC) {
            Log.d(TAG, "unbindService for music");
            unbindService(((TabMusic) currentFragment).musicConnection);
        }
        super.onDestroy();
    }

    public void disableBluetooth() {
        BluetoothService.disableBluetooth();
        stopService(new Intent(this, BluetoothService.class));
    }
}