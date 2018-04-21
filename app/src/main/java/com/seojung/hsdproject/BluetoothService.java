package com.seojung.hsdproject;


import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by SeoJung on 16. 5. 24..
 */
public class BluetoothService extends Service {

    static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "BluetoothService";

    static MainActivity mActivity;

    // Intent request code
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    static BluetoothAdapter btAdapter;
    static final Handler mHandler = new Handler();
    static BluetoothSocket mSocket;
    static OutputStream mOutputStream;
    static InputStream mInputStream;
    static Thread mWorkerThread;

    private final static char mDelimiter = '\n';
    static int readBufferPosition = 0; // 버퍼 내 수신 문자 저장 위치
    static BluetoothListener bluetoothListener;

    // Protocols
    private final static String CHANGE_MODE_TO_TIME = "121";
    private final static String CHANGE_MODE_TO_HEART = "122";
    private final static String CHANGE_MODE_TO_WEATHER = "123";
    private final static String CHANGE_MODE_TO_MUSIC = "124";
    private final static String ALARM = "!";
    private final static String HEART_VALUE = "[";
    private final static String PREV_SONG = "<";
    private final static String PLAY_OR_PAUSE = "=";
    private final static String NEXT_SONG = ">";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "OnBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "OnCreate");
        super.onCreate();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        setForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartCommand");
        return START_STICKY;
    }

    private void setForeground() {
        startForeground(5001, buildDefaultNotification());
    }

    private Notification buildDefaultNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_MIN)
                .setWhen(0)
                .setContentTitle("Project")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText("bluetooth service is running");
        return builder.build();
    }

    public static boolean isEnabled() {
        if (mOutputStream != null && mInputStream != null && mSocket.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public interface BluetoothListener {
        void OnEnabled();
        void OnDisabled();
    }

    public static void setBluetoothListener(BluetoothListener listener) {
        bluetoothListener = listener;
    }

    public static void setActivity(Activity activity) {
        mActivity = (MainActivity)activity;
    }

    /**
     * Check the enabled BluetoothService
     */
    public static void enableBluetooth() {
        Log.d(TAG, "Check the enabled BluetoothService");
        if(btAdapter.isEnabled()) {
            // 기기의 블루투스 상태가 On인 경우
            Log.d(TAG, "BluetoothService Enable Now");
            selectDevice();
        } else {
            // 기기의 블루투스 상태가 Off인 경우
            Log.d(TAG, "BluetoothService Enable Request");
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);
        }
    }

    public static void disableBluetooth() {
        bluetoothListener.OnDisabled();
        disconnect();
    }

    public static void selectDevice() {
        final Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() == 0 ) {
            //  페어링 된 장치가 없는 경우
            Log.e(TAG, "No Paired Devices");
        }
        // 페어링 된 블루투스 장치의 이름 목록 작성
        final List<String> listItems = new ArrayList<>();
        final Map<String, BluetoothDevice> deviceMap = new HashMap<>();
        for(BluetoothDevice device : pairedDevices) {
            listItems.add(device.getName());
            deviceMap.put(device.getName(), device);
        }
        listItems.add("취소");    // 취소 항목 추가
        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("블루투스 장치 선택");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == pairedDevices.size()) {
                    // 연결할 장치를 선택하지 않고 '취소'를 누른 경우
                    dialog.dismiss();
                } else {
                    // 선택한 장치와 연결을 시도함
                    connectToSelectedDevice(deviceMap.get(items[which]));
                }
            }
        });
        builder.setCancelable(false);    // 뒤로 가기 버튼 사용 금지
        builder.create().show();
    }

    private static void connectToSelectedDevice(BluetoothDevice device) {
        Log.d(TAG, "connectToSelectedDevice : " + device.getName());
        try {
            // 소켓 생성
            mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            // RFCOMM 채널을 통한 연결
            mSocket.connect();

            // 데이터 송수신을 위한 스트림 열기
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            bluetoothListener.OnEnabled();
            beginListenForData();
            Toast.makeText(mActivity, "Connected to bluetooth!", Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            // 블루투스 연결 중 오류 발생
            e.printStackTrace();
            Toast.makeText(mActivity, "Cannot connect to bluetooth. Try again", Toast.LENGTH_LONG).show();
        }
    }

    // 블루투스 송신
    public static void sendData(String msg) {
        if (isEnabled()) {
            msg += mDelimiter;// 문자열 종료 표시
            try {
                mOutputStream.write(msg.getBytes());// 문자열 전송
            } catch (Exception e) {
                // 문자열 전송 도중 오류가 발생한 경우.
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "send data : " + msg + " failed because bluetooth is not enabled");
        }
    }

    // 블루투스 수신.
    private static void beginListenForData() {
        Log.d(TAG, "beginListenForData");
        final byte[] readBuffer = new byte[1024];
        // 문자열 수신 쓰레드
        mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    try {
                        int bytesAvailable = mInputStream.available(); // 수신 데이터 확인
                        if(bytesAvailable > 0) { // 데이터가 수신된 경우
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i = 0 ; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if(b == mDelimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    mHandler.post(new Runnable() {
                                        public void run() {
                                            Log.d(TAG, "received data : " + data);
                                            if (data.equals(ALARM)) {
                                                // 알람은 아무 모드에서나 발동하도록
                                                Toast.makeText(mActivity, "Alarm!!!", Toast.LENGTH_LONG).show();
                                                Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
                                                vibrator.vibrate(2000);
                                            }
                                            if (mActivity.CUR_MODE == MainActivity.Mode.TIME) {
                                                processTime(data);
                                            } else if (mActivity.CUR_MODE == MainActivity.Mode.HEART) {
                                                processHeart(data);
                                            } else if (mActivity.CUR_MODE == MainActivity.Mode.WEATHER) {
                                                processWeather(data);
                                            } else if (mActivity.CUR_MODE == MainActivity.Mode.MUSIC) {
                                                processMusic(data);
                                            }
                                        }
                                    });
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException e) {
                        // 데이터 수신 중 오류 발생.
                        e.printStackTrace();
                    }
                }
            }
        });
        mWorkerThread.start();
    }

    private static void processTime(String data) {
        // change app
        if (data.equals(CHANGE_MODE_TO_HEART)) {
            mActivity.fragmentReplace(MainActivity.Mode.HEART);
        } else if (data.equals(CHANGE_MODE_TO_WEATHER)) {
            mActivity.fragmentReplace(MainActivity.Mode.WEATHER);
        } else if (data.equals(CHANGE_MODE_TO_MUSIC)) {
            mActivity.fragmentReplace(MainActivity.Mode.MUSIC);
        }
    }

   public static void processHeart(String data) {
        // change app
        if (data.equals(CHANGE_MODE_TO_TIME)) {
            Log.d(TAG, "[heart]change app to time");
            mActivity.fragmentReplace(MainActivity.Mode.TIME);
        } else if (data.equals(CHANGE_MODE_TO_WEATHER)) {
            Log.d(TAG, "[heart]change app to weather");
            mActivity.fragmentReplace(MainActivity.Mode.WEATHER);
        } else if (data.equals(CHANGE_MODE_TO_MUSIC)) {
            mActivity.fragmentReplace(MainActivity.Mode.MUSIC);
        } else {
            // set heart value
            // 안 바꿀 때에만 아래 코드 실행. 안그러면 cast error 발생
            if (data.startsWith(HEART_VALUE)) {
                if (data.length() > 1) {
                    char temp = data.charAt(1);
                    int ascii = (int) temp;
                    ((TabHeart) mActivity.currentFragment).setY(ascii);
                } else {
                    Log.e(TAG, "heart value - Empty String?? : " + data);
                }
            }
        }
    }

    private static void processWeather(String data) {
        // change app
        if (data.equals(CHANGE_MODE_TO_TIME)) {
            mActivity.fragmentReplace(MainActivity.Mode.TIME);
        } else if (data.equals(CHANGE_MODE_TO_HEART)) {
            mActivity.fragmentReplace(MainActivity.Mode.HEART);
        } else if (data.equals(CHANGE_MODE_TO_MUSIC)) {
            mActivity.fragmentReplace(MainActivity.Mode.MUSIC);
        }
    }

    private static void processMusic(String data) {
        // change app
        if (data.equals(CHANGE_MODE_TO_TIME)) {
            mActivity.fragmentReplace(MainActivity.Mode.TIME);
        } else if (data.equals(CHANGE_MODE_TO_HEART)) {
            mActivity.fragmentReplace(MainActivity.Mode.HEART);
        } else if (data.equals(CHANGE_MODE_TO_WEATHER)) {
            mActivity.fragmentReplace(MainActivity.Mode.WEATHER);
        } else {
            // 안 바꿀 때에만 아래 코드 실행. 안그러면 cast error 발생
            TabMusic tabMusic = ((TabMusic) mActivity.currentFragment);
            if (data.equals(PREV_SONG)) {
                tabMusic.playPrev();
                ((TabMusic) mActivity.currentFragment).playPrev();
            }
            if (data.equals(PLAY_OR_PAUSE)) {
                if (tabMusic.isPlaying()) {
                    tabMusic.pause();
                } else {
                    tabMusic.start();
                }
            }
            if (data.equals(NEXT_SONG)) {
                tabMusic.playNext();
            }
        }
    }

    public static void disconnect() {
        try {
            if (mWorkerThread != null) {
                mWorkerThread.interrupt();   // 데이터 수신 쓰레드 종료
            }
            if (mSocket != null) {
                mInputStream.close();
                mOutputStream.close();
                mSocket.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
