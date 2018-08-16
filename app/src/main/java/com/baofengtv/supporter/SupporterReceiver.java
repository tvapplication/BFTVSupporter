package com.baofengtv.supporter;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.baofengtv.middleware.tv.BFTVCommonManager;
import com.baofengtv.middleware.tv.BFTVSoundManager;
import com.baofengtv.supporter.net.OkHttpUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 上报开机音量、环绕箱插拔事件监听；
 * 上报外脑使用情况（版本，软硬采，是否插入正确）；
 * 上报无线网络使用情况（强度、信道、scan个数、路由型号）；
 * 上报遥控按键使用情况；
 * @company 暴风TV
 * @created 2018/2/1 19:49
 * @changeRecord [修改记录] <br/>
 */

public class SupporterReceiver extends BroadcastReceiver {

    public static final String KEY_VOL = "volume";

    private Thread mMonitorThread;

    private static DecimalFormat mDecimalFormat = new DecimalFormat("0.0");

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context.getApplicationContext();
        String action = intent.getAction();
        Trace.Debug("get broadcast " + action);
        //1.开机广播
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent service = new Intent(context, SlaveService.class);
            context.startService(service);

            mMonitorThread = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    //开机统计-系统开机音量
                    reportVolumeWhenBoot(mContext);
                    //开机统计-外脑连接状态
                    reportAIMicWhenBoot(mContext);
                    //统计rom版本
                    reportROMInfo(mContext);
                    //按键事件统计
                    reportKeyEvent(mContext);
                }
            };
            mMonitorThread.start();
        }
        //2.usb有设备插入
        else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            UsbDevice usbdevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbdevice == null)
                return;

            Trace.Debug("product_id" + usbdevice.getProductId() + "vendor_id" + usbdevice.getVendorId());
            if ((usbdevice.getProductId() == 0x5020) && (usbdevice.getVendorId() == 0x0416)) {
                //大耳朵外脑插入
                reportAIMicPlug(context);
            } else if ((usbdevice.getProductId() == 22352) && (usbdevice.getVendorId() == 1155)) {
                //环绕音箱dongle
                Trace.Debug("surround box connected");
                notifySurroundBoxChanged(context.getApplicationContext(), 1);
            }
        }
        //3.usb有设备拔出
        else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            UsbDevice usbdevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbdevice == null)
                return;

            if ((usbdevice.getProductId() == 0x5020) && (usbdevice.getVendorId() == 0x0416)) {
                //大耳朵外脑被拔掉
                reportAIMicInject();
            } else if ((usbdevice.getProductId() == 22352) && (usbdevice.getVendorId() == 1155)) {
                //环绕音箱dongle
                Trace.Debug("surround box inject");
                notifySurroundBoxChanged(context.getApplicationContext(), 0);
            }
        }
        //4.蓝牙设备连接
        else if (action.equals("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED")) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null)
                return;
            String deviceName = device.getName();
            if (deviceName != null && deviceName.equals("暴风蓝牙遥控器")) {
                Trace.Debug("bluetooth remote connected");
                HashMap umengMap = new HashMap();
                umengMap.put("platform", "connected-" + Utils.getCurPlatform(context));
                SlaveService.onEvent(UMengUtils.EVENT_STATUS_BLUETOOTH_REMOTE, umengMap);
            }
        } else if (action.equals("baofengtv.action.NO_OPERATION_START_APP")) {
            //执行测速
            File file = new File("/data/data/com.baofengtv.supporter/cache/test_speed");
            boolean testSpeed = !file.exists();
            Trace.Debug("need test speed ? " + testSpeed);
            if (testSpeed) {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                        reportPing();
                        reportNetworkSpeed();
                    }
                }.start();
            }

        }
    }

    //开机统计-上报系统音量
    private void reportVolumeWhenBoot(Context context) {
        Trace.Debug("reportVolumeWhenBoot()");
        int volume = BFTVSoundManager.getInstance(context).getVolume();
        String platform = Utils.getCurPlatform(context);
        SimpleDateFormat df = new SimpleDateFormat("HH");//设置日期格式
        String date = df.format(new Date());

        HashMap umengMap = new HashMap();
        umengMap.put(KEY_VOL, String.valueOf(volume));
        umengMap.put("vol_hour", String.valueOf(volume) + "@" + date);
        umengMap.put("vol_platform", String.valueOf(volume) + "@" + platform);

        SlaveService.onEvent(UMengUtils.EVENT_VOL_BOOT, umengMap);
    }

    //开机统计-外脑连接状态
    public static void reportAIMicWhenBoot(Context context) {
        Trace.Debug("reportAIMicWhenBoot()");
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> usbDeviceMap = usbManager.getDeviceList();

            if (usbDeviceMap == null)
                return;

            Iterator<UsbDevice> it = usbDeviceMap.values().iterator();
            while (it.hasNext()) {
                UsbDevice device = (UsbDevice) it.next();
                Trace.Debug("usb device vid:" + device.getVendorId() + " pid:" + device.getProductId());
                Trace.Debug("usb device name:" + device.getDeviceName());
                if (Build.VERSION.SDK_INT >= 21) {
                    String productName = device.getProductName();
                    Trace.Debug("usb device product name:" + productName);
                    Trace.Debug("usb device manufacture name:" + device.getManufacturerName());

                    if (productName != null && (productName.toLowerCase()).contains("bftv smart mic")) {
                        String serialNumber = device.getSerialNumber();
                        if (TextUtils.isEmpty(serialNumber))
                            return;

                        //外脑名字+版本
                        if (serialNumber.contains("MICS_BF") && serialNumber.length() > 13) {
                            //len of "IFLY_MICS_BF_" is 13
                            String micVer = serialNumber.substring(13);
                            String name = "AI-" + (serialNumber.charAt(14) - '0' + 1);
                            HashMap umengMap = new HashMap();
                            umengMap.put("name@version", name + "@" + micVer);
                            umengMap.put("name@version@platform", name + "@" + micVer + "@" + Utils.getCurPlatform(context));
                            SlaveService.onEvent(UMengUtils.EVENT_STATUS_AI_MIC, umengMap);
                        } else if (serialNumber.contains("USBMIC") && serialNumber.length() > 10) {
                            //len of "USBMIC" is 6
                            String micVer2 = serialNumber.substring(6, 11);
                            String name2 = "AI-" + serialNumber.substring(7, 8) + "Y";
                            HashMap umengMap = new HashMap();
                            umengMap.put("name@version", name2 + "@" + micVer2);
                            umengMap.put("name@version@platform", name2 + "@" + micVer2 + "@" + Utils.getCurPlatform(context));
                            SlaveService.onEvent(UMengUtils.EVENT_STATUS_AI_MIC, umengMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reportWifi(Context context) {
        Trace.Debug("reportWifi()");

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if(networkInfo == null)
            return;
        int networkType = networkInfo.getType();
        Trace.Debug("network type = " + networkType);
        if(networkType != ConnectivityManager.TYPE_WIFI)
            return;

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //wifi热点列表
        List<ScanResult> wifiList = wifiManager.getScanResults();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiList == null || wifiInfo == null)
            return;

        HashMap umengMap = new HashMap();
        umengMap.put("scan_counts", wifiList.size());
        String bssid = wifiInfo.getBSSID();
        if(bssid != null && bssid.length() > 8) {
            umengMap.put("connected_bssid", bssid.substring(0,8));
        }
        //查找已连接wifi信号强度
        String connectedSSID = wifiInfo.getSSID().replace("\"", "");
        StringBuilder all_level = new StringBuilder("");
        StringBuilder low_conn_detail = new StringBuilder("");
        String type = "-2.4G";
        for (ScanResult scanResult : wifiList) {
            String ssid2 = scanResult.SSID.replace("\"", "");
            all_level.append("[" + String.valueOf(scanResult.level) + "|" +
                    String.valueOf(getChannelByFrequency(scanResult.frequency)) + "]");
            all_level.append("#");
            if (connectedSSID.equals(ssid2)) {
                Trace.Debug("level = " + scanResult.level);
                umengMap.put("connected_level", String.valueOf(scanResult.level));

                int channel = getChannelByFrequency(scanResult.frequency);
                umengMap.put("connected_channel", String.valueOf(channel));
                if (scanResult.frequency > 5000)
                    type = "-5G";
                if(scanResult.level <= -70){
                    low_conn_detail.append(Utils.getSerialNumber(context) + "|" + scanResult.level);
                    String var = new String(low_conn_detail);
                    umengMap.put("low_connected_uuid", var);
                    low_conn_detail.append("|" + channel);
                }
                umengMap.put("connected_link_speed_level",
                        String.valueOf(wifiInfo.getLinkSpeed()) + "|" + type + "|" +  String.valueOf(scanResult.level));
            }
        }

        String host = intToIp(wifiManager.getDhcpInfo().gateway);
        String filePath = wgetHost(host, context);
        try {
            Thread.sleep(800);
        } catch (Exception e) {
        }
        String wifiModel = parseFile(filePath);
        Trace.Debug("model = " + wifiModel);
        umengMap.put("model", wifiModel);
        umengMap.put("all_level_channel", all_level.toString());
        umengMap.put("connected_link_speed", String.valueOf(wifiInfo.getLinkSpeed()));
        if(low_conn_detail.length() > 0) {
            low_conn_detail.append("|" + wifiModel + "|" + wifiInfo.getLinkSpeed() + "|" + type);
            umengMap.put("low_connected_detail", low_conn_detail.toString());
        }
        SlaveService.onEvent(UMengUtils.EVENT_STATUS_WIFI, umengMap);
    }

    private void reportKeyEvent(Context context) {
        if (Utils.getCurPlatform(context).equals("MST_6A838"))
            return;
        Process process = null;
        BufferedReader reader = null;
        DataOutputStream os = null;
        try {
            String[] getLogArray = new String[]{"logcat", "-v", "time", "-s", "EventHub:I"};

            process = Runtime.getRuntime().exec(getLogArray);//抓取当前的缓存日志
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int index = -1;
            String codeStr = "code=";
            String valueStr = "value=";
            String tmp1, value;
            while ((line = reader.readLine()) != null) {
                if (line.contains("vendor")) {
                    if ((index = line.indexOf(codeStr)) > 0) {
                        tmp1 = line.substring(index + 5);
                        value = tmp1.substring(tmp1.indexOf(valueStr) + 6);
                        if (value.equals("1")) {
                            String code = tmp1.substring(0, tmp1.indexOf(','));
                            if (code.equals("0") || code.equals("1")) {
                                continue;
                            }
                            String androidCode = convert2AndroidKeyCode(code);
                            if (androidCode == null) {
                                //转换失败
                                SlaveService.onEvent(UMengUtils.KEY_EVENT, Utils.curPlatform + line.substring(line.indexOf("vendor")));
                            } else {
                                SlaveService.onEvent(UMengUtils.KEY_EVENT, androidCode);
                                //Trace.Debug("keycode = " + androidCode);
                            }

                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    //统计AIOS3.0以上版本
    private void reportROMInfo(Context context) {
        Trace.Debug("reportROMInfo()");
        String version = Utils.getSystemVersion(context).toLowerCase();
        HashMap umengMap = new HashMap();
        String ret;
        if (version.compareTo("v4.0") > 0) {
            ret = "v4.+";
        } else {
            ret = "other";
        }
        umengMap.put("version", ret);
        umengMap.put("platform@version", Utils.curPlatform + " " + ret);
        SlaveService.onEvent(UMengUtils.EVENT_ROM_INFO, umengMap);
    }

    private int totalBytes = -1;
    private boolean flag = true;

    //网络测速
    private void reportNetworkSpeed() {
        Trace.Debug("reportNetworkSpeed()");
        Map<String, String> params = new HashMap<String, String>();
        params.put("method", "bftv.launcher.speed");
        params.put("apptoken", "c5c7ade9e97cf3d9ecddda3566b003ad");
        params.put("version", "2.0");
        Map<String, String> resultMap = null;
        String downloadUrl = null;
        try {
            resultMap = OkHttpUtils.doPost("http://ptbftv.gitv.tv/", params);
            int stausCode = Integer.parseInt(resultMap.get("code"));
            String content = resultMap.get("return");
            int indexstart = content.indexOf("http");
            int indexend = content.lastIndexOf("\"");
            if (stausCode == 200 && indexstart != -1)
                downloadUrl = content.substring(indexstart, indexend).replace("\\", "");
            Trace.Debug("###statusCode=" + stausCode + " downloadurl  = " + downloadUrl + " " +
                    "start" + indexstart);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(downloadUrl)) {
            downloadUrl = "http://log.fengmi.tv/speed.mp4";
        }

        flag = true;
        new CalculateSpeedThread().start();

        URL url;
        URLConnection connection;
        final InputStream inStream;
        try {
            url = new URL(downloadUrl);
            connection = url.openConnection();
            inStream = connection.getInputStream();
            totalBytes = connection.getContentLength();
            int read = 0;
            byte[] buf = new byte[4096];
            while (read != -1 && flag) {
                read = inStream.read(buf);
                //Trace.Debug("read buffer " + read);
            }
            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reportPing() {
        Trace.Debug("reportPing()");
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        String gateway = intToIp(wifiManager.getDhcpInfo().gateway);

        try {
            double delay1 = getPingDelay(gateway);
            double delay2 = getPingDelay("www.baidu.com");
            String delayStr1 = mDecimalFormat.format(delay1) + "ms";
            String delayStr2 = mDecimalFormat.format(delay2) + "ms";
            Trace.Debug("ping gateway delay " + delayStr1);
            Trace.Debug("ping server delay " + delayStr2);

            HashMap umengMap = new HashMap();
            umengMap.put("ping_gateway", delayStr1);
            umengMap.put("ping_server", delayStr2);
            umengMap.put("ping_server_uuid", delayStr2 + " " + Utils.getSerialNumber(mContext));

            SlaveService.onEvent(UMengUtils.EVENT_PING, umengMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getPingDelay(String host) throws IOException {
        double delay = 0;
        String delayStr;
        Process p;
        String key = "time=";
        p = Runtime.getRuntime().exec("/system/bin/ping -c 5 " + host);
        BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String str;
        while ((str = buf.readLine()) != null) {
            //Trace.Debug("line = " + str);
            if (str.contains(key)) {
                int i = str.indexOf(key);
                int j = str.indexOf(" ", i);

                delayStr = str.substring(i + 5, j);
                //Trace.Debug("delay:" + delayStr);
                delay += Double.parseDouble(delayStr);
            }
        }
        return delay/5;
    }

    //统计上报外脑插入事件
    private void reportAIMicPlug(Context context) {
        int code = isAIMicPlugCorrect(context.getApplicationContext());
        Trace.Debug("code = " + code);
        if (code == 0) {
            Trace.Debug("Plug  correct");
            if (errCount > 0) {
                //统计插入成功率
                String rate = errCount + "/" + (errCount + 1); //错误次数/错误次数+1
                SlaveService.onEvent(UMengUtils.EVENT_AI_MIC_PLUG_RATE, String.valueOf(rate));
            } else if (errCount == 0) { //一次插入成功
                SlaveService.onEvent(UMengUtils.EVENT_AI_MIC_PLUG_RATE, "1");
            }
            errCount = 0;
        } else {
            Trace.Debug("Plug error");
        }
        Trace.Debug("onEvent(UMengUtils.EVENT_AI_MIC_PLUG)");
        //统计每次插入结果[成功(0)|失败(-1)|异常(-100)]
        SlaveService.onEvent(UMengUtils.EVENT_AI_MIC_PLUG, String.valueOf(code));
    }

    //统计上报外脑拔出事件
    private void reportAIMicInject() {
        SlaveService.onEvent(UMengUtils.EVENT_AI_MIC_INJECT);
    }

    //大耳朵插入错误次数
    private static int errCount = 0;

    /**
     * 大耳朵外脑是否插入正确
     *
     * @param context
     * @return
     */
    private int isAIMicPlugCorrect(Context context) {
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> usbDeviceMap = usbManager.getDeviceList();

            if (usbDeviceMap == null)
                return -100;

            Iterator<UsbDevice> it = usbDeviceMap.values().iterator();
            while (it.hasNext()) {
                UsbDevice device = (UsbDevice) it.next();
                Trace.Debug("usb device vid:" + device.getVendorId() + " pid:" + device.getProductId());
                Trace.Debug("usb device name:" + device.getDeviceName());
                if (Build.VERSION.SDK_INT >= 21) {
                    String productName = device.getProductName();
                    Trace.Debug("usb device product name:" + productName);
                    Trace.Debug("usb device manufacture name:" + device.getManufacturerName());

                    if (productName != null && (productName.toLowerCase()).contains("bftv smart mic")) {
                        String serialNumber = device.getSerialNumber();
                        if (TextUtils.isEmpty(serialNumber))
                            return -100;

                        //外脑名字+版本
                        if (serialNumber.contains("MICS_BF") && serialNumber.length() > 13) {
                            //len of "IFLY_MICS_BF_" is 13
                            String micVer = serialNumber.substring(13);
                            String name = "AI-" + (serialNumber.charAt(14) - '0' + 1);

                            SlaveService.onEvent(UMengUtils.EVENT_AI_MIC_VERSION, name + "@" + micVer);
                        } else if (serialNumber.contains("USBMIC") && serialNumber.length() > 10) {
                            //len of "USBMIC" is 6
                            String micVer2 = serialNumber.substring(6, 11);
                            String name2 = "AI-" + serialNumber.substring(7, 8) + "Y";

                            SlaveService.onEvent(UMengUtils.EVENT_AI_MIC_VERSION, name2 + "@" + micVer2);
                        }

                        if (serialNumber.contains("USBMIC")) {
                            //硬采
                            String platform = BFTVCommonManager.getInstance(context).getPlatform();
                            String filename1;
                            String filename2;
                            if (platform.equals("MST_6A838")) {
                                filename1 = "/sys/devices/Mstar-ehci-2.12/usb1/1-1/1-1.1/serial";
                                filename2 = "/sys/devices/Mstar-ehci-2.12/usb1/1-1/1-1.2/serial";
                            } else if (platform.equals("AML_T962")) {
                                filename1 = "/sys/devices/c9000000.dwc3/xhci-hcd.0.auto/usb1/1-3/1-3.1/serial";
                                filename2 = "/sys/devices/c9000000.dwc3/xhci-hcd.0.auto/usb1/1-3/1-3.2/serial";
                            } else {
                                return -100;
                            }

                            int code1 = isHardMic(filename1);
                            int code2 = isHardMic(filename2);
                            if ((code1 == 0) || (code2 == 0)) {
                                return 0;
                            } else if ((code1 == -1) && (code2 == -1)) {
                                errCount++;
                                return -1;
                            } else {
                                return 0;
                            }
                        } else {
                            //软采
                            return 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -100;
    }

    private int isHardMic(String filename) {
        int ret = -1;
        File file = new File(filename);
        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(reader);
                String serial = br.readLine();
                if (serial != null && serial.contains("USBMIC")) {
                    ret = 0;
                }
                br.close();
                reader.close();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
                ret = -100;
            }
        }
        return ret;
    }

    private void notifySurroundBoxChanged(Context context, int state) {
        Intent intent = new Intent("com.baofengtv.action.SURROUND_BOX_STATE_CHANGED");
        //int型state=1插入，state=0拔出
        intent.putExtra("state", state);
        Trace.Debug("send broadcast com.baofengtv.action.SURROUND_BOX_STATE_CHANGED");
        context.sendBroadcast(intent);
    }

    private String convert2AndroidKeyCode(String code) {
        if (EventHubMap.containsKey(code)) {
            int androidKeyCode = EventHubMap.get(code);
            return String.valueOf(androidKeyCode) + KeyNameMap.get(androidKeyCode);
        }
        return null;
    }

    public static HashMap<String, Integer> EventHubMap;
    public static HashMap<Integer, String> KeyNameMap;

    static {
        EventHubMap = new HashMap<>();
        EventHubMap.put("105", KeyEvent.KEYCODE_DPAD_LEFT);
        EventHubMap.put("106", KeyEvent.KEYCODE_DPAD_RIGHT);
        EventHubMap.put("103", KeyEvent.KEYCODE_DPAD_UP);
        EventHubMap.put("108", KeyEvent.KEYCODE_DPAD_DOWN);

        EventHubMap.put("28", KeyEvent.KEYCODE_DPAD_CENTER);
        EventHubMap.put("703", KeyEvent.KEYCODE_DPAD_CENTER);
        EventHubMap.put("96", KeyEvent.KEYCODE_DPAD_CENTER);
        EventHubMap.put("353", KeyEvent.KEYCODE_DPAD_CENTER);//蓝牙

        EventHubMap.put("102", KeyEvent.KEYCODE_HOME);
        EventHubMap.put("172", KeyEvent.KEYCODE_HOME);

        EventHubMap.put("15", KeyEvent.KEYCODE_BACK);//866
        EventHubMap.put("158", KeyEvent.KEYCODE_BACK);

        EventHubMap.put("139", KeyEvent.KEYCODE_MENU);
        EventHubMap.put("127", KeyEvent.KEYCODE_MENU);

        EventHubMap.put("114", KeyEvent.KEYCODE_VOLUME_DOWN);
        EventHubMap.put("115", KeyEvent.KEYCODE_VOLUME_UP);

        EventHubMap.put("904", 733); //866
        EventHubMap.put("594", 733); //358 639
        EventHubMap.put("548", 733); //968
        EventHubMap.put("87", 733); //962
        EventHubMap.put("88", 733); //长按biu

        EventHubMap.put("116", KeyEvent.KEYCODE_POWER);//866 639 962
        EventHubMap.put("596", KeyEvent.KEYCODE_POWER);//358

        EventHubMap.put("67", 67); //调焦-
        EventHubMap.put("68", 68); //调焦+

        KeyNameMap = new HashMap<>();
        KeyNameMap.put(KeyEvent.KEYCODE_DPAD_LEFT, "左键");
        KeyNameMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, "右键");
        KeyNameMap.put(KeyEvent.KEYCODE_DPAD_UP, "上键");
        KeyNameMap.put(KeyEvent.KEYCODE_DPAD_DOWN, "下键");
        KeyNameMap.put(KeyEvent.KEYCODE_DPAD_CENTER, "OK键");
        KeyNameMap.put(KeyEvent.KEYCODE_HOME, "首页键");
        KeyNameMap.put(KeyEvent.KEYCODE_BACK, "返回键");
        KeyNameMap.put(KeyEvent.KEYCODE_MENU, "菜单键");
        KeyNameMap.put(KeyEvent.KEYCODE_VOLUME_DOWN, "音量-键");
        KeyNameMap.put(KeyEvent.KEYCODE_VOLUME_UP, "音量+键");
        KeyNameMap.put(733, "BIU键");
        KeyNameMap.put(67, "调焦-键");
        KeyNameMap.put(68, "调焦+键");
        KeyNameMap.put(KeyEvent.KEYCODE_POWER, "待机键");
    }

    ;

    /**
     * 根据频率获得信道
     *
     * @param frequency
     * @return
     */
    public static int getChannelByFrequency(int frequency) {
        return frequency > 5000 ? (frequency - 5000) / 5 : (frequency - 2407) / 5;
    }

    /**
     * 获取路由器设备信息
     *
     * @param host 要连接的域名
     */
    private static String wgetHost(String host, Context context) {
        Trace.Debug("wget_host=" + host);
//        host = "172.19.8.12";
        BufferedReader in = null;
        Runtime r = Runtime.getRuntime();
        String path = context.getCacheDir().getAbsolutePath();
        if (!path.endsWith("/")) {
            path = context.getCacheDir() + "/index.txt";
        } else {
            path = context.getCacheDir() + "index.txt";
        }
        //Trace.Debug("wget_host =" + host + " path = " + path);
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        String pingCommand;
        if (BFTVCommonManager.getInstance(context).getPlatform().contains("AML")) {
            pingCommand = "wget http://" + host + " -O " + path;
        } else {
            pingCommand = "busybox wget http://" + host + " -O " + path;
        }

        try {
            Process p = r.exec(pingCommand);
//            if (p == null) {
//                return "unknown";
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * 获取路由器设备信息
     *
     * @param path 文件路径
     */
    private static String parseFile(String path) {
        try {
            StringBuffer sb = new StringBuffer();
            File file = new File(path);
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line = "";
                while ((line = br.readLine()) != null) {
                    //Trace.Debug("parseFile = " + line);
                    if (line.contains("<title>")) {
                        int start = line.indexOf("<title>");
                        int end = line.indexOf("</title>");
                        if (start != -1 && end != -1 && (start + 7) < end) {
                            return line.substring(start + 7, end);
                        }
                        return line;
                    }
                }
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "unknown. IOException";
        }
        return "unknown";
    }

    private static String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "."
                + (0xFF & paramInt >> 16) + "." + (0xFF & paramInt >> 24);
    }

    class CalculateSpeedThread extends Thread {
        @Override
        public void run() {
            Trace.Debug("calculateSpeedThread run");
            long sum;
            int counter;
            long cur_speed, ave_speed = 0, max_speed = 0;
            try {
                sum = 0;
                counter = 0;
                long recv = TrafficStats.getTotalRxBytes();
                long recvStart = recv;
                long start = System.currentTimeMillis();
                long duration = 1000L;
                while ((System.currentTimeMillis() - start) < 17000) {
                    Thread.sleep(duration);
                    counter++;
                    long curRecv = TrafficStats.getTotalRxBytes();
                    cur_speed = ((curRecv - recv) * 1000 / duration);
                    if (cur_speed > max_speed) {
                        max_speed = cur_speed;
                    }
                    sum += cur_speed;
                    ave_speed = (sum / counter);
                    recv = curRecv;
                    if (totalBytes > 0 && (recv - recvStart) > totalBytes) {
                        break;
                    }
                }

                flag = false;
                String maxStr, aveStr;
                long max = (max_speed / 1024);
                if (max > 1024) {
                    maxStr = mDecimalFormat.format(max / 1024) + "MB/s";
                } else {
                    maxStr = max + "KB/s";
                }

                long ave = (ave_speed / 1024);
                if (ave > 1024) {
                    aveStr = mDecimalFormat.format(ave / 1024d) + "MB/s";
                } else {
                    aveStr = ave + "KB/s";
                }

                Trace.Debug("average:" + aveStr + "; max:" + maxStr);
                HashMap umengMap = new HashMap();
                umengMap.put("max_speed", maxStr);
                umengMap.put("average_speed", aveStr);
                SlaveService.onEvent(UMengUtils.EVENT_NETWORK_SPEED, umengMap);

                File file = new File("/data/data/com.baofengtv.supporter/cache/test_speed");
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
