package com.smart.sso.server.session;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理上下文的工具，对外提供获取各种上下文的方法
 */
public class ContextManager {

    private static final ThreadLocal<Map<String, Object>> contextCurrentRequest = ThreadLocal.withInitial(HashMap::new);

    /**
     * 根据名称获取上下文，这里如果当前请求没有，会去上一级寻找
     *
     * @param name
     * @return
     */
    public static Object getContextByName(String name) {
        return contextCurrentRequest.get().get(name);
    }

    public static void addContext(String key, Object value) {
        contextCurrentRequest.get().put(key, value);
    }

    public static void clearCurrentRequestContext() {
        contextCurrentRequest.remove();
    }

}

