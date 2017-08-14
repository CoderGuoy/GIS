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
    //盐城测试服务器 管理端
    public static final String GL_URL = "http://218.92.211.30:10003/bonc_ycioc_lmp/";
    //盐城测试服务器 服务端
    public static final String BASE_URL = "http://218.92.211.30:10003/bonc_ycioc_lsp/";

    //图片
    public static final String PHOTO_URL = "views/lmp/index/images/pic/";
    //================= PATH ====================

    public static final String PATH_DATA = App.getInstance().getCacheDir().getAbsolutePath() + File.separator + "data";

    public static final String PATH_CACHE = PATH_DATA + "/NetCache";

    public static final String PATH_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "codeest" + File.separator + "GIS";

}
