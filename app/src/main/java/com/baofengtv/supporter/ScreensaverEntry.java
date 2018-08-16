package com.baofengtv.supporter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2016/6/14 16:00
 * @changeRecord [修改记录] <br/>
 */
public class ScreensaverEntry implements Parcelable {

    //业务id
    public int businessId = 0;

    //网络屏保的下载地址url
    public String url = "";

    //下载屏保成功后存储的本地全路径
    public String localPath;

    //描述
    public String desc;

    //从后台直接取的配置命令（在推荐类型游戏和专场二级海报有用到，用于跳转）
    public String intent;

    public long timestamp;

    //photo(我的相册),pic(官方图片),video(官方视频)如为空则默认为pic+video
    public String type;

    public String md5 = "";

    public ScreensaverEntry(){

    }

    public ScreensaverEntry(Parcel parcel){
        this.businessId = parcel.readInt();
        this.url = parcel.readString();
        this.localPath = parcel.readString();
        this.desc = parcel.readString();
        this.intent = parcel.readString();
        this.timestamp = parcel.readLong();
        this.type = parcel.readString();
        this.md5 = parcel.readString();
    }

    public String toString(){
        return "businessId=" + businessId + "; ul=" + url
                +"; localPath=" + localPath + "; desc=" +
                desc +"; intent=" + intent + "; timestamp=" + timestamp + "; type=" + type + "; md5=" + md5;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(businessId);
        dest.writeString(url);
        dest.writeString(localPath);
        dest.writeString(desc);
        dest.writeString(intent);
        dest.writeLong(timestamp);
        dest.writeString(type);
        dest.writeString(md5);
    }

    public static final Parcelable.Creator<ScreensaverEntry> CREATOR = new Parcelable.Creator<ScreensaverEntry>() {
        @Override
        public ScreensaverEntry createFromParcel(Parcel parcel) {
            return new ScreensaverEntry(parcel);
        }

        @Override
        public ScreensaverEntry[] newArray(int size) {
            return new ScreensaverEntry[size];
        }
    };
}
