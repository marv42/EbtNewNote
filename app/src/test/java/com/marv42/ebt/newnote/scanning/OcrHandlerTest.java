/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.app.Application;
import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)
public class OcrHandlerTest {

    private static final String SERIAL_NUMBER = "XA1234567890";

    @Test
    public void deliversOcrResultToCallback() throws InterruptedException {
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        OcrHandler handler = new FakeOcrHandler(value -> {
            result.set(value);
            delivered.countDown();
        });

        handler.execute();
        runMainTasksUntil(delivered);

        assertEquals(SERIAL_NUMBER, result.get());
    }

    @Test
    public void processesOcrText() {
        FakeOcrHandler handler = new FakeOcrHandler(value -> {
        });

        assertEquals(SERIAL_NUMBER, handler.process("XX\n" + SERIAL_NUMBER + "\nZZ"));
    }

    @Test
    public void cancelPreventsCallback() throws InterruptedException {
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean();
        FakeOcrHandler handler = new FakeOcrHandler(value -> called.set(true), gate, started, finished);

        handler.execute();
        runMainTasksUntil(started);
        handler.cancel();
        gate.countDown();
        runMainTasksUntil(finished);
        pumpMainLooper(1_000);

        assertFalse(called.get());
    }

    // Gives a callback that should not happen the chance to arrive anyway.
    private static void pumpMainLooper(long millis) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            ShadowLooper.shadowMainLooper().idle();
            Thread.sleep(10);
        }
    }

    // The OCR work runs on a background thread, the callback is posted to the main looper,
    // which Robolectric does not execute on its own.
    private static void runMainTasksUntil(CountDownLatch latch) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (!latch.await(50, TimeUnit.MILLISECONDS)) {
            if (System.currentTimeMillis() > deadline)
                fail("Latch was not counted down");
            ShadowLooper.shadowMainLooper().idle();
        }
    }

    private static class FakeOcrHandler extends OcrHandler {

        private final CountDownLatch gate;
        private final CountDownLatch started;
        private final CountDownLatch finished;

        FakeOcrHandler(Callback callback) {
            this(callback, null, null, null);
        }

        FakeOcrHandler(Callback callback, CountDownLatch gate, CountDownLatch started,
                       CountDownLatch finished) {
            super(callback, "photo.jpg", null,
                    RuntimeEnvironment.getApplication().getContentResolver());
            this.gate = gate;
            this.started = started;
            this.finished = finished;
        }

        @Override
        protected String doInBackground() {
            countDown(started);
            await(gate);
            countDown(finished);
            return SERIAL_NUMBER;
        }

        String process(String allResults) {
            return toOcrResult(allResults);
        }

        private static void countDown(CountDownLatch latch) {
            if (latch != null)
                latch.countDown();
        }

        private static void await(CountDownLatch latch) {
            if (latch == null)
                return;
            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
