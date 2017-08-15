package com.bonc.ioc.gis;

import android.Manifest;
import android.databinding.DataBindingUtil;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.UiSettings;
import com.bonc.ioc.gis.databinding.ActivityMainBinding;
import com.bonc.ioc.gis.utils.MacUtils;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.Date;

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
    }

    private void initListener() {
        bindingView.btnStart.setOnClickListener(this);
        bindingView.btnEnd.setOnClickListener(this);
        bindingView.textIp.setOnClickListener(this);
        bindingView.textTest.setOnClickListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //获取权限
        initPermission();
        //Mac地址
        bindingView.textMac.setText(MacUtils.getMac());
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
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
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
                //当前位置的经度
                latitude = amapLocation.getLatitude();
                //当前位置的纬度
                longitude = amapLocation.getLongitude();
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                bindingView.textLatitude.setText(latitude + "");
                bindingView.textLontitude.setText(longitude + "");
                //获取定位时间
                SimpleDateFormat df = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(amapLocation.getTime());
                    df.format(date);
                    bindingView.textTime.setText(df + "");
                }
            } else {
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.text_ip://编辑
                break;
            case R.id.text_test://连接测试
                break;
            case R.id.btn_start://开始
                break;
            case R.id.btn_end://结束
                break;
        }
    }
}
