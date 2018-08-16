package com.baofengtv.supporter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author LiLiang
 * @version v1.0
 * @brief
 * @date 2015/8/12
 */
public class GameEntry implements Parcelable {

    //业务id
    public int businessId;

    //0:已安装 1：推荐
    public int type;

    //图标存储路径
    public String iconPath;

    //type=0(安装类型)时有效,type=1(推荐类型)时使用packageName装名字
    public String packageName;

    //type=1时有效，跳转至推荐游戏需要
    public String intent;

    public GameEntry(Parcel parcel){
        this.businessId = parcel.readInt();
        this.type = parcel.readInt();
        this.iconPath = parcel.readString();
        this.packageName = parcel.readString();
        this.intent = parcel.readString();
    }

    public GameEntry() {

    }

    public String toString(){
        return "businessId=" + this.businessId + "; type=" + this.type
                + "; packageName=" + this.packageName
                + "; intent=" + this.intent + "; iconPath=" + iconPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.businessId);
        dest.writeInt(this.type);
        dest.writeString(this.iconPath);
        dest.writeString(this.packageName);
        dest.writeString(this.intent);
    }

    public static final Parcelable.Creator<GameEntry> CREATOR = new Parcelable.Creator<GameEntry>() {
        @Override
        public GameEntry createFromParcel(Parcel parcel) {
            return new GameEntry(parcel);
        }

        @Override
        public GameEntry[] newArray(int size) {
            return new GameEntry[size];
        }
    };
}
