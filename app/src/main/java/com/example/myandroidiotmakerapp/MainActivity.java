package com.example.myandroidiotmakerapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastLocation;
    private LocationCallback mLocationCallback;
    final private int REQUEST_PERMISSIONS_FOR_LOCATION_UPDATES = 101;

    // DeviceTask 를 통해 iotMakers로 보내줄 위도 / 경도 저장할 자료형
    private Map<String, Double> rows = new HashMap<String, Double>();
    private DeviceTask deviceTask;

    // gyroscope 센서 사용을 위함
    private SensorManager sensorManager;
    private Sensor gyroSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button start_location_update_button = (Button) findViewById(R.id.device_button_start);
        start_location_update_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdates();
            }
        });

        Button stop_location_update_button = (Button) findViewById(R.id.device_button_stop);
        stop_location_update_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdates();
            }
        });

        // Location 정보를 IoTMaker에 전송하기 위한 버튼
        Button send_location_button = (Button) findViewById(R.id.device_button);
        send_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDeviceTask(); // SEND LOCATION TO IOTMAKERS 버튼이 눌려지면 해당 함수 호출

            }
        });

        // ----------자이로스코프 센서 추가 코드-----------
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // 센서 정보 계측 시 리스너를 등록한다.
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //-----------------------------------------

    }

    //---------자이로 스코프 센서 추가 코드-----------
    @Override
    protected void onDestroy() {
        // 화면 종료 시 리스너를 해제한다.
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 센서 값 계측 시 송신 할 HashMap 객체에 값을 넣는다.
        rows.put("gyrox", (double) event.values[0]);
        rows.put("gyroy", (double) event.values[1]);
        rows.put("gyroz", (double) event.values[2]);
        Log.d(TAG,"gyrox=" + event.values[0] + ", gyroy="+ event.values[1] + ", gyroz="+event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //-----------------------------------------


    private void startDeviceTask() {
        deviceTask = new DeviceTask(rows);
        deviceTask.execute();
    }

    private void startLocationUpdates() {
        // 1. 위치 요청 (Location Request) 설정
        LocationRequest locRequest =  LocationRequest.create();
        locRequest.setInterval(10000);
        locRequest.setFastestInterval(5000);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 2. 위치 업데이트 콜백 정의
        // 추가된 코드
        // 현재 위도 / 경도 값을 알아내는 코드
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mLastLocation  = locationResult.getLastLocation();

                Log.d(TAG,"latitude : " + mLastLocation.getLatitude()+
                        ", longitude : " + mLastLocation.getLongitude());
                updateUI();

                // ********** 추가 되는 부분 *************************
                // 새로운 위치가 변경될 때마다, rows Map 객체에 위도, 경도 저장
                rows.put("latitude", mLastLocation.getLatitude());
                rows.put("longitude", mLastLocation.getLongitude());
                // ********** 추가 되는 부분 *************************
            }
        };

        // 3. 위치 접근에 필요한 권한 검사
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,            // MainActivity 액티비티의 객체 인스턴스를 나타냄
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},        // 요청할 권한 목록을 설정한 String 배열
                    REQUEST_PERMISSIONS_FOR_LOCATION_UPDATES    // 사용자 정의 int 상수. 권한 요청 결과를 받을 때
            );
            return;
        }

        // 4. 위치 업데이트 요청
        mFusedLocationClient.requestLocationUpdates(locRequest,
                mLocationCallback,
                null /* Looper */);


    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSIONS_FOR_LOCATION_UPDATES: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startLocationUpdates();

                } else {
                    Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT);
                }
            }
        }
    }

    public void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void updateUI() {
        double latitude = 0.0;
        double longitude = 0.0;
        float precision = 0.0f;

        TextView latitudeTextView = (TextView) findViewById(R.id.latitude_text);
        TextView longitudeTextView = (TextView) findViewById(R.id.longitude_text);
        TextView precisionTextView = (TextView) findViewById(R.id.precision_text);

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            precision = mLastLocation.getAccuracy();
        }
        latitudeTextView.setText("Latitude: " + latitude);
        longitudeTextView.setText("Longitude: " + longitude);
        precisionTextView.setText("Precision: " + precision);
    }
}