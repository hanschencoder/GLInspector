package site.hanschen.glinspector;

import com.android.build.api.instrumentation.*;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.LibraryExtension;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.apache.commons.io.FileUtils;
import org.gradle.api.*;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"UnstableApiUsage", "NullableProblems", "ConstantConditions"})
public class InspectorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        InspectorExtension extension = project.getExtensions().create("glInspector", InspectorExtension.class);

        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                project.getLogger().warn("glInspector is " + (extension.enable ? "enable" : "disable") + ", gl error check by [" + extension.packageName + ".GLInspector]");

                LibraryExtension libraryExtension = project.getExtensions().findByType(LibraryExtension.class);
                libraryExtension.getSourceSets().getByName("main").getJava().srcDir("build/generated/source/glInspector");
                Task task = project.getTasks().create("glInspector");
                task.doFirst(new Action<Task>() {
                    @Override
                    public void execute(Task task) {
                        String classContent = "package " + extension.packageName + ";\n\n" + CLASS_CONTENT;
                        File classFile = new File(project.getBuildDir() + "/generated/source/glInspector/" + extension.packageName.replace(".", "/"), "GLInspector.java");
                        try {
                            FileUtils.writeStringToFile(classFile, classContent, "UTF-8", false);
                        } catch (IOException ignored) {
                        }
                    }
                });
                project.getTasks().withType(JavaCompile.class).all(new Action<JavaCompile>() {
                    @Override
                    public void execute(JavaCompile javaCompile) {
                        javaCompile.dependsOn(task);
                    }
                });
            }
        });

        AndroidComponentsExtension androidComponents = project.getExtensions().getByType(AndroidComponentsExtension.class);
        androidComponents.onVariants(androidComponents.selector().all(), new Action<Variant>() {
            @Override
            public void execute(Variant variant) {
                if (!extension.enable) {
                    return;
                }
                variant.transformClassesWith(InspectorClassVisitorFactory.class,
                        InstrumentationScope.PROJECT, new Function1<InstrumentationParameters.None, Unit>() {
                            @Override
                            public Unit invoke(InstrumentationParameters.None none) {
                                return Unit.INSTANCE;
                            }
                        });
                variant.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS);
            }
        });
    }

    private static final String CLASS_CONTENT = "import android.opengl.GLES20;\n" +
            "import android.util.Log;\n" +
            "\n" +
            "public class GLInspector {\n" +
            "\n" +
            "    private static String TAG = \"GLInspector\";\n" +
            "\n" +
            "    private static boolean sEnableLogcat = false;\n" +
            "    private static boolean sEnableErrorCheck = false;\n" +
            "\n" +
            "    public static void setEnableLogcat(boolean sEnableLogcat) {\n" +
            "        GLInspector.sEnableLogcat = sEnableLogcat;\n" +
            "    }\n" +
            "\n" +
            "    public static void setTag(String tag) {\n" +
            "        GLInspector.TAG = TAG;\n" +
            "    }\n" +
            "\n" +
            "    public static void setEnableErrorCheck(boolean sEnableErrorCheck) {\n" +
            "        GLInspector.sEnableErrorCheck = sEnableErrorCheck;\n" +
            "    }\n" +
            "\n" +
            "    public static void logAndCheck(String owner, String methodName, Object... parameters) {\n" +
            "        if (sEnableLogcat) {\n" +
            "            StringBuilder b = new StringBuilder();\n" +
            "            int iMax = parameters.length - 1;\n" +
            "            if (iMax == -1) {\n" +
            "                b.append(\"()\");\n" +
            "            } else {\n" +
            "                b.append('(');\n" +
            "                for (int i = 0; ; i++) {\n" +
            "                    b.append(parameters[i]);\n" +
            "                    if (i == iMax) {\n" +
            "                        b.append(')');\n" +
            "                        break;\n" +
            "                    }\n" +
            "                    b.append(\", \");\n" +
            "                }\n" +
            "                b.append(\")\");\n" +
            "            }\n" +
            "            Log.v(TAG, owner.replace(\"/\", \".\") + \"#\" + methodName + b);\n" +
            "        }\n" +
            "        if (sEnableErrorCheck) {\n" +
            "            assertNoErrors();\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public static void assertNoErrors() {\n" +
            "        int error = GLES20.glGetError();\n" +
            "        if (error != GLES20.GL_NO_ERROR) {\n" +
            "            String reason;\n" +
            "            switch (error) {\n" +
            "                case GLES20.GL_INVALID_ENUM:\n" +
            "                    reason = \"GL_INVALID_ENUM\";\n" +
            "                    break;\n" +
            "                case GLES20.GL_INVALID_VALUE:\n" +
            "                    reason = \"GL_INVALID_VALUE\";\n" +
            "                    break;\n" +
            "                case GLES20.GL_INVALID_OPERATION:\n" +
            "                    reason = \"GL_INVALID_OPERATION\";\n" +
            "                    break;\n" +
            "                case GLES20.GL_OUT_OF_MEMORY:\n" +
            "                    reason = \"GL_OUT_OF_MEMORY\";\n" +
            "                    break;\n" +
            "                case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:\n" +
            "                    reason = \"GL_INVALID_FRAMEBUFFER_OPERATION\";\n" +
            "                    break;\n" +
            "                default:\n" +
            "                    reason = Integer.toHexString(error);\n" +
            "            }\n" +
            "            Log.e(TAG, \"glError: \" + reason, new Exception());\n" +
            "            throw new IllegalStateException(\"glError: \" + reason);\n" +
            "        }\n" +
            "    }\n" +
            "}\n";
}