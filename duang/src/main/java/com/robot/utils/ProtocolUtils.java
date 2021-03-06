package com.robot.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.robot.agv.vehicle.telegrams.Protocol;
import com.robot.mvc.exceptions.RobotException;
import com.robot.numes.RobotEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * 协议对象工具
 * Created by laotang on 2019/12/22.
 */
public class ProtocolUtils {

    private static final Logger LOG =  LoggerFactory.getLogger(ProtocolUtils.class);
    private static Set<String> DEVICE_FLAG_SET;

    /**协议报文前缀*/
    private static final String START_PREFIX = "##";
    /**分隔符*/
    private static final String SEPARATOR = ",,";
    /**协议报文后缀*/
    private static final String END_SUFFIX = "ZZ";
    /**协议下行方向标识符*/
    public static final String DIRECTION_RESPONSE = "r";
    /**协议上行方向标识符*/
    public static final String DIRECTION_REQUEST = "s";


    /**
     * 根据报文内容构建协议对象
     *
     * @param telegramData 报文内容
     * @return Protocol
     */
    public static Protocol buildProtocol(String telegramData) {
        if(!checkTelegramFormat(telegramData)) {
            throw new IllegalArgumentException("报文["+telegramData+"]格式不正确");
        }

        String[] telegramArray = StrUtil.split(telegramData, SEPARATOR);
        if(ArrayUtil.isEmpty(telegramArray)) {
            throw new NullPointerException("构建协议对象时，报文内容主体不能为空");
        }

        for(String itemValue  : telegramArray){
            if(StrUtil.isEmpty(itemValue)) {
                throw new NullPointerException("协议报文的每个单元内容值不能为空");
            }
        }

       return new Protocol.Builder()
        .deviceId(telegramArray[1])
        .direction(telegramArray[2])
        .commandKey(telegramArray[3])
        .params(telegramArray[4])
        .crc(telegramArray[5])
        .build();

    }

    public static String converterString(Protocol protocol) {
        StringBuilder protocolStr = new StringBuilder();
        String rawStr = builderCrcString(protocol);
        protocolStr
                .append(rawStr)
                .append(buildCrc(rawStr))
                .append(SEPARATOR)
                .append(END_SUFFIX);
        return protocolStr.toString();
    }

    public static String builderCrcString(Protocol protocol) {

        if (!checkProtocolValue(protocol)) {
            return "";
        }

        StringBuilder protocolString = new StringBuilder();
        protocolString.append(START_PREFIX)
                .append(SEPARATOR)
                .append(protocol.getDeviceId())
                .append(SEPARATOR)
                .append(protocol.getDirection())
                .append(SEPARATOR)
                .append(protocol.getCommandKey())
                .append(SEPARATOR)
                .append(protocol.getParams())
                .append(SEPARATOR);

        return protocolString.toString();
    }

    /**
     * 构建握手报文的code
     * 将方向反转再做成code返回
     * @param protocol
     * @return
     */
    public static String builderHandshakeCode(Protocol protocol) {
        if (!checkProtocolValue(protocol)) {
            return "";
        }
        String direction = "";
        if (RobotEnum.DOWN_LINK.getValue().equals(protocol.getDirection())) {
            direction = RobotEnum.UP_LINK.getValue();
        } else if (RobotEnum.UP_LINK.getValue().equals(protocol.getDirection())){
            direction = RobotEnum.DOWN_LINK.getValue();
        }
        protocol.setDirection(direction);
        String code =  CrcUtil.CrcVerify_Str(builderCrcString(protocol));
        protocol.setCode(code);
        return code;
    }

