package net.bigyous.gptgodmc.utils;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;

// a middle man to log command output
public class GPTCommandSender implements CommandSender {
    private final ConsoleCommandSender consoleSender;
    private final StringBuilder output = new StringBuilder();

    public GPTCommandSender(ConsoleCommandSender sender) {
        this.consoleSender = sender;
    }

    @Override
    public void sendMessage(@NotNull String message) {
        output.append(message).append("\n");
        consoleSender.sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        for (String message : messages) {
            output.append(message).append("\n");
            consoleSender.sendMessage(message); // Forward each message to the console
        }
    }

    public String getOutput() {
        return output.toString();
    }

    // Override other methods as necessary
    @Override
    public String getName() {
        return "GPTSender";
    }

    @Override
    public boolean isPermissionSet(String name) {
        return consoleSender.isPermissionSet(name);
    }

    @Override
    public boolean hasPermission(String name) {
        return consoleSender.hasPermission(name);
    }

    @Override
    public boolean isOp() {
        return consoleSender.isOp();
    }

    @Override
    public void setOp(boolean value) {
        consoleSender.setOp(value);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return true;
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        return consoleSender.addAttachment(plugin, name, value);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return consoleSender.addAttachment(plugin);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return consoleSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        consoleSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        consoleSender.recalculatePermissions();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return null;
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        output.append(message).append("\n");
        consoleSender.sendMessage(message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        for (String message : messages) {
            output.append(message).append("\n");
            consoleSender.sendMessage(message); // Forward each message to the console
        }
    }

    @Override
    public @NotNull Server getServer() {
        return this.consoleSender.getServer();
    }

    @Override
    public @NotNull Spigot spigot() {
        return consoleSender.spigot();
    }

    @Override
    public @NotNull Component name() {
        return Component.text("GPTCommandSender");
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value,
            int ticks) {
        return this.consoleSender.addAttachment(plugin, name, value, ticks);
    }
}
