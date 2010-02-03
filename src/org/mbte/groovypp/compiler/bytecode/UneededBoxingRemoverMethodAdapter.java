package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.mbte.groovypp.runtime.DefaultGroovyPPMethods;

import java.math.BigDecimal;
import java.math.BigInteger;

public class UneededBoxingRemoverMethodAdapter extends StackAwareMethodAdapter implements Opcodes {
    private String boxingDesc = null;
    private static final String DTT  = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());
    private static final String DGPP = BytecodeHelper.getClassInternalName(DefaultGroovyPPMethods.class.getName());

    public UneededBoxingRemoverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropBoxing () {
        if (boxingDesc != null) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, DGPP, "box", boxingDesc);
            boxingDesc = null;
        }
    }

    public void visitInsn(int opcode) {
        if (boxingDesc != null && (opcode == POP || opcode == POP2)) {
            boxingDesc = null;
        }

        dropBoxing ();
        super.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
        dropBoxing ();
        super.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(int opcode, int var) {
        dropBoxing ();
        super.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(int opcode, String desc) {
        if (opcode == CHECKCAST && desc.equals("java/lang/Object"))
           return;

        dropBoxing ();
        super.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropBoxing ();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (boxing(opcode,owner,name)) {
            dropBoxing();
            boxingDesc = desc;
        }
        else {
          if (unboxing(opcode, owner, name)) {
              if (boxingDesc != null) {
                String type = name.substring(0, name.indexOf("Unbox"));
                type = ClassHelper.getWrapper(ClassHelper.make(type)).getNameWithoutPackage();
                String boxingName = boxingDesc.substring(boxingDesc.lastIndexOf('/')+1, boxingDesc.length()-1);
                if (boxingName.equals(type)) {
                    boxingDesc = null;
                }
                else {
                    dropBoxing();
                    super.visitMethodInsn(opcode, owner, name, desc);
                }
              }
              else
                super.visitMethodInsn(opcode, owner, name, desc);
          }
          else {
            dropBoxing();
            super.visitMethodInsn(opcode, owner, name, desc);
          }
        }
    }

    private boolean boxing(int opcode, String owner, String name) {
        return opcode == Opcodes.INVOKESTATIC && owner.equals(DGPP) && name.equals("box");
    }

    private boolean unboxing(int opcode, String owner, String name) {
        return opcode == Opcodes.INVOKESTATIC && owner.equals(DTT) && name.endsWith("Unbox");
    }

    public void visitJumpInsn(int opcode, Label label) {
        dropBoxing ();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        dropBoxing ();
        super.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        dropBoxing ();
        if (cst instanceof Integer) {
            Integer value = (Integer) cst;
            switch (value) {
                case -1:
                    super.visitInsn(Opcodes.ICONST_M1);
                    break;
                case 0:
                    super.visitInsn(Opcodes.ICONST_0);
                    break;
                case 1:
                    super.visitInsn(Opcodes.ICONST_1);
                    break;
                case 2:
                    super.visitInsn(Opcodes.ICONST_2);
                    break;
                case 3:
                    super.visitInsn(Opcodes.ICONST_3);
                    break;
                case 4:
                    super.visitInsn(Opcodes.ICONST_4);
                    break;
                case 5:
                    super.visitInsn(Opcodes.ICONST_5);
                    break;
                default:
                    if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                        super.visitIntInsn(Opcodes.BIPUSH, value);
                    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                        super.visitIntInsn(Opcodes.SIPUSH, value);
                    } else {
                        super.visitLdcInsn(Integer.valueOf(value));
                    }
            }
        } else if (cst instanceof BigDecimal) {
            super.visitTypeInsn(NEW, "java/math/BigDecimal");
            super.visitInsn(DUP);
            super.visitLdcInsn(cst.toString());
            super.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (cst instanceof BigInteger) {
            super.visitTypeInsn(NEW, "java/math/BigInteger");
            super.visitInsn(DUP);
            super.visitLdcInsn(cst.toString());
            super.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        }
        else if (cst instanceof Double) {
            Double aDouble = (Double) cst;
            if (aDouble == 1.0d)
                super.visitInsn(DCONST_1);
            else
                super.visitLdcInsn(cst);
        }
        else if (cst instanceof Long) {
            Long aLong = (Long) cst;
            if (aLong == 0L)
                super.visitInsn(LCONST_0);
            else
                if (aLong == 1L)
                    super.visitInsn(LCONST_1);
                else
                    super.visitLdcInsn(cst);
        }
        else if (cst instanceof Float) {
            Float aFloat = (Float) cst;
            if (aFloat == 1.0f)
                super.visitInsn(FCONST_1);
            else
                if (aFloat == 2.0f)
                    super.visitInsn(FCONST_2);
                else
                    super.visitLdcInsn(cst);
        }
        else if (cst == null) {
            super.visitInsn(ACONST_NULL);
        }
        else
            super.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
        dropBoxing ();
        super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
        dropBoxing ();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
        dropBoxing ();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropBoxing ();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropBoxing ();
        super.visitTryCatchBlock(start, end, handler, type);
    }
}