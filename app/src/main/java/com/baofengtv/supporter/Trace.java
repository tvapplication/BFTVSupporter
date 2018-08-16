package com.baofengtv.supporter;

import android.util.Log;

public class Trace {
	public static final String TAG = "Supporter";
	
	private static boolean flag = true;	//false
	
	/**
	 * 调试打印
	 * @param msg
	 */
	public static void Debug(String msg){
		if(flag){
			StackTraceElement[] stacks = (new Throwable()).getStackTrace();  
			
            String wrapperMsg = "[" + stacks[1].getFileName() + 
            		":" +
            		stacks[1].getLineNumber() + "]: " + 
            		msg;
            
			Log.d(TAG, wrapperMsg);
		}
	}
	
	/**
	 * 信息打印
	 * @param msg
	 */
	public static void Info(String msg){
		if(flag){
			StackTraceElement[] stacks = (new Throwable()).getStackTrace();  
			
            String wrapperMsg = "[" + stacks[1].getFileName() + 
            		":" +
            		stacks[1].getLineNumber() + "]: " + 
            		msg;
            
			Log.i(TAG, wrapperMsg);
		}
	}
	
	/**
	 * 警告打印
	 * @param msg
	 */
	public static void Warn(String msg){
		StackTraceElement[] stacks = (new Throwable()).getStackTrace();  
		
        String wrapperMsg = "[" + stacks[1].getFileName() + 
        		":" +
        		stacks[1].getLineNumber() + "]: " + 
        		msg;
        
		Log.w(TAG, wrapperMsg);
	}
	
	/**
	 * 错误打印
	 * @param msg
	 */
	public static void Error(String msg){
		StackTraceElement[] stacks = (new Throwable()).getStackTrace();  
		
        String wrapperMsg = "[" + stacks[1].getFileName() + 
        		":" +
        		stacks[1].getLineNumber() + "]: " + 
        		msg;
        
		Log.e(TAG, wrapperMsg);
	}
}
