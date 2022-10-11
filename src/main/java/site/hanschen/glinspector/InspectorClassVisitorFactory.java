package site.hanschen.glinspector;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public abstract class InspectorClassVisitorFactory implements AsmClassVisitorFactory<InstrumentationParameters.None> {

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor nextClassVisitor) {
        return new ClassVisitor(Opcodes.ASM7, nextClassVisitor) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new AdviceAdapter(Opcodes.ASM7, visitor, access, name, descriptor) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (shouldInstrument(opcode, owner, name, descriptor, isInterface)) {
                            System.out.println("[transform] " + classContext.getCurrentClassData().getClassName() + "#" + name + descriptor);

                            Type methodType = Type.getMethodType(descriptor);
                            Type[] types = methodType.getArgumentTypes();
                            int[] parameters = new int[types.length];
                            for (int i = types.length - 1; i >= 0; i--) {
                                Type type = types[i];
                                parameters[i] = newLocal(type);
                                int storeCode = type.getOpcode(Opcodes.ISTORE);
                                mv.visitVarInsn(storeCode, parameters[i]);
                            }

                            for (int i = 0; i < types.length; i++) {
                                Type type = types[i];
                                int opcode1 = type.getOpcode(Opcodes.ILOAD);
                                mv.visitVarInsn(opcode1, parameters[i]);
                            }
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

                            int array = newLocal(Type.getType(Object[].class));
                            mv.visitIntInsn(BIPUSH, types.length);
                            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                            mv.visitVarInsn(ASTORE, array);
                            for (int i = 0; i < types.length; i++) {
                                mv.visitVarInsn(ALOAD, array);
                                mv.visitIntInsn(BIPUSH, i);
                                Type type = types[i];
                                int loadCode = type.getOpcode(Opcodes.ILOAD);
                                mv.visitVarInsn(loadCode, parameters[i]);
                                boxBasicValue(this, type);
                                mv.visitInsn(AASTORE);
                            }
                            mv.visitLdcInsn(owner);
                            mv.visitLdcInsn(name);
                            mv.visitVarInsn(ALOAD, array);
                            mv.visitMethodInsn(
                                    AdviceAdapter.INVOKESTATIC, "com/flyme/renderfilter/glInspector/GLInspector", "logAndCheck",
                                    "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", false
                            );
                        } else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    }
                };
            }
        };
    }

    private static boolean shouldInstrument(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        return ("android/opengl/GLES20".equals(owner) ||
                "android/opengl/GLES30".equals(owner) ||
                "android/opengl/GLES31".equals(owner) ||
                "android/opengl/GLUtils".equals(owner)) && !"glGetError".equals(name);
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return true;
    }

    private static void boxBasicValue(MethodVisitor mv, Type type) {
        if (Type.INT_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
            );
        } else if (Type.CHAR_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false
            );
        } else if (Type.BYTE_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false
            );
        } else if (Type.BOOLEAN_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
            );
        } else if (Type.SHORT_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false
            );
        } else if (Type.FLOAT_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
            );
        } else if (Type.LONG_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
            );
        } else if (Type.DOUBLE_TYPE.equals(type)) {
            mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
            );
        }
    }
}