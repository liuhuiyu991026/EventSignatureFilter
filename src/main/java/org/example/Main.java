package org.example;

import com.sun.istack.NotNull;
import org.example.asm.JarReader;
import org.example.entity.EventHandlerClass;
import org.example.entity.EventHandlerMethod;
import org.nutz.dao.Dao;
import org.nutz.dao.util.DaoUp;
import org.nutz.lang.Files;
import org.nutz.lang.util.Regex;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.jar.JarFile;
import java.util.stream.Collectors;


public class Main {
    public static final String IS_ANDROID = "^android";
    public static final String IS_LISTENER = ".*Listener$|android/app/Activity$";
    private final static int[] Android_API_Levels = new int[]{19, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
    private static int searching = -1;
    private static BlockingQueue<EventHandlerClass> handlerClasses;
    private static BlockingQueue<EventHandlerMethod> handlerMethods;

    private static String getJarPath(int level) {
        return String.format("android-%d/android.jar", level);
    }

    private static void init() {
        try {
            handlerClasses = new LinkedBlockingDeque<>();
            handlerMethods = new LinkedBlockingDeque<>();
            DaoUp.me().init("db.properties");
            Dao dao = DaoUp.me().dao();
            dao.create(EventHandlerClass.class, true);
            dao.create(EventHandlerMethod.class, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void teardown() {
        DaoUp.me().close();
    }

    private static void resolveClassNode(@NotNull ClassNode cn) {
        resolveClassNode(cn, "android", Integer.toString(searching), EventHandlerClass.Type.SDK);
    }
    private static void resolveClassNode(@NotNull ClassNode cn, String libName, String version, int type) {
        String name = cn.name;
        var cls = new EventHandlerClass(EventHandlerClass.nextId(), name, name.substring(0, name.lastIndexOf("/")),
                cn.superName, type, version, libName);
        handlerClasses.add(cls);
        var methods = cn.methods.stream()
                .filter(m -> m.name.matches("^on.*"))
                .map(m -> new EventHandlerMethod(null, cls.getId(), m.name, m.desc, m.signature, cls))
                .collect(Collectors.toSet());
        handlerMethods.addAll(methods);
    }
    private static void runSearchTask(Runnable search){
        handlerClasses.clear();
        handlerMethods.clear();

        search.run();

        Dao dao = DaoUp.me().dao();
        dao.fastInsert(handlerClasses);
        for (EventHandlerMethod m : handlerMethods) {
            m.setClassID(m.getMaster().getId());
        }
        dao.fastInsert(handlerMethods);
    }
    private static void resolveAndroidSDK() {
        runSearchTask(Main::resolveAndroidSDKInner);
    }
    private static void resolveAndroidSDKInner(){
        try {
            for (int api : Android_API_Levels) {
                searching = api;
                File f = Files.findFile(getJarPath(api));
                JarFile jar = new JarFile(f);
                var classes = JarReader.loadClassNode(jar);
                classes
                        .filter(cn -> cn.name.matches(IS_LISTENER))
                        .forEach(Main::resolveClassNode);
                jar.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void resolveAndroidSupport(){
        runSearchTask(Main::resolveAndroidSupportInner);
    }
    private static void resolveAndroidSupportInner(){
        var support = Files.findFile("support");
        var libNamePrefix = "android-support-";
        try {
            var fs = java.nio.file.Files.walk(support.toPath()).map(Path::toFile).filter(File::isFile);
            var pattern = Regex.getPattern("-\\d\\d?");
            fs.forEach(f -> {
                var name = f.getName();
                name = name.substring(0, name.length()-4);
                var matcher = pattern.matcher(name);
                if(!matcher.find()){
                    throw new IllegalStateException();
                }
                int start = matcher.start();
                var libName = libNamePrefix+name.substring(0, start);
                var version = name.substring(start+1);
                System.out.println(libName + " " + version);
                try {
                    var jar = new JarFile(f);
                    var classes = JarReader.loadClassNode(jar);
                    classes.forEach(cn -> {
                        resolveClassNode(cn, libName, version, EventHandlerClass.Type.LIB);
                    });
                    jar.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        init();
        resolveAndroidSDK();
        resolveAndroidSupport();
        teardown();
    }
}