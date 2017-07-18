package com.wificity.onecard.centerholiday.common;

/**
 * Created by Guo on 2017-07-18.
 */

public class Constants {

    public static final String ServerIP = "192.168.32.167";
    public static final int ServerPort = 10001;
    public static final int WaitTime = 200;        //2 seconds

    public static String TAG_BYCARD = "W";
    public static String TAG_BYROOM = "R";
    public static String CMD_REGISTRATION_BYCARD= "Command 1";
    public static String CMD_REGISTRATION_BYROOM = "Command 2";

    //在VB端定义的接收指令, 预订成功或失败错误信息最好定义在VB端, 安卓端只负责显示
    public static String REC_MSG  = "MSG:";                                         // SERVER MESSAGE FLAG
    public static String REC_CONTENT_TOTAL_COUNT = "Total Count";                   // 大于0 可预订
    public static String REC_INVALID_CARD_OR_ROOM = "Invalid";                      // 无效卡/房间号
    public static String REC_BOOK_SUCCESS = "Data Send OK";                         // 预订成功
    public static String REC_OUT_OF_SERVICE = "Out Of Service";                     // 超过用餐时段
    public static String REC_BOOK_ERROR = "Data Send Error";                        // 预订失败

}
