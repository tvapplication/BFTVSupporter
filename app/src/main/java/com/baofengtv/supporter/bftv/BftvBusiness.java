package com.baofengtv.supporter.bftv;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baofengtv.supporter.AbstractBusiness;
import com.baofengtv.supporter.Constant;
import com.baofengtv.supporter.IntentUtils;
import com.baofengtv.supporter.PosterEntry;
import com.baofengtv.supporter.SlaveService;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.UMengUtils;
import com.baofengtv.supporter.Utils;
import com.baofengtv.supporter.bftv.json.BftvCover;
import com.baofengtv.supporter.bftv.json.SecondCover;
import com.baofengtv.supporter.database.ContentManager;
import com.baofengtv.supporter.loader.BftvDownloadParam;
import com.baofengtv.supporter.loader.DownloadParam;
import com.baofengtv.supporter.net.OkHttpUtils;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 获取TV窗口、暴风海报、轮播、速播、专场等主要业务海报
 * @date 2015/8/4
 */
public class BftvBusiness extends AbstractBusiness {

    public static final String APP_TOKEN = "c5c7ade9e97cf3d9ecddda3566b003ad";

    public static String VERSION = "1.0";
    public static String VERSION2 = "2.0";

    private Context mContext;

    private static BftvBusiness sInstance;

    //各一级业务海报封面
    private List<BftvCover> mEntryList = new ArrayList<BftvCover>();
    //专场的二级海报列表
    private List<SecondCover> mSpecialSecondPageList = new ArrayList<SecondCover>();

    private List<BftvDownloadParam> mDownloadList = new ArrayList<BftvDownloadParam>();

