package com.bonc.ioc.gis;

import android.os.Environment;

import java.io.File;

/**
 * @Version:
 * @Author:Guoy
 * @CreateTime:2017/6/8
 * @Descrpiton:
 */
public class Constants {
    //================= URL =====================
    public static final String URL="http://ioc.bonc.com.cn:8081/";
    //================= PATH ====================

    public static final String PATH_DATA = App.getInstance().getCacheDir().getAbsolutePath() + File.separator + "data";

    public static final String PATH_CACHE = PATH_DATA + "/NetCache";

    public static final String PATH_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "codeest" + File.separator + "GIS";

}
