// ISlaveService.aidl
package com.baofengtv.supporter;

// Declare any non-default types here with import statements
import com.baofengtv.supporter.PosterEntry;
import com.baofengtv.supporter.GameEntry;

interface ISupporterService {

    void launchBFTVById(int businessId);
    void launchBFTV(in PosterEntry entry);

    //如果返回null,说明在异步拉取，稍后会通过广播"baofengtv.supporter.action.GAME_INFO"发出
    List<GameEntry> requestGamesInfo();
    //跳转至游戏应用首页
    void launchGameApp();
    //进入我的游戏（已安装）
    void launchMyGames();
    //启动某个游戏
    void launchOneGameApp(in GameEntry game);

    //启动音乐
    void launchMusicApp();
    //启动我的音乐
    void launchMyMusics();
    //随便听听
    void launchRandomMusics();

    //启动应用市场
    void launchMarketApp();

    //启动轮播
    void launchTurnPlayApp();

    //启动画报
    void launchAlbumApp();

    //根据业务id查询对应的海报图片
    PosterEntry queryPosterById(int businessId);
    //查询所有海报图片
    List<PosterEntry> queryPosters();
}
