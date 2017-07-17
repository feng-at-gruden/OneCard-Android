package com.wificity.onecard.centerholiday.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tomcat on 2017/1/15.
 */
public class StringByteUtils {

    // 从十六进制字符串到字节数组转换
    public static byte[] HexString2Bytes(String hexstr) {
        int l = hexstr.length();
        if(l%2 != 0){
            StringBuilder sb = new StringBuilder(hexstr);
            sb.insert(hexstr.length()-1, '0');
            hexstr = sb.toString();
        }
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
        }
        return b;
    }

    private static int parse(char c) {
        if (c >= 'a')
            return (c - 'a' + 10) & 0x0f;
        if (c >= 'A')
            return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }

    /**
     * Convert byte[] to hex string.将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
     * @param src byte[] data
     * @return hex string
     */
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * "0102" 16进制字符串转 转0x01, 0x02
     * @param hexStr
     * @return
     */
    public static String hexStringFotmat(String hexStr){
        StringBuilder stringBuilder = new StringBuilder("");
        int start = 0;
        int end = 2;
        for(int i=0; i<hexStr.length()/2; i++){
            String temp = "0x"+hexStr.substring(start, end)+",";
            stringBuilder.append(temp);
            start = start + 2;
            end = end + 2;
        }
        hexStr = stringBuilder.toString();
        hexStr = hexStr.substring(0, hexStr.length()-1);
        return hexStr;
    }


    /**
     * 16进制字符串转buye[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    public static byte[] Stringtobyte(String a){
        return a.getBytes();
    }

    /**
     * 判断string是否为数字
     * @param str
     * @return
     */
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

}
