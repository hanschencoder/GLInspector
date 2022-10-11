# 1. GLInspector 简介

通过 Android Gradle Plugin Transform 为所有 Android OpenGL API 调用**自动织入字节码**，实现：
 - 打印所有 GL 调用的函数名及传入参数
 - 所有 GL 调用后都织入 glGetError 检测，让错误提前抛出，精确定位问题

logcat 输出：
```
android.opengl.GLES30#glBindVertexArray(0))
android.opengl.GLES20#glEnable(3089))
android.opengl.GLES20#glUseProgram(48))
android.opengl.GLES20#glUniformMatrix4fv(0, 1, false, [F@ecbe7f6, 0))
android.opengl.GLES20#glActiveTexture(33985))
android.opengl.GLES20#glBindTexture(3553, 26))
android.opengl.GLES20#glTexParameterf(3553, 10241, 9729.0))
android.opengl.GLES20#glTexParameterf(3553, 10240, 9729.0))
android.opengl.GLES20#glTexParameterf(3553, 10242, 10497.0))
android.opengl.GLES20#glTexParameterf(3553, 10243, 10497.0))
android.opengl.GLES20#glUniform1i(7, 1))
android.opengl.GLES20#glUniformMatrix3fv(1, 1, false, [F@5598d4d, 0))
android.opengl.GLES20#glUniform1i(4, 0))
android.opengl.GLES20#glUniform1f(2, 1.0))
android.opengl.GLES20#glUniform1f(5, 1.0))
android.opengl.GLES20#glUniform4f(3, 1.0, 1.0, 1.0, 0.0))
android.opengl.GLES30#glBindVertexArray(4))
android.opengl.GLES20#glBindBuffer(34963, 10))
android.opengl.GLES20#glDrawElements(4, 6, 5123, 0))
```

gl error 堆栈输出：
```
2022-10-11 15:07:51.264 19026-19970/? E/GLInspector: glError: GL_INVALID_FRAMEBUFFER_OPERATION
    java.lang.Exception
        at com.flyme.renderfilter.glInspector.GLInspector.assertNoErrors(GLInspector.java:69)
        at com.flyme.renderfilter.glInspector.GLInspector.logAndCheck(GLInspector.java:42)
        at com.flyme.renderfilter.filter.KawaseBlurFilter.downscaling(KawaseBlurFilter.java:110)
        at com.flyme.renderfilter.filter.KawaseBlurFilter.filterInput(KawaseBlurFilter.java:161)
        at com.flyme.renderfilter.filter.Filter.fire(Filter.java:16)
        at com.flyme.renderfilter.pipeline.FilterPipeline.process(FilterPipeline.java:77)
        at com.flyme.renderfilter.drawable.RenderFilterDrawable$RenderFilterState$1.onDraw(RenderFilterDrawable.java:140)
        at com.flyme.renderfilter.functor.Functor$TraceFunctorCallback.onDraw(Functor.java:149)
        at com.flyme.renderfilter.functor.Functor.lambda$onDraw$0(Functor.java:71)
        at com.flyme.renderfilter.functor.-$$Lambda$Functor$tyH4HulT9foRE0UNWl-K5SN3on4.accept(Unknown Source:4)
        at com.flyme.renderfilter.functor.Functor.dispatchEvent(Functor.java:106)
        at com.flyme.renderfilter.functor.Functor.onDraw(Functor.java:71)

```

# 2. 如何引入

在 build.gradle 文件中添加依赖并应用插件

```groovy
buildscript {
    dependencies {
        // 添加依赖
        classpath 'site.hanschen.glinspector:glinspector:1.0.0'
    }
}

// 应用插件
apply plugin: 'site.hanschen.glinspector'

// 配置插件
glInspector {
    // 织入字节码是否开启，默认为 false 不会对原代码做任何修改
    enable true
    // 配置 GLInspector 的包名，GLInspector 的作用下面会说明
    packageName "com.flyme.renderfilter.glInspector"
}
```

# 3. 如何使用

使用 GLInspector 后，插件会自动插入字节码，如源代码:

```java
...
GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
...
```

织入后代码：

```java
...
GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
// AUTO GENERATE CODE BY GLInspector
GLInspector.logAndCheck("android/opengl/GLES20", "glClearColor", Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(1.0f));
...
```

由于频繁打印会影响性能，glGetError 检测到错误后会主动抛出异常影响崩溃率，所以提供以下两个开关进行控制。其中 GLInspector 是自动生成的类，包名为 glInspector 中配置的包名

```java
GLInspector.setEnableLogcat(enableLogcat);
GLInspector.setEnableErrorCheck(enableErrorCheck);
```

# 4. License

Apache 2.0. See the [LICENSE](./LICENSE) file for details.