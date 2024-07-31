package com.example.examplemod;

import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String NAME = "Example Mod";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // some example code
        logger.info("NYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        logger.info("hello");
    }

    private static void sendDiscordWebhookRequestThreaded(String MessageContent) {
        logger.info("Sending \"{}\" to webhook!", MessageContent);
        logger.info("Creating new Request thread...");

        Thread RequestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL webLink = new URL("");
                    HttpURLConnection connection = (HttpURLConnection) webLink.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("User-Agent", "PostmanRuntime/7.40.0");
                    connection.setRequestProperty("Host", "discord.com");
                    connection.setDoOutput(true);

                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    String jsonBody = String.format("{\"content\":\"%s\"}", MessageContent);
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

    @Mod.EventBusSubscriber
    public static class Class {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            logger.info("New player detected! Sending webhook...");
            sendDiscordWebhookRequestThreaded(String.format("➡\uFE0F `%s` joined the game. <t:%d:R>", event.player.getName(), getFormattedEpochForTimestampMarkdown()));
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            logger.info("Player left detected! Sending webhook...");
            sendDiscordWebhookRequestThreaded(String.format("\uD83D\uDEAA `%s` left the game. <t:%d:R>", event.player.getName(), getFormattedEpochForTimestampMarkdown()));
        }
    }
}
