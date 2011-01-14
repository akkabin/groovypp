/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler.bytecode;

import org.mbte.groovypp.compiler.asm.LineNumberLabelSwitcherMethodAdapter;
import org.mbte.groovypp.compiler.bytecode.LocalVarInferenceTypes;
import org.mbte.groovypp.compiler.bytecode.LocalVarTypeInferenceState;
import org.mbte.groovypp.compiler.bytecode.BytecodeStack;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.IdentityHashMap;
import java.util.Stack;

public class StackAwareMethodAdapter extends LineNumberLabelSwitcherMethodAdapter implements Opcodes, LocalVarTypeInferenceState {

// TODO: Unimplemented bytecodes
//    int IINC = 132;
//    int JSR = 168;
//    int RET = 169;
//    int TABLESWITCH = 170;
//    int LOOKUPSWITCH = 171;

    private BytecodeStack stack = new BytecodeStack();
    private IdentityHashMap<Label, LocalVarInferenceTypes> labelMap = new IdentityHashMap<Label, LocalVarInferenceTypes>();

    private LocalVarInferenceTypes curInference = new LocalVarInferenceTypes();
    private static final LocalVarInferenceTypes AFTER_GOTO = new LocalVarInferenceTypes();

    public Stack<LocalVarInferenceTypes> loopInferences = new Stack<LocalVarInferenceTypes>();

    {
        loopInferences.push(AFTER_GOTO);
    }

    public void startLoopVisitLabel(Label label) {
        loopInferences.push(getLabelInfo(label));
        visitLabel(label);
    }

    private void jumpToLabel(int opcode, Label label) {
        final LocalVarInferenceTypes li = getLabelInfo(label);
        li.initFromStack(stack);

        li.jumpFrom(curInference);

        if (opcode == GOTO) {
            stack.clear();
            curInference = AFTER_GOTO;

            if (li.equals(loopInferences.peek())) loopInferences.pop();
        }
    }

    public void comeToLabel(Label label) {
        final LocalVarInferenceTypes li = getLabelInfo(label);
        li.initFromStack(stack);

        if (curInference != AFTER_GOTO)
            li.comeFrom(curInference);
//        System.out.println("// " + li.defVars);
        li.parentScopeInference = loopInferences.peek();
        curInference = li;
    }

    private LocalVarInferenceTypes getLabelInfo(Label label) {
        LocalVarInferenceTypes li = labelMap.get(label);
        if (li == null) {
            li = new LocalVarInferenceTypes();
            labelMap.put(label, li);
        }
        return li;
    }

    public LocalVarInferenceTypes getLocalVarInferenceTypes() {
        return curInference;
    }

    public StackAwareMethodAdapter(MethodVisitor methodVisitor) {
        super(methodVisitor);
    }

    @Override
    public void visitInsn(int i) {
        switch (i) {
            case LCMP:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case FCMPL:
            case FCMPG:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case DCMPL:
            case DCMPG:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case NOP:
                break;

            case ACONST_NULL:
                stack.push(BytecodeStack.KIND_OBJ);
                break;

            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                stack.push(BytecodeStack.KIND_INT);
                break;

            case LCONST_0:
            case LCONST_1:
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case DCONST_0:
            case DCONST_1:
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case POP:
                stack.pop();
                break;

            case POP2:
                stack.pop2();
                break;

            case DUP:
                stack.dup();
                break;

            case DUP_X1:
                stack.dup_x1();
                break;

            case DUP_X2:
                stack.dup_x2();
                break;

            case DUP2:
                stack.dup2();
                break;

            case DUP2_X1:
                stack.dup2_x1();
                break;

            case DUP2_X2:
                stack.dup2_x2();
                break;

            case SWAP:
                stack.swap();
                break;

            case I2L:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case I2F:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case I2D:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case L2I:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case L2F:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case L2D:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case F2I:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case F2L:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case F2D:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case D2I:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case D2L:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case D2F:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case I2B:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case I2C:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case I2S:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case IADD:
            case ISUB:
            case IDIV:
            case IMUL:
            case IREM:
            case ISHL:
            case IAND:
            case IOR:
            case IXOR:
            case IUSHR:
            case ISHR:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LAND:
            case LOR:
            case LXOR:
            case LREM:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case LSHL:
            case LUSHR:
            case LSHR:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case FADD:
            case FSUB:
            case FMUL:
            case FREM:
            case FDIV:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case INEG:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case LNEG:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case FNEG:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case DNEG:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case IRETURN:
                stack.pop(BytecodeStack.KIND_INT);
                stack.clear();
                break;

            case LRETURN:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.clear();
                break;

            case FRETURN:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.clear();
                break;

            case DRETURN:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.clear();
                break;

            case ARETURN:
            case ATHROW:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.clear();
                break;

            case RETURN:
                stack.clear();
                break;

            case MONITORENTER:
            case MONITOREXIT:
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_INT);
                break;

            case LALOAD:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_LONG);
                break;

