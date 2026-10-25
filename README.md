# WindowsLib - Feature-Rich Library & Data Generation API

WindowsLib is a powerful utility library for Minecraft mod development, headlined by its flagship feature: AutoDataGen.While the library offers a wide range of developer tools, AutoDataGen completely revolutionizes how you handle Data Generation by eliminating boilerplate code entirely.

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

## 🚀 Quick Start - Registering the Provider

Invoke the registration method within your data generation initialization logic by passing your mod's package path.

```java
// Register the entire data generation feature with just one line
WindowsLib.register("com.example.yourmod");
```

## 🌟 Key Feature: AutoDataGen

AutoDataGen allows you to fully register and generate data using just a single line of code and a simple annotation.

### Using the Annotation

Simply attach the `@AutoTag` annotation to the registry objects (Items, Blocks, etc.) you want to generate tags for.

```java
@AutoTag(name = "example_item", tags = {"minecraft:planks", "yourmod:custom_tag"})
public static final RegistryObject<Item> EXAMPLE_ITEM = ...;
```

## 📡 SimplePacket API

SimplePacket simplifies network packet registration and handling between client and server using `@AutoPacket`. Boilerplate payload registration is handled automatically.

### 🎯 TargetSide Options

When annotating a packet record with `@AutoPacket(TargetSide)`, specify the intended receiving side:

| TargetSide | Description |
| ----- | ----- |
| **`TargetSide.SERVER`** | Sent from client to be handled on the logical server side. |
| **`TargetSide.CLIENT`** | Sent from server to be handled on the logical client side. |
| **`TargetSide.BOTH`** | Registered on both sides for bidirectional packet handling. |

### Creating a Packet

Implement `SimplePacket` and annotate the record with `@AutoPacket`:

```java
@AutoPacket(TargetSide.SERVER) // Sending packets to the server
public record ExamplePacket(String name, int age) implements SimplePacket {
    public static final Type<ExamplePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExampleMod.MODID, "example_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExamplePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ExamplePacket::name,
            ByteBufCodecs.VAR_INT,
            ExamplePacket::age,
            ExamplePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            // Write the processing to be performed on the target side
        });
    }
}
```

### Sending a Packet

Use standard NeoForge client distributor methods:

```java
// ON THE CLIENT

// Send payload to server
ClientPacketDistributor.sendToServer(new ExamplePacket(...));

// ON THE SERVER

// Send to one player (ServerPlayer serverPlayer)
        PacketDistributor.sendToPlayer(serverPlayer, new ExamplePacket(...));

/// Send to all players tracking this chunk (ServerLevel serverLevel, ChunkPos chunkPos)
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos, new ExamplePacket(...));

/// Send to all connected players
        PacketDistributor.sendToAllPlayers(new ExamplePacket(...));
```

### 📦 Working with List Data (`CodecUtils`)

When sending packets that contain collections like `List<V>`, you can use `CodecUtils.makeCodecList` to generate a `StreamCodec` for lists effortlessly, avoiding manual collection codec boilerplate.

#### Example Usage in Packet
```java
@AutoPacket(TargetSide.SERVER)
public record SyncItemListPacket(List<ItemStack> items) implements SimplePacket {
  public static final Type<SyncItemListPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ExampleMod.MODID, "sync_item_list"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemListPacket> STREAM_CODEC = StreamCodec.composite(
          // Use CodecUtils to create a List StreamCodec from single element codec
          CodecUtils.makeCodecList(ItemStack.OPTIONAL_STREAM_CODEC),
          SyncItemListPacket::items,
          SyncItemListPacket::new
  );

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      // Process the received List<ItemStack>
    });
  }
}
```

### 💡 Key Points Explanation
1. **Batch Generation of List Codecs from Individual Element Codecs**:
   By simply passing individual element codecs such as `ItemStack.OPTIONAL_STREAM_CODEC` or `ByteBufCodecs.VAR_INT` to `CodecUtils.makeCodecList(...)`, you can safely convert them into `StreamCodecs` for `List<V>`.
2. **Upper Size Specification (`maxSize`)**:
   By default, an upper limit of 256 elements is set to prevent buffer overflow attacks over the network and excessive memory consumption. If the number of elements increases, you can change the upper limit with `makeCodecList(codec, customMaxSize)`.

## 🖥️ SimpleGUI API

SimpleGUI provides a modular component-based framework for creating custom GUIs and Tabbed Screen interfaces with synchronized menus.

