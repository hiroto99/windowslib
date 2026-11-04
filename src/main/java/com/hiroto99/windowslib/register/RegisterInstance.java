package com.hiroto99.windowslib.register;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RegisterInstance<T> {
    ResourceKey<? extends Registry<T>> key;
    String namespace;

    private RegisterInstance(ResourceKey<? extends Registry<T>> key, String namespace) {
        this.key = key;
        this.namespace = namespace;
    }

    public static <T> RegisterInstance<T> create(ResourceKey<? extends Registry<T>> key, String namespace) {
        return new RegisterInstance<>(key, namespace);
    }

    public ResourceKey<? extends Registry<T>> key() {
        return key;
    }

    public String namespace() {
        return namespace;
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        return UnifiedRegister.register(this, name, supplier);
    }

    public void register(IEventBus eventBus) {
        UnifiedRegister.register(this, eventBus);
    }
}
