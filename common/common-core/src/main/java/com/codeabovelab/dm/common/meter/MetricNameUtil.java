

package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.MetricRegistry;

import java.lang.reflect.Member;

/**
 * Tool for construct metric name <p/>
 *
 */
public final class MetricNameUtil {

    private MetricNameUtil() { }

    /**
     * create name for metric based on specified class name
     * @param clazz
     * @param name
     * @return
     */
    public static String getName(Class<?> clazz, String name) {
        return MetricRegistry.name(clazz.getCanonicalName(), name);
    }

    /**
     * create name for metric based on specified class name if 'absolute' == false
     * @param clazz
     * @param name
     * @param absolute
     * @return
     */
    public static String getName(Class<?> clazz, String name, boolean absolute) {
        if (absolute) {
            return name;
        }
        return MetricRegistry.name(clazz.getCanonicalName(), name);
    }

    /**
     * algorithm copied from
     * https://github.com/ryantenney/metrics-spring/blob/master/src/main/java/com/ryantenney/metrics/spring/Util.java
     * for full compliance with names in registry
     */
    static String chooseName(String explicitName, boolean absolute, Class<?> klass, Member member, String... suffixes) {
        if (explicitName != null && !explicitName.isEmpty()) {
            if (absolute) {
                return explicitName;
            }
            return MetricRegistry.name(klass.getCanonicalName(), explicitName);
        }
        return MetricRegistry.name(MetricRegistry.name(klass.getCanonicalName(), member.getName()), suffixes);
    }

}
