package com.wificity.onecard.centerholiday.utils;

import android.os.Handler;

import com.wificity.onecard.centerholiday.common.Constants;
import com.wificity.onecard.centerholiday.common.MySocketThread;

/**
 * Created by Guo on 2017-07-18.
 */

public class NetworkUtils {


    private static MySocketThread mySocketThread;
    private static Handler getMessageHandler;

    public static void setGetMessageHandler(Handler getMessageHandler) {
        NetworkUtils.getMessageHandler = getMessageHandler;
    }

    public static void initSocket() {
        if (mySocketThread != null && mySocketThread.isAlive())
            mySocketThread.destroy();

        if (getMessageHandler != null) {
            mySocketThread = new MySocketThread(getMessageHandler);
            mySocketThread.start();
        }
    }

    public static void sendShortMessage(String msg, Handler handler) {
        if( mySocketThread == null || !mySocketThread.isConnected) {
            mySocketThread = new MySocketThread(handler);
            mySocketThread.start();

            try{
                Thread.sleep(Constants.WaitTime);
            }catch(InterruptedException e) {}
        }

        mySocketThread.sendMessage(msg);
    }

    public static  void closeSocket()
    {
        if (mySocketThread != null)
            mySocketThread.clear();
    }
}
