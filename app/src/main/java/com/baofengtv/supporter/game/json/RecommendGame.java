package com.baofengtv.supporter.game.json;

public class RecommendGame {
	public String name;
	public String description;
	public String action;
	public String game_apk;
	public int image_width;

	public String imageurl;
	public String recommended_id;
	public String imagecode;

	public int image_height;
	public String link_code;
	public String link_type;
	public String recommend;
	public String game_class;

	/*
	public void setName(String name){
		this.name = name;
	}
	public String getName(){
		return this.name;
	}

	public void setDescription(String desc){
		this.description = desc;
	}

	public String getDescription(){
		return this.description;
	}

	public void setAction(String action){
		this.action = action;
	}
	public String getAction(){
		return this.action;
	}

	public void setGameApk(String apk){
		this.game_apk = apk;
	}
	public String getGameApk(){
		return this.game_apk;
	}

	public void setGameClass(String gameClass){
		this.game_class = gameClass;
	}
	public String getGameClass(){
		return this.game_class;
	}

	public void setImageUrl(String url){
		this.imageurl = url;
	}
	public String getImageUrl(){
		return this.imageurl;
	}

	public void setRecommendJsonStr(String jsonStr){
		this.recommended_id = jsonStr;
	}
	public String getRecommendJsonStr(){
		return this.recommended_id;
	}

	public void setImageCode(String code){
		this.imagecode = code;
	}
	public String getImageCode(){
		return this.imagecode;
	}

	public void setImageWidth(int w){
		this.image_width = w;
	}
	public int getImageWidth(){
		return this.image_width;
	}

	public void setImageHeight(int h){
		this.image_height = h;
	}
	public int getImageHeight(){
		return this.image_height;
	}

	public void setLinkCode(String code){
		this.link_code = code;
	}
	public String getLinkCode(){
		return this.link_code;
	}

	public void setLinkType(String type){
		this.link_type = type;
	}
	public String getLinkType(){
		return this.link_type;
	}

	public void setRecommend(String r){
		this.recommend = r;
	}
	public String getRecommend(){
		return this.recommend;
	}
	*/

	public String toString(){
		return "name:" + name + "; des=" + description + "; action="
				+ action + "; imageUrl=" + imageurl + "; recommended_id="
				+ recommended_id;
	}
}
