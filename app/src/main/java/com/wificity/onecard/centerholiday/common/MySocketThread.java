package com.wificity.onecard.centerholiday.common;

import android.os.Handler;
import android.os.Message;

import com.wificity.onecard.centerholiday.common.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Feng on 2015/11/6.
 */
public class MySocketThread extends Thread {

    public static final int ServerData = 999;
    public static final int SocketException = 888;
    public static final int GetKeyData = 777;

    private Socket mSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler;
    public boolean isConnected;

    public MySocketThread(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {

        try {
            mSocket = new Socket(Constants.ServerIP, Constants.ServerPort);
            isConnected = mSocket.isConnected();
            inputStream = mSocket.getInputStream();
            outputStream = mSocket.getOutputStream();

            //To monitor if receive Msg from Server
            new Thread()
            {
                @Override
                public void run()
                {
                    byte[] buffer = new byte[1024];
                    StringBuilder stringBuilder = new StringBuilder();
                    try
                    {
                        while(mSocket.isConnected())
                        {
                            int readSize = inputStream.read(buffer);
                            if(readSize == 0) continue;

                            //If Server is stopping
                            if(readSize == -1)
                            {
                                try
                                {
                                    inputStream.close();
                                    outputStream.close();
                                    isConnected = false;
                                    mSocket.close();
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }else
                                {
                                //Update the receive editText
                                stringBuilder.append(new String(buffer, 0, readSize, "gbk"));
                                Message msg = new Message();
                                msg.what = ServerData;
                                msg.obj = stringBuilder.toString();
                                handler.sendMessage(msg);
                                stringBuilder = new StringBuilder();
                            }
                        }
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                        //sendError(e.getMessage());
                    }
                }

            }.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sendError(e.getMessage());
        }
    }

    public void sendMessage(String data)
    {
        if(mSocket!=null && mSocket.isConnected() )
        {
            try{
                outputStream.write(data.getBytes());
                outputStream.flush();
            }catch(Exception e)
            {
                e.printStackTrace();
                sendError(e.getMessage());
            }
        }
    }

    public void clear()
    {
        try {
            isConnected = false;
            if (outputStream != null)
                outputStream.close();
            if (mSocket != null)
                mSocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void sendError(String error)
    {
        Message msg = new Message();
        msg.what = SocketException;
        msg.obj = error;
        handler.sendMessage(msg);
    }
}
