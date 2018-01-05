package com.bonc.ioc.gis;

import android.Manifest;
import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationManager;
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
import com.bonc.ioc.gis.net.ApiHelper;
import com.bonc.ioc.gis.net.PositionBean;
import com.bonc.ioc.gis.utils.LocationUtils;
import com.bonc.ioc.gis.utils.NetUtils;
import com.bonc.ioc.gis.utils.SPUtils;
import com.bonc.ioc.gis.utils.ToastUtil;
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
    private Double latitude;
    private Double longitude;
    private String state1 = "1";//开启巡航/定位
    private String state2 = "2";//停止巡航/定位
    private String state3 = "3";//正常行驶/巡航
    private String state4 = "4";//上报异常
    private String state = "0";//当前状态
    private ProgressDialog mProgressDialog;
    private double gpsLatitude;
    private double gpsLongitude;

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
        if (aMap == null) {
            aMap = bindingView.mapview.getMap();
        }
        setUpMap();//定位初始化
        //实例化UiSettings类对象
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);
        rxPermissions = new RxPermissions(this);
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("别着急，骚年，正在连接...");
        bindingView.textIp.setText(SPUtils.getString("ip", ""));
        bindingView.textCode.setText(SPUtils.getString("code", ""));
    }

    private void initListener() {
        bindingView.btnStart.setOnClickListener(this);
        bindingView.btnEnd.setOnClickListener(this);
        bindingView.textIp.setOnClickListener(this);
        bindingView.textCode.setOnClickListener(this);
        bindingView.textTest.setOnClickListener(this);
        bindingView.btnIpOk.setOnClickListener(this);
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
                getGPSLocation();
                //当前位置的经度
                latitude = amapLocation.getLatitude();
                //当前位置的纬度
                longitude = amapLocation.getLongitude();
                bindingView.textLatitude.setText("GD:" + latitude + "|GPS:" + gpsLatitude);
                bindingView.textLontitude.setText("GD:" + longitude + "|GPS:" + gpsLongitude);
                Log.i("gis==", "GD_lat" + latitude + ";" + "GD_lng" + longitude
                        + "GPS_lat" + gpsLatitude + "GPS_lng" + gpsLongitude);
                //获取定位时间
                bindingView.textTime.setText(getSystemTime());
                if (!NetUtils.isNetworkConnected(MainActivity.this)) {//没有网络
                    ToastUtil.show("骚年，没有网络，无法工作！");
                    bindingView.textSuccess.setText("已断开");
                    bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                    bindingView.btnStart.setText("开始");
                    state = state2;
                }
                if (state.equals(state3)) {
                    getNetData(state);
                }
            } else {
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }

    /**
     * 通过GPS获取定位信息
     */
    public void getGPSLocation() {
        Location gps = LocationUtils.getGPSLocation(this);
        if (gps == null) {
            //设置定位监听，因为GPS定位，第一次进来可能获取不到，通过设置监听，可以在有效的时间范围内获取定位信息
            LocationUtils.addLocationListener(this, LocationManager.GPS_PROVIDER, new LocationUtils.ILocationListener() {
                @Override
                public void onSuccessLocation(Location location) {
                    if (location != null) {
                        gpsLatitude = location.getLatitude();
                        gpsLongitude = location.getLongitude();
                        Toast.makeText(MainActivity.this, "gps onSuccessLocation location:  lat==" + location.getLatitude() + "     lng==" + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "gps location is null", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            gpsLatitude = gps.getLatitude();
            gpsLongitude = gps.getLongitude();
            Toast.makeText(this, "gps location: lat==" + gps.getLatitude() + "  lng==" + gps.getLongitude(), Toast.LENGTH_SHORT).show();
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
            case R.id.text_ip://服务器IP
                bindingView.textIp.setVisibility(View.GONE);
                bindingView.layoutIp.setVisibility(View.VISIBLE);
                break;
            case R.id.text_code://定位编码
                bindingView.textCode.setVisibility(View.GONE);
                bindingView.layoutCode.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_ip_ok://服务器IP确定
                if (bindingView.edittextIp.getText().length() > 0) {
                    bindingView.textIp.setVisibility(View.VISIBLE);
                    bindingView.textIp.setText(bindingView.edittextIp.getText().toString());
                    SPUtils.putString("ip", bindingView.edittextIp.getText().toString());
                    bindingView.layoutIp.setVisibility(View.GONE);
                } else {
                    ToastUtil.show("请输入服务器IP");
                }
                break;
            case R.id.btn_code_ok://定位编码确定
                if (bindingView.edittextCode.getText().length() > 0) {
                    bindingView.textCode.setVisibility(View.VISIBLE);
                    bindingView.textCode.setText(bindingView.edittextCode.getText().toString());
                    SPUtils.putString("code", bindingView.edittextCode.getText().toString());
                    bindingView.layoutCode.setVisibility(View.GONE);
                } else {
                    ToastUtil.show("请输入定位编码");
                }
                break;
            case R.id.text_test://连接测试
                if (bindingView.textIp.getText() != null && bindingView.textCode.getText() != null) {
                    state = state1;
                    getNetTestData(state);
                }
                break;
            case R.id.btn_start://开始
                if (bindingView.textSuccess.getText().equals("已连接")) {
                    state = state3;
                }
                if (bindingView.textSuccess.getText().equals("未连接")) {
                    ToastUtil.show("服务器未连接，无法开始");
                }
                break;
            case R.id.btn_end://结束
                if (bindingView.textSuccess.getText().equals("已连接")) {
                    state = state2;
                    getNetTestData(state);
                    bindingView.btnStart.setText("开始");
                } else {
                    ToastUtil.show("服务器未连接!");
                }
                break;
        }
    }

    /**
     * 连接测试
     */
    private void getNetTestData(final String state) {
        ApiHelper.getInstance("http://" + bindingView.textIp.getText() + "/").getPosition(
                state, gpsLongitude + "", gpsLatitude + "", getSystemTime(), bindingView.textCode.getText().toString())
                .subscribe(new Subscriber<PositionBean>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        mProgressDialog.show();
                    }

                    @Override
                    public void onNext(PositionBean bean) {
                        if (bean.getCode().equals("1")) {//连接成功
                            bindingView.textSuccess.setText("已连接");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.green));
                        } else {
                            bindingView.textSuccess.setText("连接失败");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                        }
                    }

                    @Override
                    public void onCompleted() {
                        mProgressDialog.dismiss();
                        mProgressDialog.cancel();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("gis_onError", e.toString());
                        if (state.equals(state1)) {//连接测试阶段异常
                            bindingView.textSuccess.setText("未连接");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                        }
                        if (state.equals(state3)) {//运行中出现异常
                            bindingView.textSuccess.setText("已断开");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                            bindingView.btnStart.setText("开始");
                        }
                    }
                });
    }

    /**
     * 上传数据
     */
    private void getNetData(final String state) {
        ApiHelper.getInstance("http://" + bindingView.textIp.getText() + "/").getPosition(
                state, gpsLongitude + "", gpsLatitude + "", getSystemTime(), bindingView.textCode.getText().toString())
                .subscribe(new Subscriber<PositionBean>() {
                    @Override
                    public void onNext(PositionBean bean) {
                        if (bean.getCode().equals("1")) {//连接成功
                            bindingView.textSuccess.setText("已连接");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.green));
                        } else {
                            bindingView.textSuccess.setText("连接失败");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                        }
                    }

                    @Override
                    public void onCompleted() {
                        bindingView.btnStart.setText("正在运行中...");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("gis_onError", e.toString());
                        if (state.equals(state1)) {//连接测试阶段异常
                            bindingView.textSuccess.setText("未连接");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                        }
                        if (state.equals(state3)) {//运行中出现异常
                            bindingView.textSuccess.setText("已断开");
                            bindingView.textSuccess.setTextColor(getResources().getColor(R.color.color_f44a4a));
                            bindingView.btnStart.setText("开始");
                        }
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
        mProgressDialog = null;
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
