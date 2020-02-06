package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;


public class VersionComparatorTest {

    private final VersionComparator vc = VersionComparator.builder()
      .addLatest("latest").addLatest("nightly")
      .addSuffix("rc")
      .addSuffix("ga")
      .build();
    @Test
    public void test() {
        compare( 0,        "",        "");
        compare( 0,       "1",       "1");
        compare( 0,      null,      null);
        compare( 1,       "1",      null);
        compare( 1,       "1",        "");
        compare(-1,       "1",     "1.1");
        compare(-1,       "1",    "1.10");
        compare( 1,    "1.10",     "1.9");
        compare(-1,     "1.1",     "1.2");
        compare(-1, "1.1.123", "1.9.123");
        compare( 0, "1.9.123", "1.9.123");
        compare( 1, "1.9.124", "1.9.123");
        compare(-1, "1.9.124",  "latest");
        compare(-1,  "latest", "nightly");
        compare(-1,  "1.9_rc",     "1.9");
        compare( 1,  "1.9_ga",  "1.9_rc");
        compare( 0,  "1.9_ga",  "1.9_ga");
        compare(-1,  "1.9_ga", "1.19_ga");
        compare(-1,    "1.8a",    "1.10");
        compare(-1,    "1.8a",     "1.8");
        compare(-1,"0.2-dev.1-sha.23f4399.dirty", "0.12-dev.10-sha.9984399");
    }

    @Test
    public void sort() {
        String[] src = new String[]{
          "0.12-dev.2-sha.9984399",
          "0.2-dev.1-sha.23f4399.dirty",
          "0.12-dev.10-sha.9984399",
          "1",
          "1-alpine",
          "0.10",
          "1.10",
          "1.10-alpine",
          "1.8.1",
          "1.8.1-alpine",
          "1.8",
          "1.8-alpine",
          "latest"
        };
        String[] exp = new String[]{
          "0.2-dev.1-sha.23f4399.dirty",
          "0.10",
          "0.12-dev.2-sha.9984399",
          "0.12-dev.10-sha.9984399",
          "1-alpine",
          "1",
          "1.8-alpine",
          "1.8",
          "1.8.1-alpine",
          "1.8.1",
          "1.10-alpine",
          "1.10",
          "latest"
        };
        VersionComparator vc = VersionComparator.builder()
          .addLatest("latest")
          .build();
        Arrays.sort(src, (a, b) -> {
            int res = vc.compare(a, b);
            System.out.println(a + (res == 0? " == " : (res > 0? " > " : " < ")) + b);
            return res;
        });
        System.out.println(Arrays.toString(src));
        assertArrayEquals(exp, src);
    }

    private void compare(int expected,  String left, String right) {
        String desc = "'" + left + "' - '" + right + "'";
        int compare = 0, invCompare = 0;
        try {
            compare = vc.compare(left, right);
            invCompare = vc.compare(right, left);
        } catch (Exception e) {
            throw new RuntimeException("on " + desc, e);
        }
        assertEquals("Test commutativity on " + desc, compare, -invCompare);
        assertEquals("Test  on " + desc, expected, compare);
    }
}