
package com.codeabovelab.dm.agent.infocol;

import com.codeabovelab.dm.agent.notifier.SysInfo;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class InfoCollectorTest {

    @Test
    public void test() throws InterruptedException {
        InfoCollector ic = new InfoCollector(null);
        ic.refresh();
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        ic.refresh();
        SysInfo info = ic.getInfo();
        assertNotNull(info);
        System.out.println(info);
        assertNotNull(info.getDisks());
        SysInfo.Memory memory = info.getMemory();
        assertNotNull(memory);
        assertNotEquals(0F, memory.getAvailable());
        assertNotEquals(0F, memory.getTotal());
        assertNotEquals(0F, memory.getUsed());
        Map<String, SysInfo.Net> nets = info.getNet();
        assertFalse(nets.isEmpty());
        // we can not check networks because sometime it may produce 0 bytes as correct value
    }

}