package com.baofengtv.supporter.loader;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2017/6/8 18:10
 * @changeRecord [修改记录] <br/>
 */

public class ScreensaverDownloadParam extends BftvDownloadParam {
    //增加以下几个字段
    public String type = "";

    public String sort = "";

    public String toString(){
        return super.toString() + "; type= " + type + "; sort=" + sort;
    }
}
