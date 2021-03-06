/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import static org.jetbrains.kotlin.codegen.inline.LocalVarRemapper.RemapStatus.*;

public class LocalVarRemapper {

    private final int allParamsSize;
    private final Parameters params;
    private final int actualParamsSize;

    private final StackValue[] remapValues;

    private final int additionalShift;

    public LocalVarRemapper(Parameters params, int additionalShift) {
        this.additionalShift = additionalShift;
        this.allParamsSize = params.totalSize();
        this.params = params;

        int realSize = 0;
        remapValues = new StackValue [params.totalSize()];

        int index = 0;
        for (ParameterInfo info : params) {
            if (!info.isSkippedOrRemapped()) {
                remapValues[index] = StackValue.local(realSize, AsmTypes.OBJECT_TYPE);
                realSize += info.getType().getSize();
            } else {
                remapValues[index] = info.isRemapped() ? info.getRemapValue() : null;
            }
            index++;
        }

        actualParamsSize = realSize;
    }

    public RemapInfo doRemap(int index) {
        int remappedIndex;

        if (index < allParamsSize) {
            ParameterInfo info = params.get(index);
            StackValue remapped = remapValues[index];
            if (info.isSkipped || remapped == null) {
                return new RemapInfo(info);
            }
            if (info.isRemapped()) {
                return new RemapInfo(remapped, info, REMAPPED);
            } else {
                remappedIndex = ((StackValue.Local)remapped).index;
            }
        } else {
            remappedIndex = actualParamsSize - params.totalSize() + index; //captured params not used directly in this inlined method, they used in closure
        }

        return new RemapInfo(StackValue.local(remappedIndex + additionalShift, AsmTypes.OBJECT_TYPE), null, SHIFT);
    }

    public RemapInfo remap(int index) {
        RemapInfo info = doRemap(index);
        if (FAIL == info.status) {
            assert info.parameterInfo != null : "Parameter info should be not null";
            throw new RuntimeException("Trying to access skipped parameter: " + info.parameterInfo.type + " at " +index);
        }
        return info;
    }

    public void visitIincInsn(int var, int increment, MethodVisitor mv) {
        RemapInfo remap = remap(var);
        assert remap.value instanceof StackValue.Local;
        mv.visitIincInsn(((StackValue.Local) remap.value).index, increment);
    }

    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index, MethodVisitor mv) {
        RemapInfo info = doRemap(index);
        //add entries only for shifted vars
        if (SHIFT == info.status) {
            int newIndex = ((StackValue.Local) info.value).index;
            if (newIndex != 0 && "this".equals(name)) {
                /*skip additional this for now*/
                return;
            }
            mv.visitLocalVariable(name, desc, signature, start, end, newIndex);
        }
    }


    public void visitVarInsn(int opcode, int var, InstructionAdapter mv) {
        RemapInfo remapInfo = remap(var);
        StackValue value = remapInfo.value;
        if (value instanceof StackValue.Local) {
            if (remapInfo.parameterInfo != null) {
                opcode = value.type.getOpcode(Opcodes.ILOAD);
            }
            mv.visitVarInsn(opcode, ((StackValue.Local) value).index);
            if (remapInfo.parameterInfo != null) {
                StackValue.coerce(value.type, remapInfo.parameterInfo.type, mv);
            }
        } else {
            assert remapInfo.parameterInfo != null : "Non local value should have parameter info";
            value.put(remapInfo.parameterInfo.type, mv);
        }
    }

    public enum RemapStatus {
        SHIFT,
        REMAPPED,
        FAIL
    }

    private static class RemapInfo {
        public final StackValue value;
        public final ParameterInfo parameterInfo;
        public final RemapStatus status;

        public RemapInfo(@NotNull StackValue value, @Nullable ParameterInfo info, RemapStatus remapStatus) {
            this.value = value;
            parameterInfo = info;
            this.status = remapStatus;
        }

        public RemapInfo(@NotNull ParameterInfo info) {
            this.value = null;
            parameterInfo = info;
            this.status = FAIL;
        }
    }
}
