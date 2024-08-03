package com.nanokoindustries.minecraftwebhook;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class CommandHandler {
    private static final Logger logger = MinecraftWebhook.logger;

    public static TextComponentString createChatMessage(String message) {
        return new TextComponentString(String.format("§2[MinecraftWebhook] %s", message));
    }

    public static class ReloadCommand extends CommandBase {
        @Override
        public String getName() {
            return "mwreload";
        }

        @Override
        public String getUsage(ICommandSender iCommandSender) {
            return "command.mwreload.usage";
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings) throws CommandException {
            logger.info("Reloading Configuration from file...");
            iCommandSender.sendMessage(createChatMessage("§7Reloading configuration from file..."));
            ConfigHandler.populateConfigClass(new File(ConfigHandler.Config.ConfigurationFullLocation));
            iCommandSender.sendMessage(createChatMessage("§7Done!"));
        }
    }
}
