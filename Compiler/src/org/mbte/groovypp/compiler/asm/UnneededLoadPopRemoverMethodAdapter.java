/*
 * Copyright 2009-2011 MBTE Sweden AB.
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

package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.CompilerStack;

public class UnneededLoadPopRemoverMethodAdapter extends UnneededDupXStoreRemoverMethodAdapter {
    private Load load;

    static abstract class Load {
        abstract void execute ();
    }

    class VarLabel extends Load {
        private Label label;

        public VarLabel(Label label) {
            this.label = label;
        }

        void execute() {
            UnneededLoadPopRemoverMethodAdapter.super.visitLabel(label);
        }
    }

    class LoadVar extends Load {
        private int opcode;
        private int var;

        public LoadVar(int opcode, int var) {
            this.opcode = opcode;
            this.var = var;
        }

        public void execute() {
            UnneededLoadPopRemoverMethodAdapter.super.visitVarInsn(opcode, var);
        }
    }

    class LoadIIncVar extends Load {
        private int opcode;
        private int var;
        private int increment;

        public LoadIIncVar(int opcode, int var, int increment) {
            this.opcode = opcode;
            this.var = var;
            this.increment = increment;
        }

        public void execute() {
            UnneededLoadPopRemoverMethodAdapter.super.visitVarInsn(opcode, var);
            UnneededLoadPopRemoverMethodAdapter.super.visitIincInsn(var, increment);
        }
    }

    class Ldc extends Load {
        private Object constant;

        public Ldc(Object cst) {
            this.constant = cst;
        }

        public void execute() {
            UnneededLoadPopRemoverMethodAdapter.super.visitLdcInsn(constant);
        }
    }

    class Checkcast extends Load {
        private String descr;

        public Checkcast(String descr) {
            this.descr = descr;
        }

        public void execute() {
            UnneededLoadPopRemoverMethodAdapter.super.visitTypeInsn(CHECKCAST, descr);
        }
    }

    public UnneededLoadPopRemoverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropLoad() {
        if (load != null) {
            load.execute();
            load = null;
        }
    }

    public void visitInsn(int opcode) {
        if (load != null && (opcode == POP || opcode == POP2)) {
            if (load instanceof VarLabel) {
                super.visitInsn(opcode);
                super.visitLabel(((VarLabel)load).label);
            }
            else
                if (load instanceof Checkcast) {
                    super.visitInsn(opcode);
                }
                else {
                    if(load instanceof LoadIIncVar) {
                        super.visitIincInsn(((LoadIIncVar) load).var, ((LoadIIncVar) load).increment);
                    }
                }
            load = null;
            return;
        }

        if(opcode == RETURN) {
            load = null;
            super.visitInsn(opcode);
            return;
        }

        dropLoad();
        switch (opcode) {
            case ACONST_NULL: visitLdcInsn(null); break;
            case ICONST_M1: visitLdcInsn(-1); break;
            case ICONST_0:  visitLdcInsn(0); break;
            case ICONST_1:  visitLdcInsn(1); break;
            case ICONST_2:  visitLdcInsn(2); break;
            case ICONST_3:  visitLdcInsn(3); break;
            case ICONST_4:  visitLdcInsn(4); break;
            case ICONST_5:  visitLdcInsn(5); break;
            case LCONST_0:  visitLdcInsn(0L); break;
            case LCONST_1:  visitLdcInsn(1L); break;
            case FCONST_0:  visitLdcInsn(0.0f); break;
            case FCONST_1:  visitLdcInsn(1.0f); break;
            case FCONST_2:  visitLdcInsn(2.0f); break;
            case DCONST_0:  visitLdcInsn(0.0d); break;
            case DCONST_1:  visitLdcInsn(1.0d); break;

            default:
                super.visitInsn(opcode);
        }
    }

    public void visitIntInsn(int opcode, int operand) {
        dropLoad();
        switch (opcode) {
            case BIPUSH:
            case SIPUSH:
                visitLdcInsn(operand);
                break;

            default:
                super.visitIntInsn(opcode, operand);
        }
    }

    public void visitVarInsn(int opcode, int var) {
        dropLoad();
        switch (opcode) {
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                load = new LoadVar(opcode, var);
                break;
            
            default:
                super.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(int opcode, String desc) {
        dropLoad();
        switch (opcode) {
            case CHECKCAST:
                load = new Checkcast(desc);
                return;

            default:
                super.visitTypeInsn(opcode, desc);
                break;
        }
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropLoad();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        dropLoad();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
        if(opcode == IF_ICMPEQ || opcode == IF_ICMPNE) {
            if(load instanceof Ldc) {
                Ldc ldc = (Ldc) load;
                if(ldc.constant instanceof Integer && ((Integer)ldc.constant) == 0) {
                    super.visitJumpInsn(opcode == IF_ICMPEQ ? IFEQ : IFNE, label);
                    return;
                }
            }
        }

        dropLoad();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        if (label instanceof CompilerStack.VarStartLabel) {
            dropLoad();
            load = new VarLabel(label);
        }
        else {
            dropLoad();
            super.visitLabel(label);
        }
    }

    public void visitLdcInsn(Object cst) {
        dropLoad();
        load = new Ldc(cst);
    }

    public void visitIincInsn(int var, int increment) {
        if(load instanceof LoadVar && ((LoadVar)load).var == var) {
            load = new LoadIIncVar(((LoadVar)load).opcode, var, increment);
        }
        else {
            dropLoad();
            super.visitIincInsn(var, increment);
        }
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
        dropLoad();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
        dropLoad();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropLoad();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropLoad();
        super.visitTryCatchBlock(start, end, handler, type);
    }
}