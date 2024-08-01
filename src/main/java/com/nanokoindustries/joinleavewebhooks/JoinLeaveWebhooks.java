package com.nanokoindustries.joinleavewebhooks;

import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Mod(modid = JoinLeaveWebhooks.MODID, name = JoinLeaveWebhooks.NAME, version = JoinLeaveWebhooks.VERSION, acceptableRemoteVersions = "*")
public class JoinLeaveWebhooks
{
    public static final String MODID = "joinleavewebhooks";
    public static final String NAME = "Join+Leave+ServerState+Chat Discord Webhook";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("NYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        logger.info("[NanokoIndustries] Join+Leave+ServerState+Chat Discord Webhook mod initialised!");
    }

    private static void sendDiscordWebhookRequestThreaded(String MessageContent, Optional<Boolean> Fancy, Optional<UUID> FancyPlayerUUID) {
        Boolean FancyOutput = Fancy.orElse(false);
        String PlayerUUID = String.valueOf(FancyPlayerUUID.orElse(null));
        logger.info("Sending \"{}\" to webhook!", MessageContent);

        Thread RequestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL webLink = new URL("");
                    HttpURLConnection connection = (HttpURLConnection) webLink.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("User-Agent", "Java Minecraft Forge");
                    connection.setRequestProperty("Host", "discord.com");
                    connection.setDoOutput(true);

                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    String jsonBody = "{\"content\":\"Mod failed to construct a proper jsonBody! Please report this\"}";
                    if (FancyOutput) {
                        jsonBody = String.format("{\"content\":null,\"embeds\":[{\"description\":\"%s\",\"thumbnail\":{\"url\":\"https://mc-heads.net/head/%s\"}}],\"allowed_mentions\":{\"parse\":[]}}", MessageContent, PlayerUUID);
                    } else {
                        jsonBody = String.format("{\"content\":\"%s\",\"allowed_mentions\":{\"parse\":[]}}", MessageContent);
                    }
                    logger.warn(jsonBody);
                    try (OutputStream stream = connection.getOutputStream()) {
                        byte[] buffer = jsonBody.getBytes(StandardCharsets.UTF_8);
                        stream.write(buffer, 0, buffer.length);
                    }

                    int httpStatusCode = connection.getResponseCode();

                    if (httpStatusCode != 204) {
                        logger.warn("The request to send to the webhook failed! Double check your Webhook URL, StatusCode = {}", httpStatusCode);
                    } else {
                        logger.info("Webhook request success!");
                    }

                    connection.disconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        RequestThread.setName("Webhook Request thread");
        RequestThread.start();
    }

    private static int getFormattedEpochForTimestampMarkdown() {
        return (int) Math.floor((double) Instant.now().toEpochMilli() / 1000);
    }

    @EventHandler
    public void closure(FMLServerStoppingEvent event) {
        logger.info("Server closing event triggered!");
        sendDiscordWebhookRequestThreaded(String.format("\uD83D\uDEAA Server shutting down! <t:%d:R>", getFormattedEpochForTimestampMarkdown()), Optional.empty(), Optional.empty());
    }

    @EventHandler
    public void started(FMLServerStartedEvent event) {
        logger.info("Server opened event triggered!");
        sendDiscordWebhookRequestThreaded(String.format("✅ Server has fully initialised! <t:%d:R>", getFormattedEpochForTimestampMarkdown()), Optional.empty(), Optional.empty());
    }

    @Mod.EventBusSubscriber
    public static class Class {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            logger.info("New player detected! Sending webhook...");
            sendDiscordWebhookRequestThreaded(String.format("➡\uFE0F `%s` joined the game. <t:%d:R>", event.player.getName(), getFormattedEpochForTimestampMarkdown()), Optional.of(true), Optional.of(event.player.getUniqueID()));
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            logger.info("Player left detected! Sending webhook...");
            sendDiscordWebhookRequestThreaded(String.format("\uD83D\uDEAA `%s` left the game. <t:%d:R>", event.player.getName(), getFormattedEpochForTimestampMarkdown()), Optional.of(true), Optional.of(event.player.getUniqueID()));
        }

        /*@SubscribeEvent
        public static void onPlayerChat(ServerChatEvent event) {
            EntityPlayer player = event.getPlayer();

            if (event.getPlayer() != null) {
                sendDiscordWebhookRequestThreaded(String.format("\uD83D\uDCAC `%s`: %s", player.getDisplayNameString(), event.getMessage()), Optional.of(true), Optional.of(player.getUniqueID()));
            }
        }*/
    }
}
