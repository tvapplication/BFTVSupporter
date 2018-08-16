package com.baofengtv.supporter.loader;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2017/6/8 16:35
 * @changeRecord [修改记录] <br/>
 */

public class BftvDownloadParam extends DownloadParam {

    //增加以下几个字段
    public int businessId = -1;

    public String intent;

    public String cardName;

    public String toString(){
        return "businessId=" + businessId + "; cardName=" + cardName +
                "; src=" + src + "; serverMd5=" + srcMD5 + "; dst=" + dst
                +"; localMd5=" + dstMD5 + "; statusCode=" + code;
    }
}
