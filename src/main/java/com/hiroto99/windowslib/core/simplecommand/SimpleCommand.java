package com.hiroto99.windowslib.core.simplecommand;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public interface SimpleCommand {
    // 各パケットで共通のハンドリング処理を強制、またはオプション化
    int execute(CommandContext<CommandSourceStack> context);
}