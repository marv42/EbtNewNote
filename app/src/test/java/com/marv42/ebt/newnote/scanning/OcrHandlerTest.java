/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static org.junit.Assert.assertEquals;
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

    // The OCR work runs on a background thread, the callback is posted to the main looper,
    // which Robolectric does not execute on its own.
    private static void runMainTasksUntil(CountDownLatch latch) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (!latch.await(50, TimeUnit.MILLISECONDS)) {
            if (System.currentTimeMillis() > deadline)
                fail("Callback was not called");
            ShadowLooper.shadowMainLooper().idle();
        }
    }

    private static class FakeOcrHandler extends OcrHandler {

        FakeOcrHandler(Callback callback) {
            super(callback, "photo.jpg", null,
                    RuntimeEnvironment.getApplication().getContentResolver());
        }

        @Override
        protected String doInBackground() {
            return SERIAL_NUMBER;
        }

        String process(String allResults) {
            return toOcrResult(allResults);
        }
    }
}
