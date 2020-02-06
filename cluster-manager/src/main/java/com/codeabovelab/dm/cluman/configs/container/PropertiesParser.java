
package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.model.ContainerSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Parser for properties like configs
 * TODO: migrate from CLI to compose API
 */
@Slf4j
@Deprecated
@Component
public class PropertiesParser extends AbstractParser {

    private final static String ENV = "environment";
    private final static String LABELS = "labels";

    @Override
    public void parse(String fileName, ContainerCreationContext context) {
        parse(fileName, context, ".properties");
    }

    @Override
    public void parse(File file, ContainerCreationContext context) {
        try (FileInputStream stream = new FileInputStream(file)) {
            Properties prop = new Properties();
            prop.load(stream);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new HashMap<>((Map) prop);
            parse(map, context);
        } catch (IOException e) {
            log.error("can't parse Properties", e);
        }
    }

    @Override
    public void parse(Map<String, Object> map, ContainerCreationContext context) {
        ContainerSource arg = new ContainerSource();
        context.addCreateContainerArg(arg);
        parse(map, arg);
    }

    public void parse(Map<String, Object> map, ContainerSource arg) {
        for (Map.Entry<String, Object> el : map.entrySet()) {
            String propKey = el.getKey();
            Object elValue = el.getValue();
            if (elValue instanceof String) {
                String val = (String) elValue;
                String[] split = StringUtils.split(propKey, ".");
                if (split != null && StringUtils.hasText(val)) {
                    String key = split[1];
                    switch (split[0]) {
                        case ENV:
                            arg.getEnvironment().add(key + "=" + val);
                            break;
                        case LABELS:
                            arg.getLabels().put(key, val);
                            break;
                    }
                } else {
                    try {
                        Object value;
                        if (val.contains(":")) {
                            value = parseMap(val);
                        } else if (val.contains(",")) {
                            value = parseList(val);
                        } else {
                            value = el.getValue();
                        }
                        BeanUtils.setProperty(arg, propKey, value);
                    } catch (Exception e) {
                        log.error("can't set value {} to property {}", propKey, el.getValue(), e);
                    }
                }
            }
        }
        log.info("Result of parsing {}", arg);
    }

    private List<String> parseList(String value) {
        List<String> list = new ArrayList<>();
        String[] split = value.split(",");
        Collections.addAll(list, split);
        return list;
    }

    private Map<String, String> parseMap(String value) {
        Map<String, String> map = new HashMap<>();
        String[] split = value.split(",");
        for (String s : split) {
            String[] m = s.split(":");
            if (m.length == 2) {
                map.put(m[0], m[1]);
            }
        }
        return map;
    }

}
