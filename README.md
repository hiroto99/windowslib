# WindowsLib - Feature-Rich Library & Data Generation API
WindowsLib is a powerful utility library for Minecraft mod development, headlined by its flagship feature: AutoDataGen. While the library offers a wide range of developer tools, AutoDataGen complete the process from annotation to generated data registration, reducing boilerplate and keeping your mod code cleaner.

For players, this mod does not add any content on its own but is required as a dependency for other mods that utilize this API.

# 💻 For Developers & Setup
## ⚠️ Important: Development Environment Setup
To use this API within your development environment (IDE), you must append specific arguments to your Run Configurations / JVM Arguments. Otherwise, the library will not be recognized properly.
Please add the following arguments to your Gradle run configurations (e.g., runClient, runServer, runData) in your build.gradle:

```
// Example for build.gradle (Forge / NeoForge / Fabric run configurations)
runs {
    client {
        // Append these arguments to your existing run configuration
        args '--mod', 'windowslib'
    }
    server {
        args '--mod', 'windowslib'
    }
    data {
        args '--mod', 'windowslib'
    }
}
```

## 🌟 Key Feature: AutoDataGen
AutoDataGen allows you to fully register and generate data using just a single line of code and a simple annotation.

## 🚀 Quick Start
### 1. Registering the Provider
Invoke the registration method within your data generation initialization logic by passing your mod's package path.

```
// Register the entire data generation feature with just one line
AutoDataGenProvider.register("com.example.yourmod");
```

### 2. Using the Annotation
Simply attach the ```@AutoTag``` annotation to the registry objects (Items, Blocks, etc.) you want to generate tags for.

```
@AutoTag(name = "example_item", tags = {"minecraft:planks", "yourmod:custom_tag"})
public static final RegistryObject<Item> EXAMPLE_ITEM = ...;
```

## 🧩 SimpleGUI Usage
WindowsLib includes a lightweight GUI system built around `BaseMenu` and `BaseScreen`.
This is useful when you want to create modular, tab-based inventory screens without writing a full custom GUI framework from scratch.

### 1. Create a menu
Extend `BaseMenu` and define the slots that belong to your machine or interface.

```java
package com.example.yourmod.gui;

import com.hiroto99.windowslib.core.simplegui.BaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class ExampleMenu extends BaseMenu<ExampleBlockEntity> {
    public ExampleMenu(int containerId, Inventory playerInventory, ExampleBlockEntity blockEntity) {
        super(null, containerId, playerInventory, blockEntity, 9, true);
    }

    @Override
    public void addContainers(ItemStacksResourceHandler handler) {
        addNormalSlot(handler, 30, 20, 0);
        addNormalSlot(handler, 50, 20, 0);
        addResultSlot(handler, 110, 20, 0);
    }
}
```

### 2. Create a screen
Extend `BaseScreen` and register GUI components for each tab.

```java
package com.example.yourmod.gui;

import com.hiroto99.windowslib.core.simplegui.BaseScreen;
import com.hiroto99.windowslib.core.simplegui.component.UILabel;
import com.hiroto99.windowslib.core.simplegui.component.UIButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExampleScreen extends BaseScreen<ExampleMenu> {
    public ExampleScreen(ExampleMenu menu, Inventory playerInventory) {
        super(menu, playerInventory, Component.literal("Example GUI"));
    }

    @Override
    protected void initTabsAndComponents() {
        addComponent(new UILabel(20, 15, "Tab 1"));
        addComponent(new UIButton(10, 40, 80, 20, "Do Action", () -> {
            // button action here
        }));

        addComponentToTab(0, new UILabel(100, 20, "Item Input"));
        addComponentToTab(1, new UILabel(100, 20, "Other Tab"));
    }
}
```

### 3. Switch tabs
The `BaseScreen` class supports multiple tabs with `switchTab(int tabId)`.

```java
screen.switchTab(1);
```

The `BaseMenu.ToggleableSlot` system makes slots visible only for the currently active tab, and `SwitchTabPacket` syncs the selected tab to the server side.

### 4. Available GUI components
The package `com.hiroto99.windowslib.core.simplegui.component` provides UI elements such as:

- UIButton
- UILabel
- UICheckbox
- UIDropDown
- UISlider
- UIProgressBar
- UITextBox
- UITabBar
- UISlotBackground

This allows you to build a clean menu UI quickly.

## 🔌 SimplePacket Usage
WindowsLib also includes a simple packet registration system for NeoForge.
Instead of manually registering each payload, you define a packet class with `@AutoPacket` and let `PacketManager` scan and register it automatically.

### 1. Define a packet

```java
package com.example.yourmod.network;

import com.hiroto99.windowslib.core.simplepacket.AutoPacket;
import com.hiroto99.windowslib.core.simplepacket.SimplePacket;
import com.hiroto99.windowslib.core.simplepacket.TargetSide;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@AutoPacket(TargetSide.SERVER)
public record ExamplePacket(int amount) implements SimplePacket {
    public static final Type<ExamplePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("yourmod", "example_packet"));

    public static final StreamCodec<FriendlyByteBuf, ExamplePacket> STREAM_CODEC = StreamCodec.composite(
            FriendlyByteBuf::readInt,
            ExamplePacket::new,
            ExamplePacket::encode
    );

    public ExamplePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // server-side processing
        });
    }
}
```

### 2. Register the package
Call `PacketManager.register` with your mod package path.

```java
import com.hiroto99.windowslib.core.simplepacket.PacketManager;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class ModNetworking {
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PacketManager.register("com.example.yourmod");
        PacketManager.registerAll(event);
    }
}
```

### 3. Send a packet

```java
ClientPacketDistributor.sendToServer(new ExamplePacket(42));
```

If the packet is marked with `TargetSide.SERVER`, the payload is registered as a server-bound packet. If it is `TargetSide.CLIENT`, it is sent to the client.

### 4. Common pattern
This is the usual structure for a packet:

- `@AutoPacket(TargetSide.SERVER)` or `@AutoPacket(TargetSide.CLIENT)`
- `TYPE` field
- `STREAM_CODEC` field
- `handle(IPayloadContext context)` implementation
- `PacketManager.register("your.mod.package")`

This keeps packet setup compact and easy to maintain.

## 🛠️ Advanced Developer Utilities
Beyond AutoDataGen, this library provides powerful utilities to handle complex Java types and references seamlessly.
## 🔍 Bidirectional Lookup System
Includes a highly convenient Lookup utility that allows you to perform reverse-lookups from code objects or raw strings effortlessly.
## ⚙️ Generic Type Converters
Provides flexible, generic-safe type conversion utilities. For example, easily converting specific List implementations into Arrays while maintaining type safety:

```
public class ListToArray<T, U extends List<T>> {
    public T[] of(U list) {
        T[] array = (T[]) new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
```

## 👥 For Players
- Supported Environments:
  - Minecraft Version: 26.1+
  - Mod Loader: [NeoForge / Fabric]

- How to Install:
  1. Ensure you have the correct Mod Loader installed.
  2. Download this mod and place the .jar file into your mods folder.
  3. Run the game alongside the mods that require this API.
## 📄 License
This API is available under the MIT License. Feel free to bundle it in your modpacks or build your own mods on top of it.
