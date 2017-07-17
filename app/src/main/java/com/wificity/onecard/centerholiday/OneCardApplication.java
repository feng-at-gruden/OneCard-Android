package com.wificity.onecard.centerholiday;

import android.app.Application;

import com.sunmi.pay.hardware.aidl.emv.EMVOpt;
import com.sunmi.pay.hardware.aidl.pinpad.PinPadOpt;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardOpt;
import com.sunmi.pay.hardware.aidl.system.BasicOpt;

import sunmi.paylib.SunmiPayKernel;

/**
 * Created by Guo on 2017-07-16.
 */

public class OneCardApplication extends Application {

    public static SunmiPayKernel mSunmiPayKernel;

    /**
     * 获取PinPad操作模块
     */
    public static PinPadOpt mPinPadOpt;

    /**
     * 获取基础操作模块
     */
    public static BasicOpt mBasicOpt;

    /**
     * 获取读卡模块
     */
    public static ReadCardOpt mReadCardOpt;

    /**
     * 获取EMV操作模块
     */
    public static EMVOpt mEMVOpt;



}
