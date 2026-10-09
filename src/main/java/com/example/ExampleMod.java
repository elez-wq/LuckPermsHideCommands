package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import me.lucko.fabric.api.permissions.v0.Permissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import java.util.function.Predicate;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
            for (CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
                wrapRequirement(node);
            }
        });
    }

    private void wrapRequirement(CommandNode<ServerCommandSource> node) {
        Predicate<ServerCommandSource> originalRequirement = node.getRequirement();
        
        node.setRequirement(source -> {
            if (!source.isExecutedByPlayer()) return originalRequirement.test(source);
            
            String permissionNode = "minecraft.command." + node.getName();
            boolean hasPerm = Permissions.check(source, permissionNode, false);
            
            return hasPerm;
        });

        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            wrapRequirement(child);
        }
    }
}
