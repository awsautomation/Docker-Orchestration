
package com.codeabovelab.dm.common.utils;

import java.net.InetAddress;

public class OSUtils {
    /**
     * operating system family
     */
    private enum Family {
        WINDOWS, LINUX, MAC, UNKNOWN, ANDROID
    }

    static {
        family = detectOsFamily();
    }

    private static final Family family;

    private static Family detectOsFamily() {
        final String kernelName = System.getProperty("os.name").toLowerCase();
        final Family family;
        if(kernelName.contains("linux")) {
            family = Family.LINUX;
        } else if(kernelName.contains("win")) {
            family = Family.WINDOWS;
        } else if(kernelName.contains("mac")) {
            family = Family.MAC;
        } else {
            family = Family.UNKNOWN;
        }
        return family;
    }

    public static String getHostName() {
        String name = null;
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch(Exception e) {
            // so we use this
            System.out.println("can not detect host name forom localhost");
        }
        // obviously, we don`t need names like `localhost`
        if(name != null && name.contains("localhost")) {
            name = null;
        }
        if(name == null) {
            if(family == Family.WINDOWS) {
                name = System.getenv("COMPUTERNAME");
            } else if(family == Family.LINUX) {
                name = System.getenv("HOSTNAME");
                if(name == null) {
                    name = System.getenv("HOST");
                }
            }
        }
        return name;
    }

    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
}
