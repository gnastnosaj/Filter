package com.github.gnastnosaj.filter.dsl.groovy;

import android.content.Context;
import android.util.Log;
import com.android.dx.Version;
import com.android.dx.dex.DexFormat;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.github.gnastnosaj.filter.dsl.core.Connection;
import com.github.gnastnosaj.filter.dsl.groovy.util.HexUtil;
import dalvik.system.DexClassLoader;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.Script;
import groovyjarjarasm.asm.ClassWriter;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.*;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class GrooidClassLoader extends GroovyClassLoader {
    private final static Attributes.Name CREATED_BY = new Attributes.Name("Created-By");
    private final static String DEX_IN_JAR_NAME = "classes.dex";
    private final static String DYNAMIC_CLASSES = "dynamicClasses";

    private final static DexOptions dexOptions;
    private final static CfOptions cfOptions;

    static {
        dexOptions = new DexOptions();
        dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;

        cfOptions = new CfOptions();
        cfOptions.positionInfo = PositionList.LINES;
        cfOptions.localInfo = true;
        cfOptions.strictNameCheck = true;
        cfOptions.optimize = false;
        cfOptions.optimizeListFile = null;
        cfOptions.dontOptimizeListFile = null;
        cfOptions.statistics = false;
    }

    private GrooidClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config);
    }

    public static GroovyObject loadAndCreateGroovyObject(Context context, String scriptText, String md5) {
        File dynamicClasses = context.getDir(DYNAMIC_CLASSES, Context.MODE_PRIVATE);
        File dex = new File(dynamicClasses, md5 + ".jar");
        Map<String, Class> classes;

        if (dex.exists()) {
            classes = defineDynamic(context, dex);
        } else {
            final Set<String> classNames = new LinkedHashSet<>();
            final DexFile dexFile = new DexFile(dexOptions);

            CompilerConfiguration config = new CompilerConfiguration();
            config.setBytecodePostprocessor((s, bytes) -> {
                ClassDefItem classDefItem = CfTranslator.translate(s + ".class", bytes, cfOptions, dexOptions);
                dexFile.add(classDefItem);
                classNames.add(s);
                return bytes;
            });
            ImportCustomizer customizer = new ImportCustomizer();
            customizer.addStaticStars(Connection.Method.class.getName());
            customizer.addStaticStars(android.util.Base64.class.getName());
            config.addCompilationCustomizers(customizer);

            GrooidClassLoader classLoader = new GrooidClassLoader(context.getApplicationContext().getClassLoader(), config);
            try {
                classLoader.parseClass(scriptText);
            } catch (Throwable throwable) {
                Log.e("GrooidShell", "Dynamic loading failed", throwable);
            }

            byte[] dalvikBytecode = null;
            try {
                dalvikBytecode = dexFile.toDex(new OutputStreamWriter(new ByteArrayOutputStream()), false);
            } catch (IOException e) {
                Log.e("GrooidShell", "Unable to convert to Dalvik", e);
            }
            classes = defineDynamic(context, classNames, dalvikBytecode, dex);
        }

        if (classes != null) {
            for (Class scriptClass : classes.values()) {
                if (Script.class.isAssignableFrom(scriptClass)) {
                    Script script = null;
                    try {
                        script = (Script) scriptClass.newInstance();
                    } catch (InstantiationException e) {
                        Log.e("GroovyDroidShell", "Unable to create script", e);
                    } catch (IllegalAccessException e) {
                        Log.e("GroovyDroidShell", "Unable to create script", e);
                    }
                    return script;
                }
            }
        }
        return null;
    }

    public static EvalResult evaluate(Context context, String scriptText) {
        long sd = System.nanoTime();

        Script script = (Script) loadAndCreateGroovyObject(context, scriptText);
        long compilationTime = System.nanoTime() - sd;

        Object result = null;
        long execTime = 0;

        if (script != null) {
            result = script.run();
            execTime = System.nanoTime() - sd;
        }

        return new EvalResult(compilationTime, execTime, result);
    }

    public static GroovyObject loadAndCreateGroovyObject(Context context, String scriptText) {
        try {
            String md5 = HexUtil.bytesToHex(MessageDigest.getInstance("MD5").digest(scriptText.getBytes()));
            return loadAndCreateGroovyObject(context, scriptText, md5);
        } catch (Throwable throwable) {
            return null;
        }
    }

    @Override
    protected ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {

        InnerLoader loader = AccessController.doPrivileged((PrivilegedAction<InnerLoader>) () -> new InnerLoader(this));

        return new ClassCollector(loader, unit, su) {
            @Override
            protected Class createClass(byte[] code, ClassNode classNode) {
                try {
                    return super.createClass(code, classNode);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected Class onClassNode(ClassWriter classWriter, ClassNode classNode) {
                try {
                    return super.onClassNode(classWriter, classNode);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

    @Override
    public Class loadClass(String name, boolean lookupScriptFiles, boolean preferClassOverScript, boolean resolve) throws CompilationFailedException {
        try {
            return super.loadClass(name, lookupScriptFiles, preferClassOverScript, resolve);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Map<String, Class> defineDynamic(Context context, File dex) {
        Map<String, Class> result = new LinkedHashMap<>();
        try {
            dalvik.system.DexFile dexFile = dalvik.system.DexFile.loadDex(dex.getAbsolutePath(), dex.getAbsolutePath().replace(".jar", ".odex"), 0);
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                result.put(className, dexFile.loadClass(className, context.getApplicationContext().getClassLoader()));
            }
            return result;
        } catch (Throwable throwable) {
            Log.e("DynamicLoading", "Unable to load class", throwable);
        }
        return null;
    }

    private static Map<String, Class> defineDynamic(Context context, Set<String> classNames, File dex) {
        Map<String, Class> result = new LinkedHashMap<>();
        try {
            DexClassLoader loader = new DexClassLoader(dex.getAbsolutePath(), dex.getParent(), null, context.getApplicationContext().getClassLoader());
            for (String className : classNames) {
                result.put(className, loader.loadClass(className));
            }
        } catch (Throwable throwable) {
            Log.e("DynamicLoading", "Unable to load class", throwable);
        }
        return result;
    }

    private static Map<String, Class> defineDynamic(Context context, Set<String> classNames, byte[] dalvikBytecode, File dex) {
        try {
            FileOutputStream fos = new FileOutputStream(dex);
            JarOutputStream jar = new JarOutputStream(fos, makeManifest());
            JarEntry classes = new JarEntry(DEX_IN_JAR_NAME);
            classes.setSize(dalvikBytecode.length);
            jar.putNextEntry(classes);
            jar.write(dalvikBytecode);
            jar.closeEntry();
            jar.finish();
            jar.flush();
            fos.flush();
            fos.close();
            jar.close();

            return defineDynamic(context, classNames, dex);
        } catch (Throwable throwable) {
            Log.e("DynamicLoading", "Unable to load class", throwable);
        }
        return null;
    }

    private static Manifest makeManifest() {
        Manifest manifest = new Manifest();
        Attributes attribs = manifest.getMainAttributes();
        attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attribs.put(CREATED_BY, "dx " + Version.VERSION);
        attribs.putValue("Dex-Location", DEX_IN_JAR_NAME);
        return manifest;
    }

    public static class EvalResult {
        final long compilationTime;
        final long execTime;
        final Object result;

        public EvalResult(long compilationTime, long execTime, Object result) {
            this.compilationTime = compilationTime;
            this.execTime = execTime;
            this.result = result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Compilation time = ").append(compilationTime / 1000000).append("ms");
            sb.append("\n");
            sb.append("Execution time = ").append(execTime / 1000000).append("ms");
            sb.append("\n");
            sb.append("Result = ").append(result);
            return sb.toString();
        }
    }
}