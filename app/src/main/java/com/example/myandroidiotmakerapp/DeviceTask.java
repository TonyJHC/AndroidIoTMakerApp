package com.example.myandroidiotmakerapp;

import android.os.AsyncTask;
import android.util.Log;

import com.kt.smcp.gw.ca.comm.exception.SdkException;
import com.kt.smcp.gw.ca.gwfrwk.adap.stdsys.sdk.tcp.BaseInfo;
import com.kt.smcp.gw.ca.gwfrwk.adap.stdsys.sdk.tcp.IMTcpConnector;
import com.kt.smcp.gw.ca.gwfrwk.adap.stdsys.sdk.tcp.LogIf;
import com.kt.smcp.gw.ca.util.IMUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeviceTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = DeviceTask.class.getSimpleName();

    private Map<String, Double> rows = new HashMap<String, Double>();

    public DeviceTask(Map<String, Double> rows) {
        this.rows = rows;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    // UI thread에서 돌아가는 MainActivity와는 다르게 background 에서 돌아가서 UI thread에게 관여하지 않음
    // 연결 상태가 안좋아 딜레이가 생기더라도 성능에 큰 영향을 끼치지 않음.
    @Override // 재정의
    protected Void doInBackground(Void... params) {

        // IoTMakers 연동 설정 정보
        BaseInfo info = new BaseInfo();
        // 접속 IP, Port 설정
        info.setIp("220.90.216.90");
        info.setPort(10020);


        // 디바이스상세정보-> Gateway 연결 ID를 입력한다.
        info.setExtrSysId("OPEN_TCP_001PTL001_1000010796");
        // 디바이스상세정보-> 디바이스 아이디를 입력한다.
        info.setDeviceId("jongviD1631689977129");
        // 디바이스상세정보-> 디바이스 패스워드를 입력한다.
        info.setPassword("j6qxubssa");


        // IoTMakers 연동 TCP Connector 생성 : 안드로이드 앱과 IoTMakers 연
        IMTcpConnector connector = new IMTcpConnector();
        try {
            connector.activate(new LogIf(), info, (long) 3000); // 1. 실제로 연결하는 코드

            long transId = IMUtil.getTransactionLongRoundKey4();

            Log.d(TAG, rows.toString());
            // 계측 데이터 HashMap 객체로 전송한다. key는 센싱태그 명 value는 계측값을 넣는다.
            connector.requestNumColecDatas(rows, new Date(), transId); // 2. 데이터를 보내는 코드

            connector.deactivate(); // 3. 연결 끊기

        } catch (SdkException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}