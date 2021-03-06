package com.robot.mvc.interfaces;


import java.util.concurrent.CountDownLatch;

/**
 * 模板接口
 * 定义算法的结构
 *
 * @author Laotang
 * @blame Android Team
 */
public interface IAction {

    /**
     * 动作名称
     * @return
     */
    String actionKey();

    /**车辆ID*/
    String vehicleId();

    /***
     * 工站设备ID
     * @return
     */
    String deviceId();

    /**
     *执行操作
     */
    void execute() throws Exception;

}
