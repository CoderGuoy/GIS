package com.bonc.ioc.gis;

import android.Manifest;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.UiSettings;
import com.bonc.ioc.gis.databinding.ActivityMainBinding;
import com.bonc.ioc.gis.gps.GPSLocationListener;
import com.bonc.ioc.gis.gps.GPSLocationManager;
import com.bonc.ioc.gis.gps.GPSProviderStatus;
import com.bonc.ioc.gis.net.ApiHelper;
import com.bonc.ioc.gis.net.PositionBean;
import com.bonc.ioc.gis.utils.NetUtils;
import com.bonc.ioc.gis.utils.SPUtils;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.text.SimpleDateFormat;
import java.util.Date;

import rx.Subscriber;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements LocationSource, AMapLocationListener, View.OnClickListener {

    private RxPermissions rxPermissions;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private ActivityMainBinding bindingView;
    private AMap aMap;
    private UiSettings mUiSettings;
    private String state1 = "1";//开启巡航/定位
    private String state2 = "2";//停止巡航/定位
    private String state3 = "3";//正常行驶/巡航
    private String state4 = "4";//上报异常
    private String state = "0";//当前状态
    private double gpsLatitude = 0.0;
    private double gpsLongitude = 0.0;
    private boolean isStart = false;
    private GPSLocationManager mGPSLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindingView = DataBindingUtil.setContentView(MainActivity.this, R.layout.activity_main);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        bindingView.mapview.onCreate(savedInstanceState);
        initData();
        initListener();
    }

    private void initData() {
        mGPSLocationManager = GPSLocationManager.getInstances(MainActivity.this);
        if (aMap == null) {
            aMap = bindingView.mapview.getMap();
        }
        setUpMap();//定位初始化
        //实例化UiSettings类对象
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);
        rxPermissions = new RxPermissions(this);
        bindingView.textCode.setText(SPUtils.getString("code", "点击输入工号"));
    }

    private void initListener() {
        bindingView.btnStart.setOnClickListener(this);
        bindingView.textCode.setOnClickListener(this);
        bindingView.btnCodeOk.setOnClickListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //获取权限
        initPermission();
    }

    private void initPermission() {
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (!aBoolean) {
                            finish();
                        }
                    }
                });
    }

    /**
     * 定位初始化
     */
    private void setUpMap() {
        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置默认定位按钮是否显示
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        // 设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
//        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));//设置放大级别
//        aMap.moveCamera(CameraUpdateFactory.changeTilt(45));//设置俯仰角0°~45°（垂直与地图时为0）
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
            mLocationOption.setInterval(1000);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                mGPSLocationManager.start(new MyListener());
                //获取定位时间
                bindingView.textTime.setText(getSystemTime());
                bindingView.textLatitude.setText("GPS:" + gpsLatitude);
                bindingView.textLontitude.setText("GPS:" + gpsLongitude);
                Log.i("gis==", "lat" + gpsLatitude + "lng" + gpsLongitude);
                if (isStart == true) {
                    if (!NetUtils.isNetworkConnected(MainActivity.this)) {//没有网络
                        Toast.makeText(MainActivity.this, "没有网络，无法工作！", Toast.LENGTH_SHORT).show();
                        bindingView.btnStart.setText("开始");
                        state = state2;
                        getNetData(state,gpsLongitude,gpsLatitude);
                    } else {//断网回复后，继续上传数据
                        state = state1;
                        getNetData(state,gpsLongitude,gpsLatitude);
                    }
                }
            } else {
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }

    // TODO: 2018/1/9 通过GPS获取定位信息
    class MyListener implements GPSLocationListener {

        @Override
        public void UpdateLocation(Location location) {
            if (location != null) {
//                text_gps_3.setText("经度：" + location.getLongitude() + "\n纬度：" + location.getLatitude());
                gpsLatitude = location.getLatitude();
                gpsLongitude = location.getLongitude();
            }
        }

        @Override
        public void UpdateStatus(String provider, int status, Bundle extras) {
            if ("gps" == provider) {
                Toast.makeText(MainActivity.this, "定位类型：" + provider, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void UpdateGPSProviderStatus(int gpsStatus) {
            switch (gpsStatus) {
                case GPSProviderStatus.GPS_ENABLED:
                    Toast.makeText(MainActivity.this, "GPS开启", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_DISABLED:
                    Toast.makeText(MainActivity.this, "GPS关闭", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_OUT_OF_SERVICE:
                    Toast.makeText(MainActivity.this, "GPS不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_TEMPORARILY_UNAVAILABLE:
                    Toast.makeText(MainActivity.this, "GPS暂时不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_AVAILABLE:
                    Toast.makeText(MainActivity.this, "GPS可用啦", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.text_code://定位编码
                bindingView.textCode.setVisibility(View.GONE);
                bindingView.layoutCode.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_code_ok://定位编码确定
                if (bindingView.edittextCode.getText().length() > 0) {
                    bindingView.textCode.setVisibility(View.VISIBLE);
                    bindingView.textCode.setText(bindingView.edittextCode.getText().toString());
                    SPUtils.putString("code", bindingView.edittextCode.getText().toString());
                    bindingView.layoutCode.setVisibility(View.GONE);
                } else {
                    Toast.makeText(MainActivity.this, "请输入定位编码", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_start://开始
                if (bindingView.textCode.getText() != null && bindingView.textCode.getText() != "") {
                    if (bindingView.textCode.getText() == "点击输入工号") {
                    Toast.makeText(MainActivity.this, "请输入工号", Toast.LENGTH_SHORT).show();
                    } else {
                        isStart = true;
                        state = state1;
                        getNetData(state,gpsLongitude,gpsLatitude);
                    }
                }
                break;
        }
    }

    // TODO: 2018/1/10 上传数据
    private void getNetData(final String state,final double gpsLongitude,final double gpsLatitude) {
        ApiHelper.getInstance(Constants.URL).getPosition(
                state,
                gpsLongitude + "", gpsLatitude + "",
                getSystemTime(),
                bindingView.textCode.getText().toString())
                .subscribe(new Subscriber<PositionBean>() {
                    @Override
                    public void onNext(PositionBean bean) {
                    }

                    @Override
                    public void onCompleted() {
                        switch (state) {
                            case "1":
                                bindingView.btnStart.setText("正在运行中...");
                                break;
                            case "2":
                                bindingView.btnStart.setText("开始");
                                break;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("gis_onError", e.toString());
                        bindingView.btnStart.setText("开始");
                    }
                });
    }

    /**
     * 获取系统当前时间
     *
     * @return
     */
    private String getSystemTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String str = format.format(curDate);
        return str;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        bindingView.mapview.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        bindingView.mapview.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        bindingView.mapview.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        bindingView.mapview.onSaveInstanceState(outState);
    }
}
