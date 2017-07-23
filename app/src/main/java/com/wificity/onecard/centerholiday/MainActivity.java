package com.wificity.onecard.centerholiday;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidl.bean.CardInfo;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardCallback;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardOpt;
import com.sunmi.pay.hardware.aidl.system.BasicOpt;
import com.wificity.onecard.centerholiday.common.Constants;
import com.wificity.onecard.centerholiday.utils.KeyUtils;
import com.wificity.onecard.centerholiday.common.MySocketThread;
import com.wificity.onecard.centerholiday.utils.NetworkUtils;
import com.wificity.onecard.centerholiday.utils.StringByteUtils;

import java.util.ArrayList;

import sunmi.paylib.SunmiPayKernel;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String NO_KEY = "------------";
    private SunmiPayKernel mSunmiPayKernel;
    private ReadCardOpt mReadCardOpt;
    private SoundPool soundPool;
    private final boolean manualSwingCard = false;          //自动刷卡 手动刷卡切换

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI(false);
        soundPool= new SoundPool(10, AudioManager.STREAM_SYSTEM,5);
        soundPool.load(this,R.raw.welcome,1);

        connSunMinService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mReadCardOpt != null && !manualSwingCard)
            swingCard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkUtils.closeSocket();
        if (OneCardApplication.mBasicOpt != null) {
            try {
                mReadCardOpt.cancelCheckCard();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.general_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuMainAbout:
                onShowAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void initUI(boolean enabled)
    {
        if(enabled)
        {
            findViewById(R.id.btnSwingCard).setVisibility(View.VISIBLE);
            findViewById(R.id.btnEnterRoomNumber).setVisibility(View.VISIBLE);
            if(mReadCardOpt!=null && !manualSwingCard)    // waiting for swing card automatically
                swingCard();
        }else{
            findViewById(R.id.btnSwingCard).setVisibility(View.INVISIBLE);
            findViewById(R.id.btnEnterRoomNumber).setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.btnSwingCard).setOnClickListener(this);
        findViewById(R.id.btnEnterRoomNumber).setOnClickListener(this);
    }



    //读卡相关
    final int timeOut = 120;
    final int block = 8;

    /**
     * 刷卡
     */
    public void swingCard() {
        try {
            //mReadCardOpt.cancelCheckCard();
            //支持磁卡，IC，NFC
            //int allType = AidlConstants.CardType.MAG | AidlConstants.CardType.IC | AidlConstants.CardType.NFC;
            int allType = AidlConstants.CardType.NFC;
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
                //Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
                beep(2);
                //Try next
                if(mReadCardOpt!=null && !manualSwingCard)
                {
                    mReadCardOpt.cancelCheckCard();
                    try {
                        Thread.sleep(1000);
                    }catch(Exception e){}
                    swingCard();
                }
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
                //Toast.makeText(this, dataStr, Toast.LENGTH_SHORT).show();
                NetworkUtils.sendShortMessage(Constants.TAG_BYCARD + dataStr + Constants.TAG_BYCARD, getMessageHandler);
                beep(1);

                if(mReadCardOpt!=null && !manualSwingCard)    //waiting for next time swing card
                    swingCard();
            }else{
                //Toast.makeText(this, "读卡错误", Toast.LENGTH_SHORT).show();
                led(4);
                beep(2);

                if(mReadCardOpt!=null && !manualSwingCard)
                {
                    mReadCardOpt.cancelCheckCard();
                    try {
                        Thread.sleep(1000);
                    }catch(Exception e){}
                    swingCard();
                }
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
        }

    };


    private void beep(int k) {
        BasicOpt mBasicOpt = OneCardApplication.mBasicOpt;
        try {
            mBasicOpt.buzzerOnDevice(k);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSound()
    {
        soundPool.play(1,1, 1, 0, 0, 1);
    }

    /**
     * 控制LED灯
     * ledIndex: 设备上的LED索引，1~4；4-红，3-绿，2-黄，1-蓝
     * ledStatus：LED状态，0表示LED灭，1表示LED亮；
     */
    private void led(int ledIndex)
    {
        BasicOpt mBasicOpt = OneCardApplication.mBasicOpt;
        try{
            mBasicOpt.ledStatusOnDevice(ledIndex, 1);
            Thread.sleep(250);
            mBasicOpt.ledStatusOnDevice(ledIndex, 0);
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






    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id)
        {
            case R.id.btnSwingCard:
                swingCard();
                break;
            case R.id.btnEnterRoomNumber:
                showEnterRoomNumberDialog();
                break;
        }
    }


    private final Handler getMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MySocketThread.ServerData:
                    String content = msg.obj.toString();
                    if( content.indexOf(Constants.REC_MSG) >=0 ) {
                        showMessageDialog(content.replace(Constants.REC_MSG, ""), null);
                    }else if( content.indexOf(Constants.REC_BOOK_SUCCESS) >=0 ){
                        showMessageDialog(getString(R.string.message_book_success), null);
                        //Book success
                        led(3);
                        playSound();
                    }else if( content.indexOf(Constants.REC_INVALID_CARD_OR_ROOM) >=0 ){
                        showMessageDialog(getString(R.string.message_invalid_input, content), null);
                        led(4);
                    }else if( content.indexOf(Constants.REC_OUT_OF_SERVICE) >=0 ){
                        showMessageDialog(getString(R.string.message_out_of_service, content), null);
                        led(4);
                    }else if( content.indexOf(Constants.REC_BOOK_ERROR) >=0 ){
                        showMessageDialog(getString(R.string.message_book_error, content), null);
                        led(4);
                    }else{
                        closeRoomInfoDialog();
                        showRoomInfoDialog(content);
                    }
                    break;
                case MySocketThread.SocketException:
                    Toast.makeText(MainActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    break;
                case MySocketThread.GetKeyData:
                    //tagDataView.setText(msg.obj.toString());
                    break;
            }
        }
    };



    //客房信息对话框

    AlertDialog roomInfoDialog;
    private void showRoomInfoDialog(String content)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = View.inflate(this, R.layout.dialog_guest_info, null);
        builder.setView(v);
        builder.setCancelable(true);
        TextView guestInfoView= (TextView) v.findViewById(R.id.guestInfo);
        guestInfoView.setText(content);

        v.findViewById(R.id.btnRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetworkUtils.sendShortMessage(Constants.CMD_REGISTRATION_BYCARD, getMessageHandler);
                closeRoomInfoDialog();
            }
        });
        roomInfoDialog = builder.create();
        WindowManager.LayoutParams layoutParams = roomInfoDialog.getWindow().getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        Window window = roomInfoDialog.getWindow();
        window.setAttributes(layoutParams);
        window.setWindowAnimations(R.style.MyAlertDialogStyle);

        roomInfoDialog.show();
    }

    private void closeRoomInfoDialog()
    {
        if(roomInfoDialog != null && roomInfoDialog.isShowing())
            roomInfoDialog.dismiss();

        if(roomNumberDialog != null && roomNumberDialog.isShowing())
            roomNumberDialog.dismiss();
    }

    private void showMessageDialog(String content, DialogInterface.OnClickListener listener)
    {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.text_information)
                .setMessage(content)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.action_ok, listener).create();
        Window window = dialog.getWindow();
        window.setWindowAnimations(R.style.MyAlertDialogStyle);
        dialog.show();
    }



    //房间号码对话框

    AlertDialog roomNumberDialog;
    EditText roomNumberEditText;
    private void showEnterRoomNumberDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = View.inflate(this, R.layout.dialog_enter_room_number, null);
        builder.setView(v);
        builder.setCancelable(true);

        roomNumberEditText = (EditText) v.findViewById(R.id.editRoomNumber);
        roomNumberEditText.setCursorVisible(false);
        v.findViewById(R.id.button_0).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_1).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_2).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_3).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_4).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_5).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_6).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_7).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_8).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_9).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_del).setOnClickListener(numberButtonOnClickListener);
        v.findViewById(R.id.button_confirm).setOnClickListener(numberButtonOnClickListener);

        roomNumberDialog = builder.create();
        roomNumberDialog.show();
    }

    private View.OnClickListener numberButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId())
            {
                case R.id.button_0:
                    performKeyDown(KeyEvent.KEYCODE_0);
                    break;
                case R.id.button_1:
                    performKeyDown(KeyEvent.KEYCODE_1);
                    break;
                case R.id.button_2:
                    performKeyDown(KeyEvent.KEYCODE_2);
                    break;
                case R.id.button_3:
                    performKeyDown(KeyEvent.KEYCODE_3);
                    break;
                case R.id.button_4:
                    performKeyDown(KeyEvent.KEYCODE_4);
                    break;
                case R.id.button_5:
                    performKeyDown(KeyEvent.KEYCODE_5);
                    break;
                case R.id.button_6:
                    performKeyDown(KeyEvent.KEYCODE_6);
                    break;
                case R.id.button_7:
                    performKeyDown(KeyEvent.KEYCODE_7);
                    break;
                case R.id.button_8:
                    performKeyDown(KeyEvent.KEYCODE_8);
                    break;
                case R.id.button_9:
                    performKeyDown(KeyEvent.KEYCODE_9);
                    break;
                case R.id.button_del:
                    performKeyDown(KeyEvent.KEYCODE_DEL);
                    break;
                case R.id.button_confirm:
                    String roomNumber = roomNumberEditText.getText().toString();
                    if(roomNumber!=null && roomNumber.length()>0) {
                        NetworkUtils.sendShortMessage(Constants.TAG_BYROOM + roomNumberEditText.getText() + Constants.TAG_BYROOM, getMessageHandler);
                        roomNumberDialog.dismiss();
                    }else{

                    }
                    break;
            }
        }
    };

    private void performKeyDown(final int keyCode)
    {
        if(roomNumberEditText==null)
            return;
        String v = roomNumberEditText.getText().toString();
        if(v.length()>=6 && keyCode != KeyEvent.KEYCODE_DEL)
            return;
        //beep(1);
        if(keyCode == KeyEvent.KEYCODE_DEL)
            roomNumberEditText.setText(v.length() >0 ? v.substring(0, v.length() - 1) : "");
        else
            roomNumberEditText.setText( v + (char)(keyCode + 41));
    }


    //
    private void onShowAboutDialog() {
        String v = "0.2";
        try{
            v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {}
        CharSequence styledText = Html.fromHtml(getString(R.string.dialog_about_mct, v));
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(styledText)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.action_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing.
                            }
                        }).create();
        ad.show();
        // Make links clickable.
        ((TextView) ad.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
