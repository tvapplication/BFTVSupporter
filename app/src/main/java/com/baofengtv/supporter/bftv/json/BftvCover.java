package com.baofengtv.supporter.bftv.json;

import java.util.List;

/**
 * @author LiLiang
 * @version v1.0
 * @brief   解析的json实体封装
 * @date 2015/8/5
 */
public class BftvCover {

    //
    private int id = -1;
    //
    private String name;
    //所属父id
    private int lid = -1;
    //封面海报url
    private String cover;
    //顺序
    private int sort;
    //下一跳的海报地址
    private String secondPic;
    //下一跳，可配置intent action、json等跳转数据
    private String url;
    //五个sub类
    private List<BftvCover> backPicList;
    //文件的md5值，用于一致性比较
    private String md5cover;
    private String md5secondpic;
    //下拉button
    private List<BftvCover> buttonList;

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

    public void setSort(int sort){
        this.sort = sort;
    }

    public int getSort(){
        return this.sort;
    }

    public void setSecondPic(String secondPic){
        this.secondPic = secondPic;
    }

    public String getSecondPic(){
        return this.secondPic;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getUrl(){
        return this.url;
    }

    public void setBackPicList(List<BftvCover> list){
        this.backPicList = list;
    }

    public List<BftvCover> getBackPicList(){
        return this.backPicList;
    }

    public void setMd5cover(String md5cover){
        this.md5cover = md5cover;
    }
    public String getMd5cover(){
        return this.md5cover;
    }

    public void setMd5secondpic(String md5secondpic){
        this.md5secondpic = md5secondpic;
    }
    public String getMd5secondpic(){
        return this.md5secondpic;
    }

    public void setButtonList(List<BftvCover> list){
        this.buttonList = list;
    }

    public List<BftvCover> getButtonList(){
        return this.buttonList;
    }

    public String toString(){
        String str= "id=" + id + "; name=" + name + "; parentId=" + lid
                +"; cover=" + cover + "; sort=" + sort
                + "; secondPic=" + secondPic +"; md5cover=" + md5cover
                +"; md5secondpic=" + md5secondpic+"; ";
        if(backPicList != null){
            str += "[backPicList] ";
            int size = backPicList.size();
            for(int i=0; i<size; i++){
                str += backPicList.get(i).toString() + "; ";
            }
        }
        if(buttonList != null){
            str += "[buttonList] ";
            int size = buttonList.size();
            for(int i=0; i<size; i++){
                str += buttonList.get(i).toString() + "; ";
            }
        }
        return str;
    }
}
