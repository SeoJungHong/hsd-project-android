package com.seojung.hsdproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

/**
 * Created by SeoJung on 16. 5. 19..
 */
public class TabWeather extends Fragment {

    final String TAG = "TabWeather";

    GoogleApiClient googleApiClient;

    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvDescription;
    private TextView tvTemperature;
    private TextView tvRequestedTime;
    private String weatherResponse;
    private double latitude = 37.47831;
    private double longitude = 126.9585;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.tab_weather, container, false);

        tvLatitude = view.findViewById(R.id.latitude);
        tvLongitude = view.findViewById(R.id.longitude);
        tvDescription = view.findViewById(R.id.description);
        tvTemperature = view.findViewById(R.id.temperature);
        tvRequestedTime = view.findViewById(R.id.requestedTime);
        return view;
    }

    @Override
    public void onResume() {
        googleApiconnect();
        super.onResume();
    }

    private void googleApiconnect() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                // 이미 연결되어 있다면 바로 위치 가져온다.
                getLocation();
                return;
            } else if (googleApiClient.isConnecting()) {
                // 연결 중이면 패스!
                Log.d("locationClient", "connecting...");
                return;
            }
        }
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("googleApiClient", "connected");
                        getLocation();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        googleApiClient.connect();
    }

    private void getLocation() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                if (location != null) {
                    Log.d(TAG, String.format("update location ; (%f,%f)", location.getLatitude(), location.getLongitude()));
                    latitude = location.getLatitude();
                    tvLatitude.setText("lat : " + latitude);
                    longitude = location.getLongitude();
                    tvLongitude.setText("lon : " + longitude);
                }
                WeatherInfoAsyncTask task = new WeatherInfoAsyncTask();
                task.execute();
            }
        }
    }

    public void disconnect() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
            googleApiClient = null;
        }
    }

    public class WeatherInfoAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            requestGet();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            parseJson(weatherResponse);
            super.onPostExecute(aVoid);
        }
    }

    private void requestGet() {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("http://api.openweathermap.org/data/2.5/weather?appid=c39498ae9d482e57c246f9ffbf6175f5&units=metric")
                    .append("&lat=").append(Double.toString(latitude))
                    .append("&lon=").append(Double.toString(longitude));
//            URL weatherUrl = new URL("http://api.openweathermap.org/data/2.5/weather?appid=c39498ae9d482e57c246f9ffbf6175f5&units=metric&lat=37.47831&lon=126.9585");
            URL weatherUrl = new URL(builder.toString());
            Log.d(TAG, weatherUrl.toString());
            HttpURLConnection connection = (HttpURLConnection) weatherUrl.openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                connection.disconnect();
                weatherResponse = response.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseJson(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            Log.d(TAG, "parseJson : " + jsonObject);
            JSONArray weatherArray = (JSONArray)jsonObject.get("weather");
            int length = weatherArray.length();
            StringBuilder builder = new StringBuilder();
            while(length > 0) {
                builder.append(weatherArray.getJSONObject(length-1).get("description"));
                if (length != 1) {
                    builder.append(" & ");
                }
                length--;
            }
            String desc = builder.toString();
            tvDescription.setText(desc);

            JSONObject main = (JSONObject)jsonObject.get("main");
            int intTemp = 0;
            if (main.get("temp") instanceof Double) {
                intTemp = (int)(double)main.get("temp");
            } else if (main.get("temp") instanceof Integer) {
                intTemp = (int)main.get("temp");
            }
            String temp = String.valueOf(intTemp);
            tvTemperature.setText(temp + " \u2103");

            Calendar time = Calendar.getInstance();
            String formattedTime = String.format("Requested at %1$Tp %1$tI:%1$tM:%1$tS", time);
            tvRequestedTime.setText(formattedTime);
            // Set length to 3
            while(temp.length() != 3) {
                temp = "0" + temp;
            }
            // Send BluetoothService
            String formattedWeather = "{" + temp + desc + "}";

            // Bluetooth
            Log.d(TAG, "[send]" + formattedWeather);
            BluetoothService.sendData(formattedWeather);
            Log.d(TAG, "[send]~3");
            BluetoothService.sendData("~3");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
