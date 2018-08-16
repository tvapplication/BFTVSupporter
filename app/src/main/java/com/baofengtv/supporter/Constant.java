package com.baofengtv.supporter;

import java.io.File;
import java.util.HashMap;

/**
 * @author LiLiang
 * @version v1.0
 * @brief   常量集合
 * @date 2015/8/2
 */
public class Constant {
    //海报的存储路径
    public static String CACHED_DIR = "/data/misc/posters/";
    //public static String SCREEN_SAVE_DIR = "/data/misc/baofengtv/screensaver/";
    public static String HOUYI_AD_DIR = "/data/misc/baofengtv/houyi_ad/";

    //海报下载地址
    //public static String BFTV_BASE_URL = "http://ptbftv.gitv.tv";//"http://bftvapi.riverrun.cn";//
    //测试地址
    //public static final String BFTV_BASE_TEST_URL = "http://bftvapi.riverrun.cn";

    //获取爱游戏推荐游戏列表的地址
    //public static final String GAME_RECOMMEND_URL =
    //        "http://open.play.cn/api/v2/tv/eoi/show_advt.json?fromer=bfGame";

    public static final String UPLOAD_LOG_URL = "http://bfm.fengmi.tv/bfm/logquery/receiveFile.ajax";

    //游戏apk发生安装/卸载后发广播
    //public static final String BROADCAST_GAMEINFO = "baofengtv.supporter.action.GAME_INFO";

    //所有海报下载完毕发广播
    //public static final String BROADCAST_POSTERS_FINISHED = "baofengtv.supporter.action.POSTER_FINISHED";

    //开机广告位
    public static final String HOUYI_AD_BOOT = "293400472202";  //小魔投开机广告位894347409981
    //关机广告位
    public static final String HOUYI_AD_POWERDOWN = "346532438380";
    //Launcher左下角广告位
    public static final String HOUYI_AD_LAUNCHER_ADV = "630609662214";
    //影视库卡片广告位
    public static final String HOUYI_AD_LAUNCHER_VIDEO = "405466569491";
    //游戏卡片广告位
    public static final String HOUYI_AD_LAUNCHER_GAME = "549454616368";
    //体育卡片广告位
    public static final String HOUYI_AD_LAUNCHER_SPORT = "174905842506";
    //购物卡片广告位
    public static final String HOUYI_AD_LAUNCHER_SHOPPING = "872638553296";

    public static final String HOUYI_AD_NORMAL_SCREEN_SAVER = "816545365913";
    //京东渠道定制机的屏保广告位
    public static final String HOUYI_AD_JD_SCREEN_SAVER = "239891100859";
    //屏保广告位
    public static String HOUYI_AD_SCREEN_SAVER = HOUYI_AD_NORMAL_SCREEN_SAVER;

    public static HashMap<String, String> HouyiAdMap;
    static {
        HouyiAdMap = new HashMap<>();
        HouyiAdMap.put(HOUYI_AD_BOOT, "开机广告");
        HouyiAdMap.put(HOUYI_AD_POWERDOWN, "关机广告");
        HouyiAdMap.put(HOUYI_AD_LAUNCHER_ADV, "Launcher左下角广告");
        HouyiAdMap.put(HOUYI_AD_LAUNCHER_VIDEO, "影视卡片启动广告");
        HouyiAdMap.put(HOUYI_AD_LAUNCHER_GAME, "游戏卡片启动广告");
        HouyiAdMap.put(HOUYI_AD_LAUNCHER_SPORT, "体育卡片启动广告");
        HouyiAdMap.put(HOUYI_AD_LAUNCHER_SHOPPING, "购物卡片启动广告");
        HouyiAdMap.put(HOUYI_AD_SCREEN_SAVER, "屏保广告");
        HouyiAdMap.put(HOUYI_AD_JD_SCREEN_SAVER, "京东定制渠道屏保广告");
    }

    /**
     * 设置网络海报的下载路径
     * @param dir
     */
    public static void setCachedPosterDir(File dir){
        if (!dir.exists()) {
            dir.mkdir();
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            dir.setExecutable(true, false);
        }
        CACHED_DIR = dir.getAbsolutePath() + "/";
    }

