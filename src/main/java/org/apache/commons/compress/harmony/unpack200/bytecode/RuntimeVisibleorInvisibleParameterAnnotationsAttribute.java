/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameter annotations class file attribute, either a RuntimeVisibleParameterAnnotations attribute or a
 * RuntimeInvisibleParameterAnnotations attribute.
 */
public class RuntimeVisibleorInvisibleParameterAnnotationsAttribute extends AnnotationsAttribute {

    /**
     * ParameterAnnotation represents the annotations on a single parameter.
     */
    public static class ParameterAnnotation {

        private final Annotation[] annotations;
        private final int num_annotations;

        public ParameterAnnotation(final Annotation[] annotations) {
            this.num_annotations = annotations.length;
            this.annotations = annotations;
        }

        public List<Object> getClassFileEntries() {
            final List<Object> nested = new ArrayList<>();
            for (Annotation annotation : annotations) {
                nested.addAll(annotation.getClassFileEntries());
            }
            return nested;
        }

        public int getLength() {
            int length = 2;
            for (Annotation annotation : annotations) {
                length += annotation.getLength();
            }
            return length;
        }

        public void resolve(final ClassConstantPool pool) {
            for (Annotation annotation : annotations) {
                annotation.resolve(pool);
            }
        }

        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeShort(num_annotations);
            for (Annotation annotation : annotations) {
                annotation.writeBody(dos);
            }
        }

    }
    private final int num_parameters;

    private final ParameterAnnotation[] parameter_annotations;

    public RuntimeVisibleorInvisibleParameterAnnotationsAttribute(final CPUTF8 name,
        final ParameterAnnotation[] parameter_annotations) {
        super(name);
        this.num_parameters = parameter_annotations.length;
        this.parameter_annotations = parameter_annotations;
    }

    @Override
    protected int getLength() {
        int length = 1;
        for (int i = 0; i < num_parameters; i++) {
            length += parameter_annotations[i].getLength();
        }
        return length;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List<Object> nested = new ArrayList<>();
        nested.add(attributeName);
        for (ParameterAnnotation parameter_annotation : parameter_annotations) {
            nested.addAll(parameter_annotation.getClassFileEntries());
        }
        return nested.toArray(ClassFileEntry.NONE);
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        for (ParameterAnnotation parameter_annotation : parameter_annotations) {
            parameter_annotation.resolve(pool);
        }
    }

    @Override
    public String toString() {
        return attributeName.underlyingString() + ": " + num_parameters + " parameter annotations";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeByte(num_parameters);
        for (int i = 0; i < num_parameters; i++) {
            parameter_annotations[i].writeBody(dos);
        }
    }

}
