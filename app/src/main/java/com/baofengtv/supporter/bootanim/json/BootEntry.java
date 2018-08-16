package com.baofengtv.supporter.bootanim.json;

/**
 * @author LiLiang
 * @version v1.0
 * @brief   开机动画（视频）json数据结构封装
 * @date 2015/9/17
 */
public class BootEntry {

    private int id;
    private String name;
    //http下载地址
    private String cover;
    //type 1.开机动画（视频），2.Launcher背景图，3二维码或广告
    private int type;
    //对开机动画（视频）无效，忽略
    private int sort;
    //2.开机动画：zip文件，3.开机视频
    private int filetype;
    //文件的md5值，用于一致性比较
    private String md5key;

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setCover(String cover){
        this.cover = cover;
    }

    public String getCover(){
        return this.cover;
    }

    public void setType(int type){
        this.type = type;
    }
    public int getType(){
        return this.type;
    }

    public void setSort(int sort){
        this.sort = sort;
    }

    public void setFiletype(int filetype){
        this.filetype = filetype;
    }
    public int getFiletype(){
        return this.filetype;
    }

    public void setMd5key(String md5key){
        this.md5key = md5key;
    }
    public String getMd5key(){
        return this.md5key;
    }

    @Override
    public String toString(){
        return "[BootEntry] name=" + name + "; cover=" + cover + "; type="
                +type + "; fileType=" + filetype + "; md5key=" + md5key;
    }
}
