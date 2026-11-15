package com.hiroto99.windowslib;

import com.hiroto99.windowslib.core.lifecycle.EntityLifecycleHooks;
import com.hiroto99.windowslib.core.simplecommand.CommandManager;
import com.hiroto99.windowslib.core.simplepacket.PacketManager;
import com.hiroto99.windowslib.modify.ModifyRegister;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber
public class ModEvents {
    @SubscribeEvent
    private static void onEntityLoaded(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        EntityLifecycleHooks.onEntityLoaded(entity);
    }

    @SubscribeEvent
    private static void onEntityUnLoaded(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        EntityLifecycleHooks.onEntityUnloaded(entity);
    }

    public static void onRegisterEvent(RegisterEvent event) {
        ModifyRegister.registerAll();
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // 💡ここで PacketManager.registerAll を呼び出して一括登録を実行する！
        // 第2引数にはご自身のModのチャンネルネームスペース（ModIDなど）を指定します
        PacketManager.registerAll(event);
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // ここでコマンドDispatcherにコマンドを登録する
        CommandManager.registerAll(event.getDispatcher());
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModEvents::onRegisterEvent);
        eventBus.addListener(ModEvents::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(ModEvents::onRegisterCommands);
    }
}
