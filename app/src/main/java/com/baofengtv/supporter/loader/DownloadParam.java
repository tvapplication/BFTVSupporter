package com.baofengtv.supporter.loader;

/**
 * 
 * @author LiLiang
 * 
 * @date 2015-7-20
 * 
 * @brief	下载参数
 * 
 * @version v1.0
 *
 */
public class DownloadParam {
	//server端下载地址 http url
	public String src;
	
	//文件下载到本地的保存路径
	public String dst;

	//断点续传标识
	public boolean resumeFlag = false;

	//服务端md5
	public String srcMD5 = "";

	//本地文件md5
	public String dstMD5 = "";

	//下载结果状态码
	public int code = -1;

	public DownloadParam(){
		
	}
	
	public DownloadParam(String url, String dstPath){
		this.src = url;
		this.dst = dstPath;
	}

}
