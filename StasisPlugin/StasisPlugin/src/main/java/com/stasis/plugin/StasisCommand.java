package com.stasis.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StasisCommand implements CommandExecutor {

    private final StasisPlugin plugin;
    private final StasisManager stasisManager;

    public StasisCommand(StasisPlugin plugin, StasisManager stasisManager) {
        this.plugin = plugin;
        this.stasisManager = stasisManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /stasis!", NamedTextColor.RED));
            return true;
        }

        String[] result = {""};
        boolean success = stasisManager.activateStasis(player, result);

        if (success) {
            player.sendMessage(
                Component.text("✦ ", NamedTextColor.DARK_PURPLE)
                    .append(Component.text(result[0]))
            );
        } else {
            player.sendMessage(Component.text(result[0]));
        }

        return true;
    }
}
