/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.zip.ZipException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JUnit testcases for org.apache.commons.compress.archivers.zip.ExtraFieldUtils.
 *
 */
public class ExtraFieldUtilsTest implements UnixStat {

    public static class AiobThrowingExtraField implements ZipExtraField {
        static final int LENGTH = 4;
        @Override
        public byte[] getCentralDirectoryData() {
            return getLocalFileDataData();
        }
        @Override
        public ZipShort getCentralDirectoryLength() {
            return getLocalFileDataLength();
        }

        @Override
        public ZipShort getHeaderId() {
            return AIOB_HEADER;
        }

        @Override
        public byte[] getLocalFileDataData() {
            return new byte[LENGTH];
        }

        @Override
        public ZipShort getLocalFileDataLength() {
            return new ZipShort(LENGTH);
        }

        @Override
        public void parseFromCentralDirectoryData(final byte[] buffer, final int offset, final int length) {
            parseFromLocalFileData(buffer, offset, length);
        }

        @Override
        public void parseFromLocalFileData(final byte[] buffer, final int offset, final int length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Header-ID of a ZipExtraField not supported by Commons Compress.
     *
     * <p>Used to be ZipShort(1) but this is the ID of the Zip64 extra
     * field.</p>
     */
    static final ZipShort UNRECOGNIZED_HEADER = new ZipShort(0x5555);

    /**
     * Header-ID of a ZipExtraField not supported by Commons Compress
     * used for the ArrayIndexOutOfBoundsTest.
     */
    static final ZipShort AIOB_HEADER = new ZipShort(0x1000);
    private AsiExtraField a;
    private UnrecognizedExtraField dummy;
    private byte[] data;

    private byte[] aLocal;

    @Test
    public void parseTurnsArrayIndexOutOfBoundsIntoZipException() {
        ExtraFieldUtils.register(AiobThrowingExtraField.class);
        final AiobThrowingExtraField f = new AiobThrowingExtraField();
        final byte[] d = new byte[4 + AiobThrowingExtraField.LENGTH];
        System.arraycopy(f.getHeaderId().getBytes(), 0, d, 0, 2);
        System.arraycopy(f.getLocalFileDataLength().getBytes(), 0, d, 2, 2);
        System.arraycopy(f.getLocalFileDataData(), 0, d, 4, AiobThrowingExtraField.LENGTH);
        try {
            ExtraFieldUtils.parse(d);
            fail("data should be invalid");
        } catch (final ZipException e) {
            assertEquals("message",
                         "Failed to parse corrupt ZIP extra field of type 1000",
                         e.getMessage());
        }
    }

    @BeforeEach
    public void setUp() {
        a = new AsiExtraField();
        a.setMode(0755);
        a.setDirectory(true);
        dummy = new UnrecognizedExtraField();
        dummy.setHeaderId(UNRECOGNIZED_HEADER);
        dummy.setLocalFileDataData(new byte[] {0});
        dummy.setCentralDirectoryData(new byte[] {0});

        aLocal = a.getLocalFileDataData();
        final byte[] dummyLocal = dummy.getLocalFileDataData();
        data = new byte[4 + aLocal.length + 4 + dummyLocal.length];
        System.arraycopy(a.getHeaderId().getBytes(), 0, data, 0, 2);
        System.arraycopy(a.getLocalFileDataLength().getBytes(), 0, data, 2, 2);
        System.arraycopy(aLocal, 0, data, 4, aLocal.length);
        System.arraycopy(dummy.getHeaderId().getBytes(), 0, data,
                         4+aLocal.length, 2);
        System.arraycopy(dummy.getLocalFileDataLength().getBytes(), 0, data,
                         4+aLocal.length+2, 2);
        System.arraycopy(dummyLocal, 0, data,
                         4+aLocal.length+4, dummyLocal.length);

    }

    /**
     * Test merge methods
     */
    @Test
    public void testMerge() {
        final byte[] local =
            ExtraFieldUtils.mergeLocalFileDataData(new ZipExtraField[] {a, dummy});
        assertEquals("local length", data.length, local.length);
        for (int i=0; i<local.length; i++) {
            assertEquals("local byte "+i, data[i], local[i]);
        }

        final byte[] dummyCentral = dummy.getCentralDirectoryData();
        final byte[] data2 = new byte[4 + aLocal.length + 4 + dummyCentral.length];
        System.arraycopy(data, 0, data2, 0, 4 + aLocal.length + 2);
        System.arraycopy(dummy.getCentralDirectoryLength().getBytes(), 0,
                         data2, 4+aLocal.length+2, 2);
        System.arraycopy(dummyCentral, 0, data2,
                         4+aLocal.length+4, dummyCentral.length);


        final byte[] central =
            ExtraFieldUtils.mergeCentralDirectoryData(new ZipExtraField[] {a, dummy});
        assertEquals("central length", data2.length, central.length);
        for (int i=0; i<central.length; i++) {
            assertEquals("central byte "+i, data2[i], central[i]);
        }

    }

    @Test
    public void testMergeWithUnparseableData() throws Exception {
        final ZipExtraField d = new UnparseableExtraFieldData();
        final byte[] b = UNRECOGNIZED_HEADER.getBytes();
        d.parseFromLocalFileData(new byte[] {b[0], b[1], 1, 0}, 0, 4);
        final byte[] local =
            ExtraFieldUtils.mergeLocalFileDataData(new ZipExtraField[] {a, d});
        assertEquals("local length", data.length - 1, local.length);
        for (int i = 0; i < local.length; i++) {
            assertEquals("local byte " + i, data[i], local[i]);
        }

        final byte[] dCentral = d.getCentralDirectoryData();
        final byte[] data2 = new byte[4 + aLocal.length + dCentral.length];
        System.arraycopy(data, 0, data2, 0, 4 + aLocal.length + 2);
        System.arraycopy(dCentral, 0, data2,
                         4 + aLocal.length, dCentral.length);


        final byte[] central =
            ExtraFieldUtils.mergeCentralDirectoryData(new ZipExtraField[] {a, d});
        assertEquals("central length", data2.length, central.length);
        for (int i = 0; i < central.length; i++) {
            assertEquals("central byte " + i, data2[i], central[i]);
        }

    }

    /**
     * test parser.
     */
    @Test
    public void testParse() throws Exception {
        final ZipExtraField[] ze = ExtraFieldUtils.parse(data);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnrecognizedExtraField);
        assertEquals("data length field 2", 1,
                     ze[1].getLocalFileDataLength().getValue());

        final byte[] data2 = new byte[data.length-1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        try {
            ExtraFieldUtils.parse(data2);
            fail("data should be invalid");
        } catch (final Exception e) {
            assertEquals("message",
                         "Bad extra field starting at "+(4 + aLocal.length)
                         + ".  Block length of 1 bytes exceeds remaining data of 0 bytes.",
                         e.getMessage());
        }
    }

    @Test
    public void testParseCentral() throws Exception {
        final ZipExtraField[] ze = ExtraFieldUtils.parse(data,false);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnrecognizedExtraField);
        assertEquals("data length field 2", 1,
                     ze[1].getCentralDirectoryLength().getValue());

    }