### 🧩 UI Component List

SimpleGUI comes with a set of pre-built `IUIComponent` classes to quickly construct rich, interactive interfaces:

| Component | Description | Main Constructor Parameters / Usage | 
| ----- | ----- | ----- | 
| **`UITabBar`** | Tab navigation header for switching between active UI states | `.addTab(new TabItem(title, icon, onClick))` | 
| **`UILabel`** | Simple text label for titles and descriptions | `new UILabel(Component text, int color)` | 
| **`UITextButton`** | Customizable text button with hover/active state colors | `new UITextButton(Component text, int normalColor, int hoverColor, Runnable action)` | 
| **`UITextBox`** | Input field for text entry and user typing | `new UITextBox(String placeholderText)` | 
| **`UICheckbox`** | Toggle button for boolean options | `new UICheckbox(String label, boolean initialValue, Consumer<Boolean> onChange)` | 
| **`UISlider`** | Numeric slider for values (0.0 to 1.0 range) | `new UISlider(String label, double initialVal, Consumer<Double> onChange)` | 
| **`UIDropDown`** | Selectable drop-down list for options | `new UIDropDown(List<String> options, Consumer<String> onSelect)` | 
| **`UIProgressBar`** | Animated progress bar (supports HORIZONTAL_GROW and VERTICAL_SHRINK) | `new UIProgressBar(Identifier texture, int width, int height, Supplier<Integer> progress, BooleanSupplier isCrafting, Direction direction)` | 
| **`UITankBar`** | Vertical liquid or power level gauge | `new UITankBar(Supplier<Float> fillPercentage)` | 
| **`UISlotBackground`** | Decorative slot background texture for container slots | `new UISlotBackground(@Nullable Identifier slotTexture)` | 

### Complete Example: Tabbed Screen & Menu Setup

Below is a full example showing how to build a tabbed GUI screen with custom UI components, toggleable slots, and menu synchronization.

#### 1. Menu Implementation (`BaseMenu`)

```java
public class ExampleMenu extends BaseMenu<BlockEntity> {

    public ExampleMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        super(ModMenuTypes.EXAMPLE_MENU.get(), containerId, inventory, extraData, 2);
    }

    public ExampleMenu(int containerId, Inventory inventory, BlockEntity blockEntity, int containerSlotsSize) {
        super(ModMenuTypes.EXAMPLE_MENU.get(), containerId, inventory, blockEntity, containerSlotsSize, true);
    }

    @Override
    public void addContainers(ItemStacksResourceHandler handler) {
        // Register container slots mapped to specific tab indices
        this.addNormalSlot(handler, 115, 52, 0);
        this.addResultSlot(handler, 155, 52, 1);
    }
}
```

#### 2. Screen Implementation (`BaseScreen`)

