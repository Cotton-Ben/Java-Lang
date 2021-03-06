/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Long.numberOfLeadingZeros;

/**
 * @author peter.lawrey
 */
public enum Jvm {
    ;
    public static final String TMP = System.getProperty("java.io.tmpdir");
    private static final boolean IS64BIT = is64Bit0();
    private static final Logger LOG = LoggerFactory.getLogger(Jvm.class);

    public static boolean is64Bit() {
        return IS64BIT;
    }

    private static boolean is64Bit0() {
        String systemProp;
        systemProp = System.getProperty("com.ibm.vm.bitmode");
        if (systemProp != null) {
            return "64".equals(systemProp);
        }
        systemProp = System.getProperty("sun.arch.data.model");
        if (systemProp != null) {
            return "64".equals(systemProp);
        }
        systemProp = System.getProperty("java.vm.version");
        return systemProp != null && systemProp.contains("_64");
    }

    private static final int PROCESS_ID = getProcessId0();

    public static int getProcessId() {
        return PROCESS_ID;
    }

    private static int getProcessId0() {
        String pid = null;
        final File self = new File("/proc/self");
        try {
            if (self.exists())
                pid = self.getCanonicalFile().getName();
        } catch (IOException ignored) {
            // ignored
        }
        if (pid == null)
            pid = ManagementFactory.getRuntimeMXBean().getName().split("@", 0)[0];
        if (pid == null) {
            int rpid = new Random().nextInt(1 << 16);
            LoggerFactory.getLogger(Jvm.class).warn("Unable to determine PID, picked a random number={}", rpid);
            return rpid;
        } else {
            return Integer.parseInt(pid);
        }
    }

    /**
     * This may or may not be the OS thread id, but should be unique across processes
     *
     * @return a unique tid of up to 48 bits.
     */
    public static long getUniqueTid() {
        return getUniqueTid(Thread.currentThread());
    }

    public static long getUniqueTid(Thread thread) {
        return ((long) getProcessId() << 32) | thread.getId();
    }

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.startsWith("win");
    }

    public static boolean isMacOSX() {
        return OS.contains("mac");
    }

    public static boolean isLinux() {
        return OS.startsWith("linux");
    }

    public static boolean isUnix() {
        return OS.contains("nix") ||
                OS.contains("nux") ||
                OS.contains("aix") ||
                OS.contains("bsd") ||
                OS.contains("hpux");
    }

    public static boolean isSolaris() {
        return OS.startsWith("sun");
    }

    public static final int PID_BITS = Maths.intLog2(getPidMax());

    public static long getPidMax() {
        if (isLinux()) {
            File file = new File("/proc/sys/kernel/pid_max");
            if (file.canRead())
                try {
                    final long n = new Scanner(file).nextLong();

                    //  if the number of threads is not a power of 2 then we round up
                    return (n & (n - 1)) == 0 ? n : 1L << -numberOfLeadingZeros(n);

                } catch (FileNotFoundException e) {
                    LOG.error("", e);
                }
        } else if (isMacOSX()) {
            return 1L << 24;
        }
        // the default.
        return 1L << 16;
    }


}
