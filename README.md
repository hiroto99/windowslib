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
