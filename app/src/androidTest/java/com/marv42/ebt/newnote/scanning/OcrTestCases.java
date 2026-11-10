/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class OcrTestCases {

    private static final String OCR_DIR = "ocr";
    private static final String EXPECTED_FILE = "expected.txt";

    private OcrTestCases() {
    }

    static List<Case> all() {
        try {
            return parse();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<Object[]> parameters() {
        final List<Object[]> parameters = new ArrayList<>();
        for (Case testCase : all())
            parameters.add(new Object[]{testCase});
        return parameters;
    }

    private static List<Case> parse() throws IOException {
        final AssetManager assets = getInstrumentation().getContext().getAssets();
        final File dir = new File(getInstrumentation().getTargetContext().getCacheDir(), OCR_DIR);
        if (!dir.isDirectory() && !dir.mkdirs())
            throw new IOException("Cannot create " + dir);
        final List<Case> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                assets.open(OCR_DIR + "/" + EXPECTED_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                cases.add(parseLine(assets, dir, line));
            }
        }
        return cases;
    }

    private static Case parseLine(AssetManager assets, File dir, String line) throws IOException {
        final String[] fields = line.split(";", -1);
        if (fields.length != 2)
            throw new IOException("Invalid line: " + line);
        final String fileName = fields[0].trim();
        final List<String> serials = new ArrayList<>();
        for (String serial : fields[1].split(","))
            if (!serial.trim().isEmpty())
                serials.add(serial.trim());
        return new Case(fileName, extract(assets, dir, fileName), serials);
    }

    private static String extract(AssetManager assets, File dir, String fileName) throws IOException {
        final File file = new File(dir, fileName);
        final long assetSize = assetSize(fileName);
        if (file.length() != assetSize || assetSize <= 0) {
            try (InputStream input = assets.open(OCR_DIR + "/" + fileName);
                 FileOutputStream output = new FileOutputStream(file)) {
                final byte[] buffer = new byte[1024];
                int read;
                while ((read = input.read(buffer)) != -1)
                    output.write(buffer, 0, read);
            }
        }
        return file.getAbsolutePath();
    }

    private static long assetSize(String fileName) {
        try (ZipFile zip = new ZipFile(getInstrumentation().getContext().getPackageCodePath())) {
            final ZipEntry entry = zip.getEntry("assets/" + OCR_DIR + "/" + fileName);
            return entry == null ? -1 : entry.getSize();
        } catch (IOException e) {
            return -1;
        }
    }

    static final class Case {

        private final String fileName;
        private final String path;
        private final List<String> serials;

        private Case(String fileName, String path, List<String> serials) {
            this.fileName = fileName;
            this.path = path;
            this.serials = Collections.unmodifiableList(serials);
        }

        String path() {
            return path;
        }

        List<String> serials() {
            return serials;
        }

        @Override
        public String toString() {
            return fileName;
        }
    }
}