    @Test
    public void testParseWithRead() throws Exception {
        ZipExtraField[] ze =
            ExtraFieldUtils.parse(data, true,
                                  ExtraFieldUtils.UnparseableExtraField.READ);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnrecognizedExtraField);
        assertEquals("data length field 2", 1,
                     ze[1].getLocalFileDataLength().getValue());

        final byte[] data2 = new byte[data.length-1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        ze = ExtraFieldUtils.parse(data2, true,
                                   ExtraFieldUtils.UnparseableExtraField.READ);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnparseableExtraFieldData);
        assertEquals("data length field 2", 4,
                     ze[1].getLocalFileDataLength().getValue());
        for (int i = 0; i < 4; i++) {
            assertEquals("byte number " + i,
                         data2[data.length - 5 + i],
                         ze[1].getLocalFileDataData()[i]);
        }
    }

    @Test
    public void testParseWithSkip() throws Exception {
        ZipExtraField[] ze =
            ExtraFieldUtils.parse(data, true,
                                  ExtraFieldUtils.UnparseableExtraField.SKIP);
        assertEquals("number of fields", 2, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
        assertTrue("type field 2", ze[1] instanceof UnrecognizedExtraField);
        assertEquals("data length field 2", 1,
                     ze[1].getLocalFileDataLength().getValue());

        final byte[] data2 = new byte[data.length-1];
        System.arraycopy(data, 0, data2, 0, data2.length);
        ze = ExtraFieldUtils.parse(data2, true,
                                   ExtraFieldUtils.UnparseableExtraField.SKIP);
        assertEquals("number of fields", 1, ze.length);
        assertTrue("type field 1", ze[0] instanceof AsiExtraField);
        assertEquals("mode field 1", 040755,
                     ((AsiExtraField) ze[0]).getMode());
    }
}
