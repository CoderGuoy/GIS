package com.bonc.ioc.gis.net;


import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * @Version:V1.0
 * @Author:CoderGuoy
 * @CreateTime:2017年8月14日
 * @Descrpiton:网络请求接口
 */
public interface ApiServices {
    @GET("bonc_ioc_tm/mobile/savePostion")
    Observable<PositionBean> setPosition(@Query("state") String state,
                                         @Query("lon") String lon,
                                         @Query("lat") String lat,
                                         @Query("saveTime") String saveTime,
                                         @Query("posCode") String posCode);
}