    public static BftvBusiness getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BftvBusiness(context);
        }
        return sInstance;
    }

    private BftvBusiness(Context context) {
        mContext = context;

        mTaskRunnable = new Runnable() {
            @Override
            public void run() {
                //1.请求所有海报的url地址
                mGetPosterUrlRunnable.run();
                //2.mEntryList得到赋值
                int size = mEntryList.size();
                if (size == 0) {
                    return;
                }
                clearDownloadList();
                //2.1逐个下载海报
                for (int i = 0; i < size; i++) {
                    BftvCover entry = mEntryList.get(i);
                    int businessId = convert2BusinessId(entry.getId());
                    if (businessId < 0) {
                        continue;
                    }

                    if (businessId == Constant.ID_TV_WINDOW /*|| businessId == Constant.ID_ADV_BAOFENG*/) {
                        //TV小窗口的海报不再下载
                        //后裔广告系统，不再下载小广告位
                        continue;
                    }
                    addDownloadPoster(entry.getCover(), businessId, entry.getUrl(), entry.getName(), entry.getMd5cover());

                    //下载backPic
                    List<BftvCover> subList = entry.getBackPicList();
                    if (subList != null) {
                        int subSize = subList.size();
                        for (int j = 0; j < subSize; j++) {
                            int subBusinessId = businessId + j + 1;
                            if (subBusinessId > 0) {
                                addDownloadPoster(subList.get(j).getCover(), subBusinessId, subList.get(j).getUrl(),
                                        subList.get(j).getName(), subList.get(j).getMd5cover());
                            }
                        }
                    }
                    //下拉button
                    List<BftvCover> subBtnList = entry.getButtonList();
                    if ((businessId == Constant.ID_SHOPPING || businessId == Constant.ID_SPORT)
                            && subBtnList != null) {
                        Trace.Debug("###sub button data");
                        writeSubBtn2DB(businessId, subBtnList);
                    }
                }
                //3.1获取专场的二级海报url
                getSpecialSecondPagePosterRunnable.run();
                //3.2 mSpecialSecondPageList得到赋值
                size = mSpecialSecondPageList.size();
                if (size == 0) {
                    return;
                }

                final int MAX_SPECIAL_ITEM = 10;
                int count = (size < MAX_SPECIAL_ITEM) ? size : MAX_SPECIAL_ITEM;
                for (int k = 0; k < count; k++) {
                    SecondCover secondSpecialCover = mSpecialSecondPageList.get(k);
                    //二级专场的图片不再由写死id转化，后台能删除，要灵活适配
                    //Constant.ID_SPECIAL_SECOND_SUB1起始
                    int subId = Constant.ID_SPECIAL_SECOND_SUB1 + k;
                    //Constant.ID_SPECIAL_SECOND_SUB1_BACKGROUND起始
                    int subBackId = Constant.ID_SPECIAL_SECOND_SUB1_BACKGROUND + k;
                    //二级专场未做MD5校验，最后一个参数传null

                    String name = secondSpecialCover.getName();
                    addDownloadPoster(secondSpecialCover.getCover(), subId,
                            secondSpecialCover.getUrl(), name, null);
                    addDownloadPoster(secondSpecialCover.getBackPic(), subBackId,
                            secondSpecialCover.getUrl(), name, null);

                }
                //如有上次旧的则删除
                int min = Constant.ID_SPECIAL + 1 + 10 + count;
                int max = Constant.ID_SPECIAL + 10 + 10;
                ContentManager.deletePosterDB(mContext, min, max);
                ContentManager.deletePosterDB(mContext, min + 10, max + 10);

                //开始下载
                downloadAllPosters(mDownloadList);
            }
        };
    }

    /**
     * 购物和体育卡片的下拉button写入数据库
     *
     * @param businessId
     * @param subBtnList
     */
    private void writeSubBtn2DB(final int businessId, List<BftvCover> subBtnList) {
        int subBtnSize = subBtnList.size(); //最多两个下拉button
        subBtnSize = subBtnSize < 2 ? subBtnSize : 2;
        for (int k = 0; k < subBtnSize; k++) {
            final int writeId = businessId + (k + 1) * 10;
            final String writeName = subBtnList.get(k).getName();
            final String writeIntent = subBtnList.get(k).getUrl();
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    PosterEntry writePoster = new PosterEntry();
                    writePoster.businessId = writeId;
                    writePoster.intent = writeIntent;
                    writePoster.desc = writeName;
                    writePoster.imagePath = "";
                    //写数据库
                    ContentManager.writePoster2DB(mContext, writePoster);
                }
            });
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public Runnable getTaskRunnable() {
        return mTaskRunnable;
    }

    //获取海报的url
    private Runnable mGetPosterUrlRunnable = new Runnable() {
        @Override
        public void run() {
            int retryCount = 3;
            String baseUrl = Constant.BFTV_BASE_URL;
            Map<String, String> params = new HashMap<String, String>();
            params.put("method", "bftv.launcher.list");
            params.put("apptoken", APP_TOKEN);
            params.put("version", VERSION);

            params.put("uuid", Utils.getSerialNumber(mContext));
            params.put("platform", Utils.getCurPlatform(mContext));
            params.put("mac", Utils.getEth0MacAddress(mContext));
            params.put("softid", Utils.getSoftId(mContext));
            params.put("sys_version", Utils.getSystemVersion(mContext));

            boolean ret = false;
            while (retryCount > 0) {
                Trace.Debug("retryCount = " + retryCount);
                try {
                    Map<String, String> resultMap = OkHttpUtils.doPost(baseUrl, params);
                    int statusCode = Integer.parseInt(resultMap.get(OkHttpUtils.CODE));
                    Trace.Debug("###statusCode=" + statusCode);
                    String content = resultMap.get(OkHttpUtils.RETURN);
                    Trace.Debug("###content=" + content);
                    if (statusCode == 200) {
                        List<BftvCover> list = parseJsonData(content);
                        if (list != null && list.size() > 0) {
                            mEntryList.clear();
                            mEntryList.addAll(list);
                        }
                        ret = true;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    MobclickAgent.reportError(mContext, e);
                }
                retryCount--;
            }
            Map<String, String> umengMap = new HashMap<String, String>();
            umengMap.put("value", "bftv.launcher.list:" + String.valueOf(ret));
            umengMap.put("interface", "bftv.launcher.list");
            SlaveService.onEvent(UMengUtils.EVENT_REQUEST_RESULT, umengMap);
        }
    };

    /**
     * 解析json数据
     *
     * @param jsonStr
     */
    private List<BftvCover> parseJsonData(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) {
            Trace.Warn("jsonStr is null!");
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(jsonStr);
            int statusCode = json.getIntValue("status");
            if (statusCode != 200) {
                Trace.Warn("###error! statusCode = " + statusCode);
                return null;
            }
            JSONArray jsonArray = json.getJSONArray("data");
            List<BftvCover> list = JSON.parseArray(jsonArray + "", BftvCover.class);
            if (list == null || list.size() == 0) {
                Trace.Warn("###parseJSONArray('data') failed!");
                return null;
            }

            int size = jsonArray.size();
            for (int i = 0; i < size; i++) {
                String jsonBackPic = jsonArray.getJSONObject(i).getString("backpic");
                String jsonButton = jsonArray.getJSONObject(i).getString("button");
                //解析backPic节点
                if (!TextUtils.isEmpty(jsonBackPic)) {
                    List<BftvCover> backCoverList = JSON.parseArray(jsonBackPic, BftvCover.class);
                    if (backCoverList != null && backCoverList.size() > 0) {
                        list.get(i).setBackPicList(backCoverList);
                    }
                }
                //解析button节点
                if (!TextUtils.isEmpty(jsonButton)) {
                    List<BftvCover> subBtnList = JSON.parseArray(jsonButton, BftvCover.class);
                    if (subBtnList != null && subBtnList.size() > 0) {
                        list.get(i).setButtonList(subBtnList);
                    }
                }
                Trace.Debug(i + ": " + list.get(i).toString());
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            MobclickAgent.reportError(mContext, e);
            return null;
        }
    }

    //获取专场的二级页面海报url
    private Runnable getSpecialSecondPagePosterRunnable = new Runnable() {
        @Override
        public void run() {
            int retryCount = 3;
            String baseUrl = Constant.BFTV_BASE_URL;
            Map<String, String> params = new HashMap<String, String>();
            params.put("method", "bftv.launcher.sublist");
            params.put("apptoken", APP_TOKEN);
            params.put("version", VERSION);
            params.put("id", "6");      //6是专场的id

            //LiLiang added on 2016-3-29
            params.put("uuid", Utils.getSerialNumber(mContext));
            params.put("platform", Utils.getCurPlatform(mContext));
            params.put("mac", Utils.getEth0MacAddress(mContext));
            params.put("softid", Utils.getSoftId(mContext));
            params.put("sys_version", Utils.getSystemVersion(mContext));

            boolean ret = false;
            while (retryCount > 0) {
                Trace.Debug("retryCount = " + retryCount);
                try {
                    Map<String, String> resultMap = OkHttpUtils.doPost(baseUrl, params);
                    int statusCode = Integer.parseInt(resultMap.get(OkHttpUtils.CODE));
                    Trace.Debug("###statusCode=" + statusCode);
                    String content = resultMap.get(OkHttpUtils.RETURN);
                    Trace.Debug("###content=" + content);
                    if (statusCode == 200) {
                        List<SecondCover> list = parseSecondCoverJsonData(content);
                        if (list != null && list.size() > 0) {
                            mSpecialSecondPageList.clear();
                            int size = list.size();
                            for (int i = 0; i < size; i++) {
                                //将6张小背景图过滤掉，只存二级专场
                                if (!TextUtils.isEmpty(list.get(i).getBackPic())) {
                                    mSpecialSecondPageList.add(list.get(i));
                                }
                            }
                        }
                        ret = true;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    MobclickAgent.reportError(mContext, e);
                }
                retryCount--;
            }
            Map<String, String> umengMap = new HashMap<String, String>();
            umengMap.put("value", "bftv.launcher.sublist-special:" + String.valueOf(ret));
            umengMap.put("interface", "bftv.launcher.sublist-special");
            SlaveService.onEvent(UMengUtils.EVENT_REQUEST_RESULT, umengMap);
        }
    };

    /**
     * 解析json数据
     *
     * @param jsonStr
     */
    private List<SecondCover> parseSecondCoverJsonData(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) {
            Trace.Warn("jsonStr is null!");
            return null;
        }
        JSONObject json = JSON.parseObject(jsonStr);
        int statusCode = json.getIntValue("status");
        if (statusCode != 200) {
            Trace.Warn("###error! statusCode = " + statusCode);
            return null;
        }
        JSONArray jsonArray = json.getJSONArray("data");
        List<SecondCover> list = JSON.parseArray(jsonArray + "", SecondCover.class);
        if (list == null || list.size() == 0) {
            Trace.Warn("###parseJSONArray('data') failed!");
            return null;
        }

        int size = jsonArray.size();
        for (int i = 0; i < size; i++) {
            Trace.Debug(i + ": " + list.get(i).toString());
        }

        return list;
    }

    /**
     * 下载海报，并将url、业务id、本地下载的文件路径建立数据库存储
     *
     * @param imageUrl      网络海报的url下载地址
     * @param businessId    业务id
     * @param serverFileMD5 服务端返回的海报md5值，如果不为空就与本地文件做md5一致性校验
     */
    private void addDownloadPoster(String imageUrl, int businessId,
                                String jumpIntent, String name, String serverFileMD5) {
        if (TextUtils.isEmpty(imageUrl)) {
            return;
        }

        if ((businessId >= Constant.ID_SPECIAL_SECOND_SUB1 && businessId <= Constant.ID_SPECIAL_SECOND_SUB_MAX)) {
            if (!TextUtils.isEmpty(jumpIntent) && !jumpIntent.startsWith("http")) {
                String pkgName = IntentUtils.getPkgNameByJsonIntent(jumpIntent);
                if (!TextUtils.isEmpty(pkgName) && !Utils.isPackageInstalled(mContext, pkgName)) {
                    //二级专场，下一跳所对应的启动package未安装
                    if (IntentUtils.hasUninstalledJsonIntent(jumpIntent)) {
                        //json指令串包含uninstalledContent字段，继续写入数据库
                        Trace.Debug(pkgName + " uninstalled. but has uninstalledContent key. write 2 db. businessId=" + businessId);
                    } else {
                        //不写入数据库，抛弃
                        Trace.Debug(pkgName + " uninstalled. don't download. don't write 2 db. businessId=" + businessId);
                        //如果对应id已有数据库记录，则删除
                        ContentManager.deletePosterDB(mContext, businessId);
                        return;
                    }
                }
            }

            //LiLiang added on 2016-9-5. 根据baofengtv.en.VR属性判断是否支持VR
            if (name != null && name.startsWith("VR")) {
                boolean isSupportVR = Utils.isSupportVR();
                if (!isSupportVR) {
                    Trace.Debug("no support VR. don't show VR card");
                    int ret = ContentManager.deletePosterDB(mContext, businessId);
                    Trace.Debug("###delete vr db return " + ret);
                    return;
                }
            }
        } else if (businessId >= Constant.ID_ADV_BAOFENG_SECOND && businessId <= Constant.ID_ADV_BAOFENG_SECOND_MAX ||
                businessId == Constant.ID_QUICK_PLAY) {
            //广告位大图或速播（速播卡片用来运营其他入口）
            if (!TextUtils.isEmpty(jumpIntent) && !jumpIntent.startsWith("http")) {
                String pkgName = IntentUtils.getPkgNameByJsonIntent(jumpIntent);
                if (!TextUtils.isEmpty(pkgName) && !Utils.isPackageInstalled(mContext, pkgName)) {
                    if (IntentUtils.hasUninstalledJsonIntent(jumpIntent)) {
                        Trace.Debug(pkgName + " uninstalled. but has uninstalledContent key. write 2 db. businessId=" + businessId);
                    } else {
                        Trace.Debug(pkgName + " uninstalled. don't download. don't write 2 db. businessId=" + businessId);
                        ContentManager.deletePosterDB(mContext, businessId);
                        return;
                    }
                }
            }
        }

        final String md5 = Utils.getMD5(imageUrl) + Utils.getPostfix(imageUrl);
        PosterEntry readPoster = ContentManager.readPosterFromDB(mContext, businessId);
        Trace.Debug("###readPoster=" + readPoster);
        //是否需要下载
        boolean flag = false;
        if (TextUtils.isEmpty(readPoster.imagePath)) {
            flag = true;
        } else {
            File file = new File(readPoster.imagePath);
            if (!file.exists()) {
                flag = true;
            } else if (file.length() == 0) {
                file.delete();
                flag = true;
            } else if (!(readPoster.imagePath.equals(Constant.CACHED_DIR + md5))) {
                file.delete();
                flag = true;
            }
        }

        //没有缓存，需要下载
        if (flag) {
            BftvDownloadParam param = new BftvDownloadParam();
            param.src = imageUrl;
            param.dst = Constant.CACHED_DIR + md5;
            param.srcMD5 = serverFileMD5;
            param.businessId = businessId;
            param.cardName = name;
            param.intent = jumpIntent;
            //加入下载列表
            addDownloadItem(param);
        } else {
            //已有缓存无需下载网络海报
            Trace.Debug("###get poster from cache. no need to download it. businessId="
                    + readPoster.businessId);
            //但如果intent和desc(name)发生变化要更新数据库相应字段
            boolean needUpdate = false;
            //1.比对intent字段
            if (TextUtils.isEmpty(readPoster.intent)) {
                if (!TextUtils.isEmpty(jumpIntent)) {
                    needUpdate = true;
                }
            } else {
                if (TextUtils.isEmpty(jumpIntent)) {
                    needUpdate = true;
                } else {
                    if (!readPoster.intent.equals(jumpIntent)) {
                        needUpdate = true;
                    }
                }
            }
            //2.比对desc(name)字段
            if (TextUtils.isEmpty(readPoster.desc)) {
                if (!TextUtils.isEmpty(name)) {
                    needUpdate = true;
                }
            } else {
                if (TextUtils.isEmpty(name)) {
                    needUpdate = true;
                } else {
                    if (!readPoster.desc.equals(name)) {
                        needUpdate = true;
                    }
                }
            }

            if (needUpdate) {
                readPoster.intent = jumpIntent;
                readPoster.desc = name;
                //写数据库
                ContentManager.writePoster2DB(mContext, readPoster);
            }
        }
    }

    private int convert2BusinessId(int id) {
        switch (id) {
            case 2:
                return Constant.ID_TV_WINDOW;
            case 3:
                return Constant.ID_ADV_BAOFENG;
            case 4:
                return Constant.ID_TURN_PLAY;
            case 5:
                return Constant.ID_QUICK_PLAY;
            case 6:
                return Constant.ID_SPECIAL;
            case 7:
                return Constant.ID_FILM_LIBRARY;
            case 8:
                return Constant.ID_FILM_CHILDREN;
            case 9:
                return Constant.ID_GAME;
            case 10:
                return Constant.ID_MUSIC;
            case 11:
                return Constant.ID_APP_MARKET;
            case 15:
                return Constant.ID_SPORT;
            case 16:
                return Constant.ID_SHOPPING;
            default:
                return -100;
        }
    }

    private void clearDownloadList(){
        mDownloadList.clear();
    }

    private void addDownloadItem(BftvDownloadParam param){
        mDownloadList.add(param);
    }

    private void downloadAllPosters(List<BftvDownloadParam> params) {
        if(params == null || params.size() == 0){
            return;
        }
        Observable.from(params) //List逐一onNext
                .map(new Func1<BftvDownloadParam, BftvDownloadParam>() {//map事件加工：发生在io线程
                    @Override
                    public BftvDownloadParam call(BftvDownloadParam downloadParam) {
                        Trace.Debug("[RxJava download]" + downloadParam.toString());
                        try {
                            int statusCode = OkHttpUtils.doDownload(downloadParam);
                            downloadParam.code = statusCode;
                        } catch (IOException e) {
                            e.printStackTrace();
                            downloadParam.code = -1;
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                            downloadParam.code = -1;
                        }
                        return downloadParam;
                    }
                })
                .subscribeOn(Schedulers.io())
                //subscribe事件消费,默认在当前线程
                .subscribe(onNextAction, onErrorAction, onCompletedAction);
    }

    final Action1<BftvDownloadParam> onNextAction = new Action1<BftvDownloadParam>() {
        @Override
        public void call(BftvDownloadParam param) {
            Trace.Debug("[RxJava download finished]" + param.toString());
            boolean isSucc = (param.code == OkHttpUtils.CODE_OK || param.code == OkHttpUtils.CODE_RESUME_OK);
            if (isSucc) {
                //增加文件md5一致性比较
                File file = new File(param.dst);
                boolean needWrite2DB = true;
                if (file.exists() && file.length() == 0) {
                    file.delete();
                    needWrite2DB = false;
                }

                if (!TextUtils.isEmpty(param.srcMD5)) {
                    param.dstMD5 = Utils.fileMD5(param.dst);
                    Trace.Debug("###local MD5 = " + param.dstMD5);
                    if (!TextUtils.isEmpty(param.dstMD5) &&
                            !(param.srcMD5).equalsIgnoreCase(param.dstMD5)) {
                        SlaveService.onEvent(UMengUtils.EVENT_MD5_DISMATCH);
                        Trace.Warn("###md5 dismatch delete downloaded file. " + param.businessId);

                        if (file.exists()) {
                            file.delete();
                        }
                        needWrite2DB = false;
                    }
                }

                if (needWrite2DB) {
                    PosterEntry writePoster = new PosterEntry();
                    writePoster.businessId = param.businessId;
                    writePoster.url = param.src;
                    writePoster.imagePath = param.dst;
                    writePoster.intent = param.intent;
                    writePoster.desc = param.cardName;
                    //写数据库
                    ContentManager.writePoster2DB(mContext, writePoster);
                }
            }
            Map<String, String> umengMap = new HashMap<String, String>();
            umengMap.put("value", String.valueOf(param.businessId) + ":" + String.valueOf(isSucc));
            umengMap.put("business_id", String.valueOf(param.businessId));
            SlaveService.onEvent(UMengUtils.EVENT_DOWNLOAD_RESULT, umengMap);
        }
    };

    Action1<Throwable> onErrorAction = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            // Error handling
        }
    };

    Action0 onCompletedAction = new Action0() {
        @Override
        public void call() {
            Trace.Debug("download all posters completed!!!");
            SlaveService.getInstance().notifyDownloadPostersCompleted();
        }
    };
}
