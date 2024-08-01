package com.nanokoindustries.joinleavewebhooks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static com.nanokoindustries.joinleavewebhooks.ConfigHandler.initConfig;
import static com.nanokoindustries.joinleavewebhooks.Webhook.sendDiscordWebhookRequestThreaded;

@Mod(modid = JoinLeaveWebhooks.MODID, name = JoinLeaveWebhooks.NAME, version = JoinLeaveWebhooks.VERSION, acceptableRemoteVersions = "*")
@Config.LangKey("joinleavewebhooks.config.title")
public class JoinLeaveWebhooks
{
    public static final String MODID = "joinleavewebhooks";
    public static final String NAME = "Join+Leave+ServerState+Chat Discord Webhook";
    public static final String VERSION = "1.0";

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        File configurationFile = initConfig(event);
        logger.info("Configuration file set to: {}", configurationFile);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("NYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        logger.info("[NanokoIndustries] Join+Leave+ServerState+Chat Discord Webhook mod initialised!");

        if (ConfigHandler.Config.DiscordWebhookToken.isEmpty()) {
            logger.error("The webhook URL is empty! Edit your configuration file located in the \"config\" folder in the server files");
        }
    }

    @EventHandler
    public void commandSetup(FMLServerStartingEvent event) {
        event.registerServerCommand(new ConfigHandler.ReloadCommand());
    }

    private static int getFormattedEpochForTimestampMarkdown() {
        return (int) Math.floor((double) Instant.now().toEpochMilli() / 1000);
    }

    private static String cleanEventToString(String eventToString) { // this is so bad :3c
        // net.minecraftforge.fml.common.event.FMLServerStartedEvent@5cbdb3ac -> FMLServerStartedEvent
        String[] splitArray = eventToString.split("@");
        String[] letsGetTheEvent = splitArray[0].split("\\.");

        return letsGetTheEvent[letsGetTheEvent.length - 1];
    }

    private static String formatMessageContentBasedOnLayout(String layoutMessage, Optional<Event> eventDataOptional) {
        String formatted = layoutMessage.replaceAll("%epoch%", Integer.toString(getFormattedEpochForTimestampMarkdown()));
        formatted = formatted.replaceAll("\\\\n", "\n");

        if (eventDataOptional.isPresent()) {
            Event eventData = eventDataOptional.get();
            String eventName = cleanEventToString(eventData.toString());
            logger.info("Got formatted Event: {}", eventName);

            switch (eventName) {
                case "PlayerEvent$PlayerLoggedInEvent":
                    PlayerEvent.PlayerLoggedInEvent playerLoggedInEvent = (PlayerEvent.PlayerLoggedInEvent) eventData;
                    formatted = formatted.replaceAll("%playername%", playerLoggedInEvent.player.getName());
                    break;
                case "PlayerEvent$PlayerLoggedOutEvent":
                    PlayerEvent.PlayerLoggedOutEvent playerLoggedOutEvent = (PlayerEvent.PlayerLoggedOutEvent) eventData;
                    formatted = formatted.replaceAll("%playername%", playerLoggedOutEvent.player.getName());
                    break;
                case "LivingDeathEvent":
                    LivingDeathEvent livingDeathEvent = (LivingDeathEvent) eventData;
                    EntityPlayer deadPlayer = (EntityPlayer) livingDeathEvent.getEntity();
                    String killer = livingDeathEvent.getSource().getDeathMessage(deadPlayer).getFormattedText().replaceAll("§r", "");

                    formatted = formatted.replaceAll("%deathmessage%", killer);
                    break;
                case "ServerChatEvent":
                    ServerChatEvent serverChatEvent = (ServerChatEvent) eventData;
                    EntityPlayer player = serverChatEvent.getPlayer();
                    String message = serverChatEvent.getMessage();

                    formatted = formatted.replaceAll("%playername", player.getName());
                    formatted = formatted.replaceAll("%chatmessage%", message);
                    break;
                default:
            }
        }

        logger.warn(formatted);

        return formatted;
    }

    @EventHandler
    public void started(FMLServerStartedEvent event) {
        if (!ConfigHandler.Config.TriggerOnServerInitialised) return;

        logger.info("Server opened event triggered!");
        sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.ServerInitialisedLayout, Optional.empty()), Optional.empty(), Optional.empty());
    }

    @EventHandler
    public void closure(FMLServerStoppingEvent event) {
        if (!ConfigHandler.Config.TriggerOnServerClosing) return;

        logger.info("Server closing event triggered!");
        sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.ServerClosingLayout, Optional.empty()), Optional.empty(), Optional.empty());
    }

    @Mod.EventBusSubscriber
    public static class Class {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (ConfigHandler.Config.DiscordWebhookToken.isEmpty()) {
                ITextComponent WarningMessage = new TextComponentString(String.format("§2[JoinLeaveWebhooks]§c§l /!\\ Server Configuration Warning!\n§cYou have not provided a Discord webhook URL in the mod's configuration file, any events that trigger a webhook will not succeed until you provide one.\n\n§cThe configuration file is located at: §c§o%s", ConfigHandler.Config.ConfigurationFullLocation));
                event.player.sendMessage(WarningMessage);
            }

            if (!ConfigHandler.Config.TriggerOnPlayerJoin) return;
            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerJoinLayout, Optional.of(event)), Optional.of(true), Optional.of(event.player.getUniqueID()));
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerLeave) return;
            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerLeaveLayout, Optional.of(event)), Optional.of(true), Optional.of(event.player.getUniqueID()));
        }

        @SubscribeEvent
        public static void onPlayerChat(ServerChatEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerChat) return;
            EntityPlayer player = event.getPlayer();

            if (event.getPlayer() != null) {
                sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerChatLayout, Optional.of(event)), Optional.of(true), Optional.of(player.getUniqueID()));
            }
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerDeath) return;
            if (!(event.getEntity() instanceof EntityPlayer)) return;

            EntityPlayer deadPlayer = (EntityPlayer) event.getEntity();
            String killer = event.getSource().getDeathMessage(deadPlayer).getFormattedText().replaceAll("§r", "");

            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerDeathLayout, Optional.of(event)), Optional.of(false), Optional.of(deadPlayer.getUniqueID()));
        }

        @SubscribeEvent
        public static void onPlayerCommandExecute(CommandEvent event) {
            logger.warn(event);
        }
    }
}
