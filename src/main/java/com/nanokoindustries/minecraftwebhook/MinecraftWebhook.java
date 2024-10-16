package com.nanokoindustries.minecraftwebhook;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.nanokoindustries.minecraftwebhook.ConfigHandler.initConfig;
import static com.nanokoindustries.minecraftwebhook.Webhook.sendDiscordWebhookRequestThreaded;

@Mod(modid = MinecraftWebhook.MODID, name = MinecraftWebhook.NAME, version = MinecraftWebhook.VERSION, acceptableRemoteVersions = "*")
@Config.LangKey("minecraftwebhook.config.title")
public class MinecraftWebhook
{
    public static final String MODID = "minecraftwebhook";
    public static final String NAME = "MinecraftWebhook";
    public static final String VERSION = "1.0";

    public static Logger logger;
    public static Boolean currentlyShuttingDown = false;
    public static Map<UUID, Long> playerPlaytimeActivity = new HashMap<UUID, Long>();
    public static Long serverInitTimestamp;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        File configurationFile = initConfig(event);
        logger.info("Configuration file set to: {}", configurationFile);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("MinecraftWebhook initialised!");

        if (ConfigHandler.Config.DiscordWebhookToken.isEmpty()) {
            logger.error("The webhook URL is empty! Edit your configuration file located in the \"config\" folder in the server files");
        }
    }

    @EventHandler
    public void commandSetup(FMLServerStartingEvent event) {
        logger.info("Registering server commands...");
        event.registerServerCommand(new CommandHandler.ReloadCommand());
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
        Long currentTimestamp = Instant.now().toEpochMilli();
        String formatted = layoutMessage.replaceAll("%epoch%", Integer.toString(getFormattedEpochForTimestampMarkdown()));
        formatted = formatted.replaceAll("%serveruptime%", ms.format(currentTimestamp - serverInitTimestamp));
        formatted = formatted.replaceAll("%rawserveruptime%", String.valueOf(currentTimestamp - serverInitTimestamp));
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
                    UUID playerUUID = playerLoggedOutEvent.player.getUniqueID();
                    Long currentSessionPlaytime = playerPlaytimeActivity.get(playerUUID);

                    formatted = formatted.replaceAll("%playername%", playerLoggedOutEvent.player.getName());
                    formatted = formatted.replaceAll("%playtime%", ms.format(currentTimestamp - currentSessionPlaytime));
                    formatted = formatted.replaceAll("%rawplaytime%", String.valueOf(currentTimestamp - currentSessionPlaytime));
                    playerPlaytimeActivity.remove(playerUUID);
                    break;
                case "LivingDeathEvent":
                    LivingDeathEvent livingDeathEvent = (LivingDeathEvent) eventData;
                    EntityPlayer deadPlayer = (EntityPlayer) livingDeathEvent.getEntity();
                    String killer = livingDeathEvent.getSource().getDeathMessage(deadPlayer).getUnformattedText();

                    formatted = formatted.replaceAll("%deathmessage%", killer);
                    break;
                case "ServerChatEvent":
                    ServerChatEvent serverChatEvent = (ServerChatEvent) eventData;
                    EntityPlayer player = serverChatEvent.getPlayer();
                    String message = serverChatEvent.getMessage();
                    String cleanedMessage = message.replaceAll("\n", "\\\\n"); // clean out special chars so players cannot do funny formatting in messages
                    if (ConfigHandler.Config.SanitiseChatMessages) {
                        logger.info("Sanitising chat message content...");
                        cleanedMessage = cleanedMessage.replaceAll("`", "\\\\\\\\`");
                        cleanedMessage = cleanedMessage.replaceAll("\\*", "\\\\\\\\*");
                        cleanedMessage = cleanedMessage.replaceAll("~", "\\\\\\\\~");
                        cleanedMessage = cleanedMessage.replaceAll("_", "\\\\\\\\_");
                    }

                    formatted = formatted.replaceAll("%playername%", player.getName());
                    formatted = formatted.replaceAll("%chatmessage%", cleanedMessage);
                    break;
                case "AdvancementEvent":
                    AdvancementEvent advancementEvent = (AdvancementEvent) eventData;
                    EntityPlayer playerGot = advancementEvent.getEntityPlayer();
                    String advancementName = ((AdvancementEvent) eventData).getAdvancement().getDisplayText().getUnformattedText(); // what
                    String cleanAdvancementName = advancementName.replaceAll("\\[", "");
                    cleanAdvancementName = cleanAdvancementName.replaceAll("]", "");

                    formatted = formatted.replaceAll("%playername%", playerGot.getName());
                    formatted = formatted.replaceAll("%advancementname%", cleanAdvancementName);
                    break;
                default:
            }
        }

        return formatted;
    }

    @EventHandler
    public void started(FMLServerStartedEvent event) {
        serverInitTimestamp = Instant.now().toEpochMilli();
        if (!ConfigHandler.Config.TriggerOnServerInitialised) return;

        logger.info("Server opened event triggered!");
        sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.ServerInitialisedLayout, Optional.empty()), Optional.of(ConfigHandler.Config.ServerInitialisedLayoutUseEmbed), Optional.empty(), Optional.empty());
    }

    @EventHandler
    public void closure(FMLServerStoppingEvent event) {
        currentlyShuttingDown = true;
        if (!ConfigHandler.Config.TriggerOnServerClosing) return;

        logger.info("Server closing event triggered!");
        sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.ServerClosingLayout, Optional.empty()), Optional.of(ConfigHandler.Config.ServerClosingLayoutUseEmbed), Optional.empty(), Optional.empty());
    }

    @Mod.EventBusSubscriber
    public static class Class {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (ConfigHandler.Config.DiscordWebhookToken.isEmpty()) {
                ITextComponent WarningMessage = CommandHandler.createChatMessage(String.format("§c§l/!\\ Server Configuration Warning!\n§cYou have not provided a Discord webhook URL in the mod's configuration file, any events that trigger a webhook will not succeed until you provide one.\n\n§cThe configuration file is located at: §c§o%s", ConfigHandler.Config.ConfigurationFullLocation));
                event.player.sendMessage(WarningMessage);
            }
            playerPlaytimeActivity.put(event.player.getUniqueID(), Instant.now().toEpochMilli());

            if (!ConfigHandler.Config.TriggerOnPlayerJoin || currentlyShuttingDown) return;
            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerJoinLayout, Optional.of(event)), Optional.of(ConfigHandler.Config.PlayerJoinLayoutUseEmbed), Optional.of(event.player.getUniqueID()), Optional.empty());
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerLeave || currentlyShuttingDown) return;
            if (!playerPlaytimeActivity.containsKey(event.player.getUniqueID())) {
                logger.warn(String.format("PlayerLoggedOutEvent was called on player `%s` but they are not in the playerPlaytimeActivity HashMap!", event.player.getUniqueID()));
                return;
            }

            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerLeaveLayout, Optional.of(event)), Optional.of(ConfigHandler.Config.PlayerLeaveLayoutUseEmbed), Optional.of(event.player.getUniqueID()), Optional.empty());
        }

        @SubscribeEvent
        public static void onPlayerChat(ServerChatEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerChat || currentlyShuttingDown) return;
            EntityPlayer player = event.getPlayer();

            if (event.getPlayer() != null) {
                sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerChatLayout, Optional.of(event)), Optional.of(ConfigHandler.Config.PlayerChatLayoutUseEmbed), Optional.of(player.getUniqueID()), Optional.empty());
            }
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerDeath || currentlyShuttingDown) return;
            if (!(event.getEntity() instanceof EntityPlayer)) return;
            EntityPlayer deadPlayer = (EntityPlayer) event.getEntity();

            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerDeathLayout, Optional.of(event)), Optional.of(ConfigHandler.Config.PlayerDeathLayoutUseEmbed), Optional.of(deadPlayer.getUniqueID()), Optional.empty());
        }

        @SubscribeEvent
        public static void onPlayerAchievement(AdvancementEvent event) {
            if (!ConfigHandler.Config.TriggerOnPlayerAdvancement || currentlyShuttingDown) return;
            String advancementName = event.getAdvancement().getDisplayText().getUnformattedText();
            if (advancementName.contains(":recipes/")) {
                logger.warn("Ignoring `recipes` advancement as it is not a proper advancement: {}...", advancementName);
                return;
            }
            EntityPlayer player = event.getEntityPlayer();

            sendDiscordWebhookRequestThreaded(formatMessageContentBasedOnLayout(ConfigHandler.Config.PlayerAdvancementLayout, Optional.of(event)), Optional.of(ConfigHandler.Config.PlayerAdvancementLayoutUseEmbed), Optional.of(player.getUniqueID()), Optional.empty());
        }
    }
}
