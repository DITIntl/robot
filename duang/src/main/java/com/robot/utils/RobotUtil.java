package com.robot.utils;

import com.robot.agv.common.telegrams.Response;
import com.robot.agv.vehicle.telegrams.Protocol;
import com.robot.agv.vehicle.telegrams.ProtocolParam;
import com.robot.core.AppContext;
import com.robot.mvc.exceptions.RobotException;
import com.robot.mvc.helper.ActionHelper;
import com.robot.mvc.interfaces.IAction;
import com.robot.numes.RobotEnum;
import com.robot.service.common.ActionResponse;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.MovementCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class RobotUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RobotUtil.class);


    /***
     * 根据线名称取openTCS线路图上的车辆
     */
    public static Vehicle getVehicle(String vehicleName){
        java.util.Objects.requireNonNull(vehicleName, "车辆名称不能为空");
        return AppContext.getOpenTcsObjectService(vehicleName).fetchObject(Vehicle.class, vehicleName);
    }

    /***
     * 根据点名称取openTCS线路图上的点
     */
    public static Point getPoint(String vehicleName,String pointName){
        java.util.Objects.requireNonNull(pointName, "点名称不能为空");
        return AppContext.getOpenTcsObjectService(vehicleName).fetchObject(Point.class, pointName);
    }

    /***
     * 根据协议对象取上报卡号或预停车协议内容的点
     */
    public static String getReportPoint(Protocol protocol){
        java.util.Objects.requireNonNull(protocol, "协议对象不能为空");
        if (ProtocolUtils.isRptacProtocol(protocol.getCommandKey()) ||
                ProtocolUtils.isRptrtpProtocol(protocol.getCommandKey())) {
            return protocol.getParams().split(RobotEnum.PARAMLINK.getValue())[0];
        } else{
            throw new RobotException("取上报卡号时，协议["+ProtocolUtils.converterString(protocol)+"]指令不符， 不是上报卡号[rptac/rptrtp]指令");
        }
    }

    /***
     * 根据线名称取openTCS线路图上的车辆
     */
    public static Location getLocation(String vehicleName, String locationName){
        java.util.Objects.requireNonNull(locationName, "位置名称不能为空");
        return AppContext.getOpenTcsObjectService(vehicleName).fetchObject(Location.class, locationName);
    }

    /**
     * 取点属性值
     * @param pointName 点名称
     * @param key 属性key
     * @param defaultValue 默认值
     * @return
     */
    public static String getPointPropertiesValue(String vehicleName, String pointName, String key, String defaultValue) {
        Point point =  getPoint(vehicleName, pointName);
        java.util.Objects.requireNonNull(point, "根据["+pointName+"]找不到对应的点对象");
        Map<String,String> pointMap = point.getProperties();

        if(null == pointMap || ToolsKit.isEmpty(key)) {
            throw new RobotException("点对象属性集合或者属性关键字不能为空");
        }
        String value = pointMap.get(key);
        if(ToolsKit.isEmpty(value)) {
            LOG.info("根据["+key+"]取点对象属性值时，值不存在，返回默认值：" + defaultValue);
            return defaultValue;
        }
        RobotEnum directionEnum = getDirectionEnum(value);
        if(ToolsKit.isEmpty(directionEnum)) {
            throw new RobotException("在DirectionEnum枚举里不存在与["+value+"]对应的枚举值，请检查");
        }
        return directionEnum.getValue();
    }

    public static RobotEnum getDirectionEnum(String directionStr) {
        for (RobotEnum directionEnum : RobotEnum.values()) {
            if (directionEnum.getValue().equals(directionStr)) {
                return directionEnum;
            }
        }
        LOG.info("取["+directionStr+"]枚举对象为空");
        return null;
    }

    public static String  buildProtocolParamString (List<ProtocolParam> protocolParamList) {
        StringBuilder paramsString = new StringBuilder();
        int length = protocolParamList.size();
        if(ToolsKit.isEmpty(protocolParamList) || length == 0) {
            throw new RobotException("协议参数集合不能为空");
        }
        ProtocolParam startProtocolParam = protocolParamList.get(0);
        paramsString.append(startProtocolParam.getBefore());
        for(int i=1; i<length; i++) {
            ProtocolParam nextProtocolParam = protocolParamList.get(i);
            if(ToolsKit.isNotEmpty(nextProtocolParam)) {
                paramsString.append(RobotEnum.PARAMLINK.getValue()).append(nextProtocolParam.getBefore());
            }
        }
        // 最后一个点的处理
        ProtocolParam endProtocolParam = protocolParamList.get(length-1);
        paramsString.append(RobotEnum.PARAMLINK.getValue()).append(endProtocolParam.getAfter());
        LOG.info("创建协议字符串：{}", paramsString);
        return paramsString.toString();
    }

    /**
     * 最后执行的动作名称是否包含自定义模板集合中
     * @param currentCmd
     * @return
     */
    public static  boolean isContainActionsKey(MovementCommand currentCmd) {
        String operation = currentCmd.getOperation();
        Map<String, IAction> actionMap = ActionHelper.duang().getCustomActionsQueue();
        IAction actionTemplate = actionMap.get(operation);
        if(ToolsKit.isEmpty(actionTemplate)) {
            actionTemplate = actionMap.get(operation.toUpperCase());
            if(ToolsKit.isEmpty(actionTemplate)) {
                actionTemplate = actionMap.get(operation.toLowerCase());
            }
        }
        if(ToolsKit.isEmpty(actionTemplate)) {
            LOG.error("请先配置需要执行的自定义指令组合，名称需要一致，不区分大小写");
            return false;
        }
        return true;
    }

    /**
     * 模拟设备返回信息与验证码
     * @param response
     * @return
     */
    public static ActionResponse simulation(Response response){
        java.util.Objects.requireNonNull(response, "response is null");
        Protocol protocol = response.getProtocol();
        if (ToolsKit.isEmpty(protocol)) {
            LOG.info("模拟设备返回信息时，响应对象里的协议对象为空，返回响应对象");
            return (ActionResponse)response;
        }
        String cmdKey = protocol.getCommandKey();
        // 如果不是rpt开头的指令，则更改方向
        if (!cmdKey.startsWith("rpt")) {
            protocol.setDirection(RobotEnum.DOWN_LINK.getValue());
            // 计算出验证码
            String code = CrcUtil.CrcVerify_Str(ProtocolUtils.builderCrcString(protocol));
            protocol.setCode(code);
        }
        return new ActionResponse(protocol) {
            @Override
            public String cmd() {
                return protocol.getCommandKey();
            }
        };
    }

}
