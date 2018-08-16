package com.egame.tv.services.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class EgameInstallAppBean implements Parcelable {
    private String dataDir;
    private String picpath;
    private String id;
    private String name;
    private String packageName;
    private String versionCode;

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionCode() {
        return this.versionCode;
    }

    public String getDataDir() {
        return this.dataDir;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setDataDir(String paramString) {
        this.dataDir = paramString;
    }

    public void setId(String paramString) {
        this.id = paramString;
    }

    public void setName(String paramString) {
        this.name = paramString;
    }

    public void setPackageName(String paramString) {
        this.packageName = paramString;
    }

    public String getPicpath() {
        return picpath;
    }

    public void setPicpath(String picpath) {
        this.picpath = picpath;
    }

    public String toString() {
        return "AppBean [packageName=" + this.packageName + ", name="
                + this.name + ", dataDir=" + this.dataDir + ", picPath="
                + this.picpath + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(dataDir);
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(packageName);
        dest.writeString(versionCode);
        dest.writeString(picpath);
    }

    public static final Parcelable.Creator<EgameInstallAppBean> CREATOR = new Creator<EgameInstallAppBean>() {

        @Override
        public EgameInstallAppBean[] newArray(int arg0) {
            return new EgameInstallAppBean[arg0];
        }

        @Override
        public EgameInstallAppBean createFromParcel(Parcel source) {
            String dataDir = source.readString();
            String id = source.readString();
            String name = source.readString();
            String packageName = source.readString();
            String versionCode = source.readString();
            String picpath = source.readString();

            EgameInstallAppBean appBean = new EgameInstallAppBean();
            appBean.setDataDir(dataDir);
            appBean.setId(id);
            appBean.setName(name);
            appBean.setPackageName(packageName);
            appBean.setVersionCode(versionCode);
            appBean.setPicpath(picpath);
            return appBean;
        }
    };

}