package com.hiroto99.windowslib;

import com.hiroto99.windowslib.core.simplecommand.CommandManager;
import com.hiroto99.windowslib.core.simplepacket.PacketManager;
import com.hiroto99.windowslib.datagen.AutoDataGenProvider;
import com.hiroto99.windowslib.worldgen.ModBiomeModifiers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WindowsLib.MODID)
public class WindowsLib {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "windowslib";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WindowsLib(IEventBus modEventBus, ModContainer modContainer) {
        PacketManager.createStreamCodecKey();

        // This is the `register` statement used to make WindowsLib recognize it.
        // It is required in order to use the API's functionality.
        WindowsLib.register("com.hiroto99.windowslib");

        ModBiomeModifiers.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (WindowsLib) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(AutoDataGenProvider::gatherData);

        NeoForge.EVENT_BUS.register(ModEvents.class);
        NeoForge.EVENT_BUS.addListener(ModEvents::onRegisterCommands);
        modEventBus.addListener(ModEvents::onRegisterPayloads);
    }

    public static void register(String modPackagePath) {
        AutoDataGenProvider.register(modPackagePath);
        PacketManager.register(modPackagePath);
        CommandManager.register(modPackagePath);
    }
}
