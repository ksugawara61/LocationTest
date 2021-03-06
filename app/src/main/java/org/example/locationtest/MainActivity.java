package org.example.locationtest;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private static final long UPDATE_INTERVAL = 5000;
    private static final long FASTEST_INTERVAL = 1000;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private TextView mOutput;
    private ScrollView mScroller;
    private GoogleApiClient mGoogleApiClient;
    private long mLastTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 新しいAPIクライアントを定義
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // ビューの参照を取得
        mOutput = (TextView)findViewById(R.id.output);
        mScroller = (ScrollView)findViewById(R.id.scroller);

        // 更新から更新までの時間の計算のために現在の時刻を取得
        mLastTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Google Playサービスが利用できるならクライアントを接続
        if (serviceAvailable()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // 接続ステータスを表示
        log("Connected");

        // 現在地を取得
        Location location = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient
        );
        log("Locations (starting with last known):");
        if (location != null) {
            dumpLocation(location);
        }

        // 高い精度で1〜5秒間隔で更新を要求する
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        dumpLocation(location);
    }

    /**
     * 出力ウィンドウに文字列を書き込む
     */
    private void log(String string) {
        long newTime = System.currentTimeMillis();
        mOutput.append(String.format("+%04d: %s\n", newTime - mLastTime, string));
        mLastTime = newTime;

        // テキストビューをスクロールして最後の部分を表示するためのトリック
        mScroller.post(new Runnable() {
            @Override
            public void run() {
                mScroller.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * 指定された位置情報を文字列で表す。引数はnullの場合がある
     */
    private void dumpLocation(Location location) {
        if (location == null) {
            log("Locatiojn[unknown]");
        }
        else {
            log(location.toString());
        }
    }

    /**
     * Google Playサービスが使えるかどうかをチェックする
     */
    private boolean serviceAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            log("Google Play services is available");
            return true;
        }
        else {
            log("Google Play services is not available");
            showErrorDialog(resultCode);
            return false;
        }
    }

    /**
     * Google Playサービスのエラーメッセージを表示する
     */
    private void showErrorDialog(int resultCode) {
        // Google Playサービスからエラーダイアログを取得する
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        if (errorDialog != null) {
            // エラーを表示する
            errorDialog.show();
        }
    }

    /**
     * 位置情報クライアントへの接続が失敗した時に位置情報サービスから呼び出される
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // 例えば新バージョンをインストールするなどすれば解決するのか？
        log("Connection failed");
        if (connectionResult.hasResolution()) {
            try {
                // エラーの解決を試みるアクティビティを起動
                log("Trying to resolve the error...");
                connectionResult.startResolutionForResult(
                        this, CONNECTION_FAILURE_RESOLUTION_REQUEST
                );
            } catch (IntentSender.SendIntentException e) {
                log("Exception during resolution: " + e.toString());
            }
        }
        else {
            // 解決不能
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * クライアントサービスが一時的に接続切断状態になったときに位置情報サービスから呼び出される
     */
    @Override
    public void onConnectionSuspended(int cause) {
        log("Connection suspended");
    }

    /**
     * Google Playサービスの新バージョンをダウンロードしたあとの処理をする
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                // 結果コードがOKなら、再接続を試みる
                log("Resolution result code is: " + resultCode);
                switch(resultCode) {
                    case RESULT_OK:
                        // ここで再接続を試す
                        mGoogleApiClient.connect();
                        break;
                }
                break;
        }
    }

}

