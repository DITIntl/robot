package com.robot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.robot.mvc.exceptions.RobotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ToolsKit {

    private static final Logger logger = LoggerFactory.getLogger(ToolsKit.class);
    private static final Set<String> EXCLUDED_METHOD_NAME = new HashSet<>();

    public static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        /**过滤对象的null属性*/
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        /**过滤map中的null key*/
        objectMapper.getSerializerProvider().setNullKeySerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator generator, SerializerProvider serializers) throws IOException, JsonProcessingException {
                generator.writeFieldName("");
            }
        });
        /**过滤map中的null值*/
        objectMapper.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator generator, SerializerProvider serializers) throws IOException, JsonProcessingException {
                generator.writeString("");
            }
        });
    }

    /**
     * json字符串转换为对象
     * @param jsonStr   json格式的字符串
     * @param clazz 待转换的对象
     * @param <T>  返回泛型值
     * @return
     * @throws Exception
     */
    public static <T> T jsonParseObject(String jsonStr, Class<T> clazz)  {
        try {
            return objectMapper.readValue(jsonStr, clazz);
        } catch (Exception e) {
            throw new RobotException(e.getMessage() ,e);
        }
    }

    public static <T> List<T> jsonParseArray(String jsonStr, TypeReference<T> typeReference)  {
        try {
            return (List<T>)objectMapper.readValue(jsonStr, typeReference);
        } catch (Exception e) {
            throw new RobotException(e.getMessage(), e);
        }
    }


    public static String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RobotException(e.getMessage(), e);
        }
    }

    /***
     * 判断传入的对象是否为空
     *
     * @param obj
     *            待检查的对象
     * @return 返回的布尔值,为空或等于0时返回true
     */
    public static boolean isEmpty(Object obj) {
        return checkObjectIsEmpty(obj, true);
    }

    /***
     * 判断传入的对象是否不为空
     *
     * @param obj
     *            待检查的对象
     * @return 返回的布尔值,不为空或不等于0时返回true
     */
    public static boolean isNotEmpty(Object obj) {
        return checkObjectIsEmpty(obj, false);
    }

    @SuppressWarnings("rawtypes")
    private static boolean checkObjectIsEmpty(Object obj, boolean bool) {
        if (null == obj) {
            return bool;
        }
        else if (obj == "" || "".equals(obj) || "null".equalsIgnoreCase(String.valueOf(obj))) {
            return bool;
        }
        else if (obj instanceof Integer || obj instanceof Long || obj instanceof Double) {
            try {
                Double.parseDouble(obj + "");
            } catch (Exception e) {
                return bool;
            }
        } else if (obj instanceof String) {
            if (((String) obj).length() <= 0) {
                return bool;
            }
            if ("null".equalsIgnoreCase(obj+"")) {
                return bool;
            }
        } else if (obj instanceof Map) {
            if (((Map) obj).size() == 0) {
                return bool;
            }
        } else if (obj instanceof Collection) {
            if (((Collection) obj).size() == 0) {
                return bool;
            }
        } else if (obj instanceof Object[]) {
            if (((Object[]) obj).length == 0) {
                return bool;
            }
        }
        return !bool;
    }


    /**
     * 构建过滤方法名集合，默认包含Object类里公共方法
     * @param excludeMethodClass  如果有指定，则添加指定类下所有方法名
     *
     * @return
     */
    public static Set<String> buildExcludedMethodName(Class<?>... excludeMethodClass) {
        if(EXCLUDED_METHOD_NAME.isEmpty()) {
            Method[] objectMethods = Object.class.getDeclaredMethods();
            for (Method m : objectMethods) {
                EXCLUDED_METHOD_NAME.add(m.getName());
            }
        }
        Set<String> tmpExcludeMethodName = null;
        if(null != excludeMethodClass) {
            tmpExcludeMethodName = new HashSet<>();
            for (Class excludeClass : excludeMethodClass) {
                Method[] excludeMethods = excludeClass.getDeclaredMethods();
                if (null != excludeMethods) {
                    for (Method method : excludeMethods) {
                        tmpExcludeMethodName.add(method.getName());
                    }
                }
            }
            tmpExcludeMethodName.addAll(EXCLUDED_METHOD_NAME);
        }
        return (null == tmpExcludeMethodName) ? EXCLUDED_METHOD_NAME : tmpExcludeMethodName;
    }

    /**
     * 是否正常公用的API方法
     * 正常方法是指访问权限是public的且不是抽像，静态，接口，Final的方法
     * @param mod       Modifier的mod
     * @return
     */
    public static boolean isPublicMethod(int mod) {
        return !(Modifier.isAbstract(mod) || Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isInterface(mod) || Modifier.isPrivate(mod) || Modifier.isProtected(mod));
    }

    /**
     * 合并数组
     *
     * @param firstArray
     *            第一个数组
     * @param secondArray
     *            第二个数组
     * @return 合并后的数组
     */
    public static byte[] concat(byte[] firstArray, byte[] secondArray) {
        if (firstArray == null || secondArray == null) {
            return null;
        }
        byte[] bytes = new byte[firstArray.length + secondArray.length];
        System.arraycopy(firstArray, 0, bytes, 0, firstArray.length);
        System.arraycopy(secondArray, 0, bytes, firstArray.length, secondArray.length);
        return bytes;
    }

    /**
     * 取车辆host名称
     * @return
     */
    public static String getVehicleHostName() {
        return  SettingUtils.getStringByGroup("host.name", "vehicle", "host");
    }

    /**
     * 取车辆port名称
     * @return
     */
    public static String getVehiclePortName() {
        return SettingUtils.getStringByGroup("port.name", "vehicle", "port");
    }

    /**
     * 端口最小值
     * @return
     */
    public static int getMinPort(){
        return SettingUtils.getInt("min.port", "vehicle", 5050);
    }

    /**
     * 端口最大值
     * @return
     */
    public static int getMaxPort(){
        return SettingUtils.getInt("max.port", "vehicle", 6060);
    }

    /**
     * 此通信适配器的命令队列接受的命令数。必须至少为1。
     * @return
     */
    public static int getCommandQueueCapacity(){
//            return 100;
        return 2;
//            return isTrafficControl(AppContext.getCommAdapter().getProcessModel()) ? 3 :100;
    }

    /**
     * 要发送给车辆的最大订单数。
     * @return
     */
    public static int getSentQueueCapacity() {
//            return 100;
        return 2;
//            return isTrafficControl(AppContext.getCommAdapter().getProcessModel()) ? 2 :100;
    }
}
