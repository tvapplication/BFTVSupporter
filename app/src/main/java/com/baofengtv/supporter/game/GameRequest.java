package com.baofengtv.supporter.game;

import com.baofengtv.supporter.game.json.RecommendGame;

import java.util.List;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description Retrofix+RxJava拉取游戏推荐接口数据
 * @company 暴风TV
 * @created 2017/6/9 16:00
 * @changeRecord [修改记录] <br/>
 */

public class GameRequest {

    public static class RecommendResult<T> {
        public int code;
        public String text;
        public T ext;
    }

    public static interface RecommendGameApi {
        @GET("show_advt.json?fromer=bfGame")
        Observable<RecommendResult<List<RecommendGame>>> getRecommendGameList();
    }

    public static void getRecommendData(Subscriber<RecommendResult<List<RecommendGame>>> subscriber){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://open.play.cn/api/v2/tv/eoi/")
                .addConverterFactory(GsonConverterFactory.create()) //gson
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create()) //RxJava
                .build();

        RecommendGameApi api = retrofit.create(RecommendGameApi.class);
        api.getRecommendGameList()
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);

    }
}
