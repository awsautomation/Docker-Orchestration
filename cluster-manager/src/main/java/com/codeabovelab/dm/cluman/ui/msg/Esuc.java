

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.utils.Closeables;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * event source update context
 */
@Slf4j
class Esuc {
    private final Map<String, Subscriptions<?>> newMap;
    private final Map<String, Subscriptions<?>> oldMap;

    public Esuc(Map<String, Subscriptions<?>> oldMap) {
        this.oldMap = oldMap;
        this.newMap = new HashMap<>(oldMap.size());
    }

    void update(String key, Function<String, Subscriptions<?>> factory) {
        Subscriptions<?> subs = this.oldMap.get(key);
        if(subs == null) {
            try {
                subs = factory.apply(key);
            } catch (Exception e) {
                log.error("Can not update subscriptions for '{}' key, due to error:", key, e);
            }
        }
        newMap.put(key, subs);
    }

    void putAll(Map<String, Subscriptions<?>> systemSubs) {
        this.newMap.putAll(systemSubs);
    }

    Map<String, Subscriptions<?>> getNewMap() {
        return newMap;
    }

    Map<String, Subscriptions<?>> getOldMap() {
        return oldMap;
    }

    void free() {
        //close outdated subscriptions (do not put it in finally block)
        for(Map.Entry<String, Subscriptions<?>> e: oldMap.entrySet()) {
            if(newMap.containsKey(e.getKey())) {
                continue;
            }
            Subscriptions<?> value = e.getValue();
            Closeables.closeIfCloseable(value);
        }
    }
}