    /**
     * 检查报文对象值，如果为空值的话，则返回false
     * @param protocol
     * @return
     */
    private static boolean checkProtocolValue(Protocol protocol) {
        java.util.Objects.requireNonNull(protocol, "协议对象不能为空");

        Map<String,Object> protocolMap = BeanUtil.beanToMap(protocol);
        for (Iterator<Map.Entry<String,Object>> iterator = protocolMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String,Object> entry = iterator.next();
            if (null != entry.getValue()) {
                String value = entry.getValue().toString();
                if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value)) {
                    LOG.error("构建协议对象时，报文内容主体不能为空");
                    return false;
                }
            }
        }

        return true;
    }

    private static String buildCrc(String crc) {
        if (StrUtil.isNotBlank(crc)) {
            return CrcUtil.CrcVerify_Str(crc);
        }
        return "0000";
    }

    /**
     * 是否报文格式
     * @param telegramData
     * @return 正确返回true
     */
    public static boolean checkTelegramFormat(String telegramData) {
        return telegramData.contains(START_PREFIX) &&
                        telegramData.contains(SEPARATOR) &&
                        StrUtil.endWith(telegramData, END_SUFFIX);
    }

    /**
     * 是否是Order协议，所有不是移动指令的协议，都是订单协议
     * @param commandKey
     * @return
     */
    public static boolean isOrderProtocol(String commandKey) {
        return !isStateProtocol(commandKey);
    }

    /**
     * 是否是State协议，即车辆移动指令协议为状态协议
     * @param commandKey 指令
     * @return 如果是返回true
     */
    public static boolean isStateProtocol(String commandKey) {
        return "setrout".equalsIgnoreCase(commandKey) ||
                        "rptac".equalsIgnoreCase(commandKey) ||
                        "rptrtp".equalsIgnoreCase(commandKey);
    }

    public static boolean isRptacProtocol(String commandKey) {
        return "rptac".equalsIgnoreCase(commandKey);
    }

    public static boolean isRptrtpProtocol(String commandKey) {
        return "rptrtp".equalsIgnoreCase(commandKey);
    }


    /**
     * 检查是否为状态类型的请求，
     * 方向为s时为请求
     *
     * @param protocol 待检查的协议对象
     * @return 是则返回true，否则返回false
     */
    public static boolean isOrderRequest(Protocol protocol) {
        requireNonNull(protocol, "报文协议对象不能为空");
        return ProtocolUtils.isOrderProtocol(protocol.getCommandKey()) &&
                ProtocolUtils.DIRECTION_REQUEST.equalsIgnoreCase(protocol.getDirection());
    }

    /**
     * 检查是否为状态类型的响应
     *方向为r时为响应
     *
     * @param protocol 待检查的报文协议对象
     * @return 是则返回true，否则返回false
     */
    public static boolean isOrderResponse(Protocol protocol) {
        requireNonNull(protocol, "报文协议对象不能为空");

        return ProtocolUtils.isOrderProtocol(protocol.getCommandKey()) &&
                ProtocolUtils.DIRECTION_RESPONSE.equalsIgnoreCase(protocol.getDirection());
    }

    public static boolean isAllowAccess(String deviceId) {
        return getDeviceFlagSet().contains(deviceId);
    }

    private static Set<String> getDeviceFlagSet() {
        if(null == DEVICE_FLAG_SET) {
            DEVICE_FLAG_SET = SettingUtils.getStringsToSet("device.name", "security");
            if (ToolsKit.isEmpty(DEVICE_FLAG_SET)) {
                throw new RobotException("请先在app.setting里设置device.name值！该值用于允许指定的车辆或设备ID访问系统");
            }
        }
        return DEVICE_FLAG_SET;
    }


    public static List<String> getTelegram2List(String telegram) {
//        String telegram = "##,,A002,,s,,setrout,,mf400::mf708,,721a,,ZZ##,,A002,,s,,setrout,,mf400::mf708,,721b,,ZZ##,,A002,,s,,setrout,,mf400::mf708,,721c,,ZZ";

//        A030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZA030##,,A030,,s,,rpterr,,0040,,d229,,ZZA030##,,A030,,s,,rptmag,,0::0::1::1,,ff8a,,ZZ

        List<String> returnTelegramList = null;
        String splitString = RobotEnum.SEPARATOR.getValue() + RobotEnum.FRAME_END.getValue();
        List<String> telegramList = StrUtil.splitTrim(telegram, splitString);
        if (ToolsKit.isNotEmpty(telegramList)) {
            returnTelegramList = new ArrayList<>(telegramList.size());
            for (String telegramItem : telegramList) {
                String telegramData = telegramItem + splitString;
                if(!checkTelegramFormat(telegramData)) {
                    LOG.info("报文["+telegramData+"]格式不正确");
                    continue;
                }
                returnTelegramList.add(telegramData);
            }
        }
        return returnTelegramList;
    }
}
