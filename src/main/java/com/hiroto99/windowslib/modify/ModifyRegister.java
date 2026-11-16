package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.core.simplecommand.SimpleCommand;
import com.hiroto99.windowslib.modify.datasaver.SaveableModify;
import net.minecraft.resources.Identifier;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                var packetClasses = reflections.getSubTypesOf(SaveableModify.class);

                for (Class<? extends SaveableModify> clazz : packetClasses) {
                    if (SimpleCommand.class.isAssignableFrom(clazz)) {
                        registerModify(clazz);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to scan package for packets: " + packagePath);
                e.printStackTrace();
            }
        }
    }

    private static <T extends SaveableModify> void registerModify(Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var constructor = clazz.getConstructor().newInstance();
        Method method = clazz.getMethod("type");
        ModifySystemManager.registerModify(
                (Identifier) method.invoke(constructor),
                constructor
        );
    }
}