```java
public class ExampleScreen extends BaseScreen<ExampleMenu> {

    private static final Identifier ARROW_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/sprites/container/furnace/lit_progress.png");

    public ExampleScreen(ExampleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        int startX = this.leftPos;
        int startY = this.topPos;

        // 1. Tab Bar Initialization
        var tabBar = new UITabBar();
        tabBar.setPosition(startX, startY - 26);
        tabBar.addTab(new TabItem(Component.literal("Tab 1"), null, () -> switchTab(0)));
        tabBar.addTab(new TabItem(Component.literal("Tab 2"), null, () -> switchTab(1)));
        tabBar.setSize(32, 24);

        addComponentToTab(0, tabBar);
        addComponentToTab(1, tabBar);

        // 2. Tab 0 Components
        var titleLabel = new UILabel(Component.literal("UI Component Lab"), 0x404040FF);
        titleLabel.setPosition(startX + 8, startY + 6);
        addComponentToTab(0, titleLabel);

        var textBox = new UITextBox("Type here...");
        textBox.setPosition(startX + 10, startY + 20);
        textBox.setSize(90, 14);
        addComponentToTab(0, textBox);

        var checkbox = new UICheckbox("Enable Feature", true, (state) -> {
            System.out.println("Checkbox status: " + state);
        });
        checkbox.setPosition(startX + 10, startY + 38);
        addComponentToTab(0, checkbox);

        var button = new UITextButton(Component.literal("Save"), 0x404040FF, 0x202020FF, () -> {
            System.out.println("Save button clicked!");
        });
        button.setPosition(startX + 10, startY + 54);
        button.setSize(90, 12);
        addComponentToTab(0, button);

        // 3. Tab 1 Components
        var slider = new UISlider("Power Limit", 0.75, (val) -> {
            System.out.println(String.format("Value: %.2f%%", val * 100));
        });
        slider.setPosition(startX + 10, startY + 54);
        slider.setSize(90, 12);
        addComponentToTab(1, slider);

        var dropdown = new UIDropDown(
                List.of("Mode: Normal", "Mode: Fast", "Mode: Eco"),
                (selected) -> System.out.println("Selected mode: " + selected)
        );
        dropdown.setPosition(startX + 10, startY + 70);
        dropdown.setSize(90, 14);
        addComponentToTab(1, dropdown);

        var progressBar = new UIProgressBar(
                ARROW_TEXTURE, 24, 16,
                () -> 16,
                () -> true,
                UIProgressBar.Direction.HORIZONTAL_GROW
        );
        progressBar.setPosition(startX + 115, startY + 28);
        addComponentToTab(1, progressBar);

        var tankBar = new UITankBar(() -> 0.70f);
        tankBar.setPosition(startX + 148, startY + 20);
        tankBar.setSize(16, 50);
        addComponentToTab(1, tankBar);

        // Default initial tab
        switchTab(0);
    }

    @Override
    protected void initTabsAndComponents() {
        super.initTabsAndComponents();

        int startX = this.leftPos;
        int startY = this.topPos;

        // Map slot background frames to respective menu slots by tab ID
        var bg1 = new UISlotBackground();
        bg1.setPosition(startX + 115, startY + 52);
        addSlotToTab(getMenu().getToggleableSlot(0), bg1);

        var bg2 = new UISlotBackground();
        bg2.setPosition(startX + 155, startY + 52);
        addSlotToTab(getMenu().getToggleableSlot(1), bg2);
    }
}
```

#### 3. Registering the Screen

Register the screen on the client event bus during client setup:

```java
public void registerScreens(RegisterMenuScreensEvent event) {
    event.register(ModMenuTypes.EXAMPLE_MENU.get(), ExampleScreen::new);
}
```

## 🛠️ Advanced Developer Utilities

Beyond AutoDataGen, this library provides powerful utilities to handle complex Java types, typed references, and entity lookups seamlessly.

### 🔗 Lightweight Entity Reference System (`Ref`)

The `ref` package provides a typed, memory-safe mechanism for referencing entities via `UUID` without retaining heavy object instances in memory.

#### Component Breakdown

| Class | Type | Description | Key Usage |
| ----- | ----- | ----- | ----- |
| **`EntityRef<T>`** | Wrapper Class | Typed, lightweight reference to an `Entity` identified by `UUID`. Safe across unloads. | `new EntityRef<Cow>(entityUuid)` |
| **`PlayerRef`** | Wrapper Class | Typed, lightweight reference to a Player identified by UUID. Not available for offline players. | `new PlayerRef(playerUuid)` |
| **`EntityRefManager`** | Manager Utility | Backend resolver that fetches entity instances from `ServerLevel` using stored UUIDs. | `EntityRefManager.resolve(uuid, level)` |

#### Example Usage

```java
// Create a lightweight, typed reference using an Entity's UUID
UUID entityUuid = cowEntity.getUUID();
EntityRef<Cow> cowRef = new EntityRef<>(entityUuid);

// Resolve the actual entity safely on the server level when needed
Cow cow = cowRef.resolve(serverLevel);
if (cow != null) {
    // Perform operations on the resolved entity
    cow.setCustomName(Component.literal("Target Cow"));
}
```

## 🔍 Bidirectional Lookup System

Includes a highly convenient Lookup utility that allows you to perform reverse-lookups from code objects or raw strings effortlessly.

## ⚙️ Generic Type Converters

Provides flexible, generic-safe type conversion utilities. For example, easily converting specific List implementations into Arrays while maintaining type safety:

```java
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

# 👥 For Players

* Supported Environments:

  * Minecraft Version: 26.1+

  * Mod Loader: \[NeoForge / Fabric\]

* How to Install:

  1. Ensure you have the correct Mod Loader installed.

  2. Download this mod and place the .jar file into your mods folder.

  3. Run the game alongside the mods that require this API.

# 📄 License

This API is available under the MIT License. Feel free to bundle it in your modpacks or build your own mods on top of it.