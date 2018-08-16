package com.baofengtv.supporter.bftv.json;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 二级海报json构造体，适用于专场和运营位的二级海报
 * @date 2015/9/16
 */
public class SecondCover {
    //
    private int id = -1;
    //
    private String name;
    //所属父id
    private int lid = -1;
    //封面海报url
    private String cover;
    //下一跳，可配置intent action、json等跳转数据
    private String url;
    //背景url
    private String backPic;

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

    public void setLid(int lid){
        this.lid = lid;
    }

    public int getLid(){
        return this.lid;
    }

    public void setCover(String cover){
        this.cover = cover;
    }

    public String getCover(){
        return this.cover;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getUrl(){
        return this.url;
    }

    public void setBackPic(String backPic){
        this.backPic = backPic;
    }

    public String getBackPic(){
        return this.backPic;
    }

    public String toString(){
        String str= "id=" + id + "; name=" + name + "; parentId=" + lid
                +"; cover=" + cover + "; backpic=" + backPic;
        return str;
    }
}
