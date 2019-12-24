package com.robot.mvc.dispatch;

import cn.hutool.core.thread.ThreadUtil;

import com.robot.agv.common.telegrams.Request;
import com.robot.agv.common.telegrams.Response;
import com.robot.agv.vehicle.telegrams.OrderResponse;
import com.robot.agv.vehicle.telegrams.Protocol;
import com.robot.mvc.dispatch.route.Route;
import com.robot.mvc.dispatch.route.RouteHelper;
import com.robot.mvc.utils.ToolsKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 调度分发工厂
 * 根据协议指令中的车辆ID及指令动作，将协议分发到对应的service里的method。
 * 所以在Service里必须要实现对应指令动作的方法。
 *
 * @author Laotang
 */
public class DispatchFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchFactory.class);

    private static final Map<String, Route> SERVICE_METHOD_MAP = new ConcurrentHashMap<>();

    /**
     * 根据IProtocol里的参数，反射调用对应Service里的方法
     * @param request
     */
    public Object execute(Request request) {
        if (SERVICE_METHOD_MAP.isEmpty()) {
            SERVICE_METHOD_MAP.putAll(RouteHelper.getRoutes());
        }

        Protocol protocol = request.getProtocol();
        if (ToolsKit.isEmpty(protocol)) {
            LOG.error("协议对象为空，返回null退出处理！");
            return null;
        }
        Response response = new OrderResponse(request);


        // 线程进行应答回复
        ThreadUtil.execute(new AnswerHandler(protocol));

        // 线程进行业务处理
        FutureTask<Response> futureTask = (FutureTask<Response>) ThreadUtil.execAsync(new BusinessHandler(request, response));
        try {
            return futureTask.get(3000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            LOG.error("执行时发生InterruptedException: {}, {}", ie.getMessage(), ie);
        } catch (ExecutionException ee) {
            LOG.error("执行时发生ExecutionException :{}, {}", ee.getMessage(), ee);
        } catch (TimeoutException te) {
            LOG.error("执行时发生TimeoutException:{}, {}", te.getMessage(), te);
        } finally {
            if (futureTask.isDone()) {
                // 中止线程，参数为true时，会中止正在运行的线程，为false时，如果线程未开始，则停止运行
                futureTask.cancel(true);
                // 回复握手消息
//                HandshakerFactory.duang().replyProtocol(protocol);
            }
        }
        return  null;
    }

}
