package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Field;
import java.util.function.Predicate;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                Object commandManager = server.getClass().getMethod("getCommandManager").invoke(server);
                CommandDispatcher dispatcher = (CommandDispatcher) commandManager.getClass().getMethod("getDispatcher").invoke(commandManager);
                
                for (Object n : dispatcher.getRoot().getChildren()) {
                    CommandNode node = (CommandNode) n;
                    wrapRequirement(node);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void wrapRequirement(CommandNode node) {
        Predicate originalRequirement = node.getRequirement();
        
        try {
            // Находим скрытое финальное поле requirement в классе CommandNode
            Field requirementField = CommandNode.class.getDeclaredField("requirement");
            requirementField.setAccessible(true); // Взламываем защиту private

            // Создаем наше новое условие видимости
            Predicate customRequirement = source -> {
                try {
                    boolean isPlayer = (boolean) source.getClass().getMethod("isExecutedByPlayer").invoke(source);
                    if (!isPlayer) {
                        return originalRequirement.test(source);
                    }

                    String commandName = node.getName();
                    String permissionNode = "minecraft.command." + commandName;

                    // Динамически запрашиваем LuckPerms через Fabric Permissions API
                    Class<?> permsClass = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
                    java.lang.reflect.Method checkMethod = permsClass.getMethod("check", source.getClass(), String.class, boolean.class);
                    
                    return (boolean) checkMethod.invoke(null, source, permissionNode, false);
                } catch (Exception e) {
                    return originalRequirement.test(source);
                }
            };

            // Принудительно записываем новое условие в поле
            requirementField.set(node, customRequirement);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Повторяем процесс для всех подкоманд
        for (Object child : node.getChildren()) {
            wrapRequirement((CommandNode) child);
        }
    }
}
