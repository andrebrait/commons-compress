/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.pack200;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class Pack200UtilsTest extends AbstractTestCase {

    @Test
    public void testNormalize() throws Throwable {
        final File input = getFile("bla.jar");
        final File[] output = createTempDirAndFile();
        try {
            Pack200Utils.normalize(input, output[1], new HashMap<>());
            try (InputStream is = Files.newInputStream(output[1].toPath());
                    ArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("jar", is)) {

                ArchiveEntry entry = in.getNextEntry();
                while (entry != null) {
                    final File archiveEntry = new File(dir, entry.getName());
                    archiveEntry.getParentFile().mkdirs();
                    if (entry.isDirectory()) {
                        archiveEntry.mkdir();
                        entry = in.getNextEntry();
                        continue;
                    }
                    Files.copy(in, archiveEntry.toPath());
                    entry = in.getNextEntry();
                }

                in.close();
            }
        } finally {
            output[1].delete();
            output[0].delete();
        }
    }

    @Test
    public void testNormalizeInPlace() throws Throwable {
        final File input = getFile("bla.jar");
        final File[] output = createTempDirAndFile();
        try {
            try (InputStream is = Files.newInputStream(input.toPath())) {
                Files.copy(is, output[1].toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            Pack200Utils.normalize(output[1]);

            try (InputStream is = Files.newInputStream(output[1].toPath());
                    ArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("jar", is)) {

                ArchiveEntry entry = in.getNextEntry();
                while (entry != null) {
                    final File archiveEntry = new File(dir, entry.getName());
                    archiveEntry.getParentFile().mkdirs();
                    if (entry.isDirectory()) {
                        archiveEntry.mkdir();
                        entry = in.getNextEntry();
                        continue;
                    }
                    Files.copy(in, archiveEntry.toPath());
                    entry = in.getNextEntry();
                }
            }
        } finally {
            output[1].delete();
            output[0].delete();
        }
    }

}
