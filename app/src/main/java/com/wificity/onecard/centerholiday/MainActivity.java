package com.wificity.onecard.centerholiday;

import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidl.bean.CardInfo;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardCallback;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardOpt;
import com.sunmi.pay.hardware.aidl.system.BasicOpt;
import com.wificity.onecard.centerholiday.utils.KeyUtils;
import com.wificity.onecard.centerholiday.utils.StringByteUtils;

import java.util.ArrayList;

import sunmi.paylib.SunmiPayKernel;

public class MainActivity extends AppCompatActivity {

    public static final String NO_KEY = "------------";
    private SunmiPayKernel mSunmiPayKernel;
    private ReadCardOpt mReadCardOpt;

    final int timeOut = 120;
    final int block = 8;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI(false);
        connSunMinService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OneCardApplication.mBasicOpt != null) {
            try {
                //关闭NFC天线
                OneCardApplication.mBasicOpt.RFOff();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSunmiPayKernel != null) {
            mSunmiPayKernel.unbindPayService(getBaseContext());
        }
    }

    private void initUI(boolean enabled)
    {
        findViewById(R.id.btnSwingCard).setEnabled(enabled);
        findViewById(R.id.btnSwingCard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swingCard();
            }
        });
        if(enabled)
            swingCard();
    }

    /**
     * 刷卡
     */
    public void swingCard() {
        try {
            //支持磁卡，IC，NFC
            int allType = AidlConstants.CardType.MAG | AidlConstants.CardType.IC | AidlConstants.CardType.NFC;
            mReadCardOpt.readCard(allType, mReadCardCallback, timeOut);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 授权
     */
    private void m1Auth(String uuid) {
        try {
            byte[] uid = StringByteUtils.HexString2Bytes(uuid);
            byte[] key = KeyUtils.getkey(uid);

            int result = mReadCardOpt.m1Auth(0, block, key);
            if (result == 0) {
                m1ReadBlock(key);
            } else {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 读取块数据
     */
    private void m1ReadBlock(byte[] key) {
        try {
            ArrayList<String> blocks = new ArrayList<>();
            for(int i=0;i<4;i++)
            {
                byte[] blockData = new byte[16];
                int blockResult = mReadCardOpt.m1ReadBlock(block+i, blockData);
                if (blockResult == 0) {
                    String data = StringByteUtils.bytesToHexString(blockData);
                    blocks.add(data);
                } else {
                    //TODO
                }
            }
            //
            if(blocks.size()==4)
            {
                String[] ret = blocks.toArray(new String[blocks.size()]);
                int last = blocks.size() -1;
                if (KeyUtils.isKeyBReadable(KeyUtils.hexStringToByteArray(ret[last].substring(12, 20)))) {
                    ret[last] = KeyUtils.byte2HexString(key) + ret[last].substring(12, 32);
                } else {
                    ret[last] = KeyUtils.byte2HexString(key) + ret[last].substring(12, 20) + NO_KEY;
                }

                //数据读取成功
                String dataStr = "";
                for(int i=0; i<ret.length; i++)
                {
                    dataStr += ret[i];
                }
                Toast.makeText(this, dataStr, Toast.LENGTH_SHORT).show();
                beep();
                //TODO, send data

                //Waiting for next time
                swingCard();
            }else{
                Toast.makeText(this, "读卡错误", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    ReadCardCallback mReadCardCallback = new ReadCardCallback.Stub() {
        @Override
        public void onStartReadCard() throws RemoteException {

        }

        @Override
        public void onFindMAGCard(CardInfo cardInfo) throws RemoteException {
        }

        @Override
        public void onFindNFCCard(final CardInfo cardInfo) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //检卡成功; 进行认证
                    m1Auth(cardInfo.uuid);
                }
            });
        }

        @Override
        public void onFindICCard(CardInfo cardInfo) throws RemoteException {
        }

        @Override
        public void onError(int code) throws RemoteException {
        }

        @Override
        public void onTimeOut() throws RemoteException {
            Toast.makeText(getBaseContext(), "刷卡超时", Toast.LENGTH_SHORT).show();
        }

    };


    private void beep() {
        BasicOpt mBasicOpt = OneCardApplication.mBasicOpt;
        try {
            mBasicOpt.buzzerOnDevice(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接支付SDK
     */
    private void connSunMinService() {
        mSunmiPayKernel = SunmiPayKernel.getInstance();
        mSunmiPayKernel.connectPayService(getApplicationContext(), mConnCallback);
    }

    /**
     * 连接状态回调
     */
    private SunmiPayKernel.ConnCallback mConnCallback = new SunmiPayKernel.ConnCallback() {
        @Override
        public void onServiceConnected() {
            try {
                OneCardApplication.mPinPadOpt = mSunmiPayKernel.mPinPadOpt;
                OneCardApplication.mBasicOpt = mSunmiPayKernel.mBasicOpt;
                OneCardApplication.mReadCardOpt = mSunmiPayKernel.mReadCardOpt;
                OneCardApplication.mEMVOpt = mSunmiPayKernel.mEMVOpt;

                mReadCardOpt = OneCardApplication.mReadCardOpt;

                //存储KEK,TMK,PIK,MAK,TDK,各种默认的密钥, This is for PinPad
                //OneCardApplication.initSecretKey();
                //加载默认aid和capk
                //OneCardApplication.loadaidcapk(); This is for EMVOpt
                OneCardApplication.mReadCardOpt.cancelCheckCard();

                initUI(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected() {
        }
    };



}
