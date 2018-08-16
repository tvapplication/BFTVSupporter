package com.baofengtv.supporter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author LiLiang
 * @version v1.0
 * @brief
 * @date 2015/8/3
 */
public class PosterEntry implements Parcelable{

    //业务id
    public int businessId = 0;

    //网络海报的下载地址url
    public String url = "";

    //下载网络图片成功后存储的本地全路径
    public String imagePath;

    //描述
    public String desc;

    //从后台直接取的配置命令（在推荐类型游戏和专场二级海报有用到，用于跳转）
    public String intent;

    public PosterEntry(){

    }

    public PosterEntry(Parcel parcel){
        this.businessId = parcel.readInt();
        this.url = parcel.readString();
        this.imagePath = parcel.readString();
        this.desc = parcel.readString();
        this.intent = parcel.readString();
    }

    public String toString(){
        return "businessId=" + businessId + "; ul=" + url
                +"; imagePath=" + imagePath + "; desc=" +
                desc +"; intent=" + intent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(businessId);
        dest.writeString(url);
        dest.writeString(imagePath);
        dest.writeString(desc);
        dest.writeString(intent);
    }

    public static final Parcelable.Creator<PosterEntry> CREATOR = new Parcelable.Creator<PosterEntry>() {
        @Override
        public PosterEntry createFromParcel(Parcel parcel) {
            return new PosterEntry(parcel);
        }

        @Override
        public PosterEntry[] newArray(int size) {
            return new PosterEntry[size];
        }
    };
}
