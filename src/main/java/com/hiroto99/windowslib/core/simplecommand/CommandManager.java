package com.hiroto99.windowslib.core.simplecommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class CommandManager {

    // ユーザーが提示してくれた登録用メソッドで使われるセット
    private static final Set<String> packagesToScan = new HashSet<>();

    /**
     * スキャン対象のパッケージパスを動的に追加する
     */
    public static void register(String modPackagePath) {
        packagesToScan.add(modPackagePath);
    }

    /**
     * 登録されたすべてのパッケージを一括スキャンしてNeoForgeに登録する
     */
    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String packagePath : packagesToScan) {
            try {
                // 登録されたパッケージパスごとにReflectionsを生成してスキャン
                Reflections reflections = new Reflections(packagePath);
                var packetClasses = reflections.getTypesAnnotatedWith(AutoCommand.class);

                for (Class<?> clazz : packetClasses) {
                    if (SimpleCommand.class.isAssignableFrom(clazz)) {
                        registerCommand(dispatcher, (Class<? extends SimpleCommand>) clazz);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to scan package for packets: " + packagePath);
                e.printStackTrace();
            }
        }
    }

    private static <T extends SimpleCommand> void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, Class<T> clazz) {
        try {
            var annotation = clazz.getAnnotation(AutoCommand.class);
            var constructor = clazz.getConstructor().newInstance();
            var method = clazz.getMethod("execute", CommandContext.class);

            dispatcher.register(
                    Commands.literal(annotation.commandLiteral())
                            .executes(context -> {
                                try {
                                    return (int) method.invoke(constructor, context);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                } catch (InvocationTargetException e) {
                                    // 💡 重要: execute() の内部処理でエラー（例外）が発生した場合は、ここにラップされて飛んできます
                                    System.err.println("executeメソッドの内部で例外が発生しました。");
                                    throw new RuntimeException(e);
                                }
                            })
            );
        } catch (Exception e) {
            System.err.println("Failed to register packet: " + clazz.getName());
            e.printStackTrace();
        }
    }
}
