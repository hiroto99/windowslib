package com.hiroto99.windowslib.register;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class UnifiedRegister {
    private static final Map<RegisterInstance<?>, DeferredRegister<?>> TYPES = new HashMap<>();

    public static <T, I extends T> DeferredHolder<T, I> register(RegisterInstance<T> instance, String name, Supplier<? extends I> supplier) {
        DeferredRegister<T> TYPE = search(instance);
        return TYPE.register(name, supplier);
    }

    public static void register(RegisterInstance<?> instance, IEventBus eventBus) {
        DeferredRegister<?> TYPE = search(instance);
        TYPE.register(eventBus);
    }

    private static <T> DeferredRegister<T> search(RegisterInstance<T> instance) {
        DeferredRegister<T> TYPE;
        if (TYPES.containsKey(instance)) {
            DeferredRegister<?> TYPETEMP = TYPES.get(instance);
            try {
                TYPE = (DeferredRegister<T>) TYPETEMP;
            } catch (ClassCastException e) {
                System.err.println("Failed to cast DeferredRegister from RegisterInstance: " + TYPETEMP.toString() +
                        " To: " + instance.key().identifier().getPath());
                throw e;
            }
        } else {
            TYPE = DeferredRegister.create(instance.key(), instance.namespace);
            TYPES.put(instance, TYPE);
        }
        return TYPE;
    }

    public static <T extends AbstractContainerMenu> Supplier<MenuType<T>> createMenu(IContainerFactory<T> factory) {
        return () -> IMenuTypeExtension.create(factory);
    }
}