    public static void setHouyiAdDir(File dir){
        if (!dir.exists()) {
            dir.mkdir();
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            dir.setExecutable(true, false);
        }
        HOUYI_AD_DIR = dir.getAbsolutePath() + "/";
    }
/*
    public static final int ID_INVALID = -1;

    //==========don't change these values==========//
    //每个海报对应的业务id值
    //TV窗口
    public static final int ID_TV_WINDOW = 100;
    public static final int ID_TV_SOURCE1 = 101;
    public static final int ID_TV_SOURCE2 = 102;
    public static final int ID_TV_SOURCE3 = 103;

    //暴风广告位（TV小窗口下侧）
    public static final int ID_ADV_BAOFENG = 200;
    //点击广告位展示的二级大图,最多数组【201,210】
    public static final int ID_ADV_BAOFENG_SECOND = 201;
    //...
    public static final int ID_ADV_BAOFENG_SECOND_MAX = 210;

    //轮播海报
    public static final int ID_TURN_PLAY = 300;
    //轮播海报的6个小背景图
    public static final int ID_TURN_PLAY_SUB1 = 301;
    public static final int ID_TURN_PLAY_SUB2 = 302;
    public static final int ID_TURN_PLAY_SUB3 = 303;
    public static final int ID_TURN_PLAY_SUB4 = 304;
    public static final int ID_TURN_PLAY_SUB5 = 305;
    public static final int ID_TURN_PLAY_SUB6 = 306;

    //速播
    public static final int ID_QUICK_PLAY = 400;
    //速播的6个小背景图
    public static final int ID_QUICK_PLAY_SUB1 = 401;
    public static final int ID_QUICK_PLAY_SUB2 = 402;
    public static final int ID_QUICK_PLAY_SUB3 = 403;
    public static final int ID_QUICK_PLAY_SUB4 = 404;
    public static final int ID_QUICK_PLAY_SUB5 = 405;
    public static final int ID_QUICK_PLAY_SUB6 = 406;

    //专场
    public static final int ID_SPECIAL = 500;
    //专场海报的6个小背景图
    public static final int ID_SPECIAL_SUB1 = 501;
    public static final int ID_SPECIAL_SUB2 = 502;
    public static final int ID_SPECIAL_SUB3 = 503;
    public static final int ID_SPECIAL_SUB4 = 504;
    public static final int ID_SPECIAL_SUB5 = 505;
    public static final int ID_SPECIAL_SUB6 = 506;
    //点击专场后展示的海报及全背景图，最多十组【511,520】【521,530】
    public static final int ID_SPECIAL_SECOND_SUB1 = 511;
    public static final int ID_SPECIAL_SECOND_SUB2 = 512;
    public static final int ID_SPECIAL_SECOND_SUB3 = 513;
    public static final int ID_SPECIAL_SECOND_SUB4 = 514;
    public static final int ID_SPECIAL_SECOND_SUB5 = 515;
    public static final int ID_SPECIAL_SECOND_SUB6 = 516;
    //...
    public static final int ID_SPECIAL_SECOND_SUB_MAX = 520;

    public static final int ID_SPECIAL_SECOND_SUB1_BACKGROUND = 521;
    public static final int ID_SPECIAL_SECOND_SUB2_BACKGROUND = 522;
    public static final int ID_SPECIAL_SECOND_SUB3_BACKGROUND = 523;
    public static final int ID_SPECIAL_SECOND_SUB4_BACKGROUND = 524;
    public static final int ID_SPECIAL_SECOND_SUB5_BACKGROUND = 525;
    public static final int ID_SPECIAL_SECOND_SUB6_BACKGROUND = 526;
    //...
    public static final int ID_SPECIAL_SECOND_SUB_MAX_BACKGROUND = 530;

    //影视库
    public static final int ID_FILM_LIBRARY = 600;
    //影视库海报的6个小背景图
    public static final int ID_FILM_LIBRARY_SUB1 = 601;
    public static final int ID_FILM_LIBRARY_SUB2 = 602;
    public static final int ID_FILM_LIBRARY_SUB3 = 603;
    public static final int ID_FILM_LIBRARY_SUB4 = 604;
    public static final int ID_FILM_LIBRARY_SUB5 = 605;
    public static final int ID_FILM_LIBRARY_SUB6 = 606;
    //最近观看
    public static final int ID_FILM_LIBRARY_RECENT_PLAYED = 610;
    //我的收藏
    public static final int ID_FILM_LIBRARY_FAVORITE = 620;

    //少儿
    public static final int ID_FILM_CHILDREN = 700;
    //少儿海报的6个小背景图
    public static final int ID_FILM_CHILDREN_SUB1 = 701;
    public static final int ID_FILM_CHILDREN_SUB2 = 702;
    public static final int ID_FILM_CHILDREN_SUB3 = 703;
    public static final int ID_FILM_CHILDREN_SUB4 = 704;
    public static final int ID_FILM_CHILDREN_SUB5 = 705;
    public static final int ID_FILM_CHILDREN_SUB6 = 706;

    //游戏
    public static final int ID_GAME = 800;
    //推荐游戏
    public static final int ID_GAME_RECOMMEND_SUB1 = 801;
    public static final int ID_GAME_RECOMMEND_SUB2 = 802;
    public static final int ID_GAME_RECOMMEND_SUB3 = 803;
    //已安装的游戏
    public static final int ID_GAME_INSTALLED_SUB1 = 811;
    public static final int ID_GAME_INSTALLED_SUB2 = 812;
    public static final int ID_GAME_INSTALLED_SUB3 = 813;
    //三个整合后的游戏id（用于launcher）
    public static final int ID_GAME_SUB1 = 821;
    public static final int ID_GAME_SUB2 = 822;
    public static final int ID_GAME_SUB3 = 823;
    //我的游戏
    public static final int ID_GAME_MINE = 830;

    //音乐
    public static final int ID_MUSIC = 900;
    //我的音乐
    public static final int ID_MUSIC_MINE = 910;
    //随便听听
    public static final int ID_MUSIC_RANDOM = 920;

    //应用市场
    public static final int ID_APP_MARKET = 1000;
    public static final int ID_APP_MRAKET_INSTALLED_SUB1 = 1001;
    public static final int ID_APP_MRAKET_INSTALLED_SUB2 = 1002;
    public static final int ID_APP_MRAKET_INSTALLED_SUB3 = 1003;
    //我的应用
    public static final int ID_APP_MARKET_MINE = 1010;

    //我的（菜单）
    public static final int ID_MENU_MINE = 1100;
    public static final int ID_MENU_MINE_VIP = 1110;

    //开机动画下载业务
    public static final int ID_BOOT_ANIMATION = 1200;
    //开机视频下载业务
    public static final int ID_BOOT_VIDEO = 1300;

    //购物卡片
    public static final int ID_SHOPPING = 1400;
    public static final int ID_SHOPPING_SUB1 = 1410;
    public static final int ID_SHOPPING_SUB2 = 1420;

    //体育卡片
    public static final int ID_SPORT = 1500;
    public static final int ID_SPORT_SUB1 = 1510;
    public static final int ID_SPORT_SUB2 = 1520;
*/
}
