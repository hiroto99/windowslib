package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.modify.network.ClientModify;
import net.minecraft.resources.Identifier;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class ModifyRegister {

    // ユーザーが提示してくれた登録用メソッドで使われるセット
    private static final Set<String> packagesToScan = new HashSet<>();

    /**
     * スキャン対象のパッケージパスを動的に追加する
     */
    public static void register(String modPackagePath) {
        packagesToScan.add(modPackagePath);
    }

    /**
     * 登録されたすべてのパッケージを一括スキャンしてModifySystemManagerに登録する
     */
    public static void registerAll() {
        for (String packagePath : packagesToScan) {
            try {
                // 登録されたパッケージパスごとにReflectionsを生成してスキャン
                Reflections reflections = new Reflections(packagePath);
                var modifyClasses = reflections.getSubTypesOf(Modify.class);

                for (Class<? extends Modify> clazz : modifyClasses) {
                    if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface()) {
                        registerModify(clazz);
                    }
                }

                var clientModifyClasses = reflections.getSubTypesOf(ClientModify.class);

                for (Class<? extends ClientModify> clazz : clientModifyClasses) {
                    if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface()) {
                        registerClientModify(clazz);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to scan package for packets: " + packagePath);
                e.printStackTrace();
            }
        }
    }

    private static <T extends Modify> void registerModify(Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var constructor = clazz.getConstructor().newInstance();
        Method method = clazz.getMethod("type");
        ModifySystemManager.registerModify(
                (Identifier) method.invoke(constructor),
                constructor
        );
    }

    private static <T extends ClientModify> void registerClientModify(Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var constructor = clazz.getConstructor().newInstance();
        Method method = clazz.getMethod("type");
        ModifySystemManager.registerModifyClient(
                (Identifier) method.invoke(constructor),
                constructor
        );
    }
}
