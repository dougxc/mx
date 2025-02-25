/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.mxtool.junit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;

/**
 * Timing support for JUnit test runs.
 */
class TimingDecorator extends MxRunListenerDecorator {

    private long startTime;
    private long classStartTime;
    private Description currentTest;
    final Map<Class<?>, Long> classTimes;
    final Map<Description, Long> testTimes;

    TimingDecorator(MxRunListener l) {
        super(l);
        this.classTimes = new ConcurrentHashMap<>();
        this.testTimes = new ConcurrentHashMap<>();
    }

    @Override
    public void testClassStarted(Class<?> clazz) {
        classStartTime = System.nanoTime();
        super.testClassStarted(clazz);
    }

    @Override
    public void testClassFinished(Class<?> clazz, int numPassed, int numFailed, int numIgnored, int numAssumptionFailed) {
        long totalTime = System.nanoTime() - classStartTime;
        super.testClassFinished(clazz, numPassed, numFailed, numIgnored, numAssumptionFailed);
        if (beVerbose()) {
            getWriter().print(' ' + valueToString(totalTime));
        }
        classTimes.put(clazz, totalTime / 1_000_000);
    }

    @Override
    public void testStarted(Description description) {
        currentTest = description;
        startTime = System.nanoTime();
        super.testStarted(description);
    }

    @Override
    public void testFinished(Description description) {
        long totalTime = System.nanoTime() - startTime;
        super.testFinished(description);
        if (beVerbose()) {
            getWriter().print(" " + valueToString(totalTime));
        }
        currentTest = null;
        testTimes.put(description, totalTime / 1_000_000);
    }

    static String valueToString(long valueNS) {
        long timeWholeMS = valueNS / 1_000_000;
        long timeFractionMS = (valueNS / 100_000) % 10;
        return String.format("%d.%d ms", timeWholeMS, timeFractionMS);
    }

    /**
     * Gets the test currently starting but not yet completed along with the number of milliseconds
     * it has been executing.
     *
     * @return {@code null} if there is no test currently executing
     */
    public Object[] getCurrentTestDuration() {
        Description current = currentTest;
        if (current != null) {
            long timeMS = (System.nanoTime() - startTime) / 1_000_000;
            return new Object[]{current, timeMS};
        }
        return null;
    }
}