            case FALOAD:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_FLOAT);
                break;

            case DALOAD:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;

            case AALOAD:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_OBJ);
                break;

            case IASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case LASTORE:
                stack.pop(BytecodeStack.KIND_LONG);
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case FASTORE:
                stack.pop(BytecodeStack.KIND_FLOAT);
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case DASTORE:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case AASTORE:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case ARRAYLENGTH:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_INT);
                break;

            default:
                throw new RuntimeException("Unrecognized operation");

        }
        super.visitInsn(i);
    }

    @Override
    public void visitIntInsn(int i, int i1) {
        switch (i) {
            case BIPUSH:
            case SIPUSH:
                stack.push(BytecodeStack.KIND_INT);
                break;

            case NEWARRAY:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_OBJ);
                break;

            default:
                throw new RuntimeException("Unrecognized operation");
        }
        super.visitIntInsn(i, i1);
    }

    @Override
    public void visitVarInsn(int i, int i1) {
        switch (i) {
            case ILOAD:
                stack.push(BytecodeStack.KIND_INT);
                break;
            case LLOAD:
                stack.push(BytecodeStack.KIND_LONG);
                break;
            case FLOAD:
                stack.push(BytecodeStack.KIND_FLOAT);
                break;
            case DLOAD:
                stack.push(BytecodeStack.KIND_DOUBLE);
                break;
            case ALOAD:
                stack.push(BytecodeStack.KIND_OBJ);
                break;
            case ISTORE:
                stack.pop(BytecodeStack.KIND_INT);
                break;
            case LSTORE:
                stack.pop(BytecodeStack.KIND_LONG);
                break;
            case FSTORE:
                stack.pop(BytecodeStack.KIND_FLOAT);
                break;
            case DSTORE:
                stack.pop(BytecodeStack.KIND_DOUBLE);
                break;
            case ASTORE:
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            default:
                throw new RuntimeException("Unrecognized operation");
        }

        super.visitVarInsn(i, i1);
    }

    @Override
    public void visitTypeInsn(int i, String s) {
        switch (i) {
            case NEW:
                stack.push(BytecodeStack.KIND_OBJ);
                break;

            case ANEWARRAY:
                stack.pop(BytecodeStack.KIND_INT);
                stack.push(BytecodeStack.KIND_OBJ);
                break;

            case CHECKCAST:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_OBJ);
                break;

            case INSTANCEOF:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(BytecodeStack.KIND_INT);
                break;

            default:
                throw new RuntimeException("Unrecognized operation");
        }
        super.visitTypeInsn(i, s);
    }

    @Override
    public void visitFieldInsn(int i, String s, String s1, String s2) {
        switch (i) {
            case GETSTATIC:
                stack.push(fieldKind(s2));
                break;

            case PUTSTATIC:
                stack.pop(fieldKind(s2));
                break;

            case GETFIELD:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.push(fieldKind(s2));
                break;

            case PUTFIELD:
                stack.pop(fieldKind(s2));
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            default:
                throw new RuntimeException("Unrecognized operation");
        }
        super.visitFieldInsn(i, s, s1, s2);
    }

    private byte fieldKind(String s2) {
        switch (s2.charAt(0)) {
            case 'I':
            case 'B':
            case 'S':
            case 'Z':
            case 'C':
                return BytecodeStack.KIND_INT;

            case 'F':
                return BytecodeStack.KIND_FLOAT;

            case 'D':
                return BytecodeStack.KIND_DOUBLE;

            case 'J':
                return BytecodeStack.KIND_LONG;

            default:
                return BytecodeStack.KIND_OBJ;
        }
    }

    @Override
    public void visitMethodInsn(int i, String s, String s1, String s2) {
        popArgs(s2);
        switch (i) {
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKEINTERFACE:
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case INVOKESTATIC:
                break;

            default:
                throw new RuntimeException("Unrecognized operation");
        }
        pushResult(s2);

        super.visitMethodInsn(i, s, s1, s2);
    }

    private void pushResult(String s2) {
        s2 = s2.substring(s2.indexOf(')') + 1);
        if (s2.charAt(0) != 'V')
            stack.push(fieldKind(s2));
    }

    private void popArgs(String s2) {
        s2 = s2.substring(1, s2.lastIndexOf(')'));
        byte[] args = new byte[256];
        int count = 0;
        while (s2.length() > 0) {
            switch (s2.charAt(0)) {
                case 'I':
                case 'B':
                case 'S':
                case 'Z':
                case 'C':
                    args[count++] = BytecodeStack.KIND_INT;
                    s2 = s2.substring(1);
                    break;

                case 'F':
                    args[count++] = BytecodeStack.KIND_FLOAT;
                    s2 = s2.substring(1);
                    break;

                case 'D':
                    args[count++] = BytecodeStack.KIND_DOUBLE;
                    s2 = s2.substring(1);
                    break;

                case 'J':
                    args[count++] = BytecodeStack.KIND_LONG;
                    s2 = s2.substring(1);
                    break;

                case '[':
                    args[count++] = BytecodeStack.KIND_OBJ;
                    int k = 1;
                    while (s2.charAt(k) == '[')
                        k++;
                    if (s2.charAt(k) == 'L')
                        s2 = s2.substring(s2.indexOf(';') + 1);
                    else
                        s2 = s2.substring(k + 1);
                    break;

                default:
                    args[count++] = BytecodeStack.KIND_OBJ;
                    s2 = s2.substring(s2.indexOf(';') + 1);
            }
        }

        while (count > 0) {
            stack.pop(args[--count]);
        }
    }

    @Override
    public void visitJumpInsn(int i, Label label) {
        switch (i) {
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
                stack.pop(BytecodeStack.KIND_INT);
                break;

            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
                stack.pop(BytecodeStack.KIND_INT);
                stack.pop(BytecodeStack.KIND_INT);
                break;

            case IF_ACMPEQ:
            case IF_ACMPNE:
                stack.pop(BytecodeStack.KIND_OBJ);
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case IFNULL:
            case IFNONNULL:
                stack.pop(BytecodeStack.KIND_OBJ);
                break;

            case GOTO:
                break;
        }

        jumpToLabel(i, label);
        super.visitJumpInsn(i, label);
    }

    @Override
    public void visitLabel(Label label) {
        comeToLabel(label);
        super.visitLabel(label);
    }

    @Override
    public void visitLdcInsn(Object o) {
        if (o instanceof Integer || o instanceof Boolean || o instanceof Short || o instanceof Character || o instanceof Byte)
            stack.push(BytecodeStack.KIND_INT);
        else if (o instanceof Float)
            stack.push(BytecodeStack.KIND_FLOAT);
        else if (o instanceof Double)
            stack.push(BytecodeStack.KIND_DOUBLE);
        else if (o instanceof Long)
            stack.push(BytecodeStack.KIND_LONG);
        else if (o instanceof String)
            stack.push(BytecodeStack.KIND_OBJ);
        else
            throw new RuntimeException("Unrecognized operation");

        super.visitLdcInsn(o);
    }

    @Override
    public void visitIincInsn(int i, int i1) {
        super.visitIincInsn(i, i1);
    }

    @Override
    public void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
        super.visitTableSwitchInsn(i, i1, label, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
        super.visitLookupSwitchInsn(label, ints, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String s, int i) {
        for (int ii = i; ii > 0; --ii)
            stack.pop(BytecodeStack.KIND_INT);
        stack.push(BytecodeStack.KIND_OBJ);
        super.visitMultiANewArrayInsn(s, i);
    }

    public void startExceptionBlock() {
        stack.push(BytecodeStack.KIND_OBJ);
    }
}
