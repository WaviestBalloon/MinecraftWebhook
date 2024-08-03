package com.nanokoindustries.minecraftwebhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Webhook {
    private static final Logger logger = MinecraftWebhook.logger;

    private static void verboseLog(String message) {
        if (ConfigHandler.Config.EnableWebhookHandlerDebugging) {
            logger.warn("[WebhookHandler]: {}", message);
        }
    }

    public static void sendDiscordWebhookRequestThreaded(String MessageContent, Optional<Boolean> Fancy, Optional<UUID> FancyPlayerUUID, Optional<Integer> RetryCounter) {
        String token = ConfigHandler.Config.DiscordWebhookToken;
        if (token.isEmpty()) {
            logger.error("The webhook URL is empty! Refusing to send request - Edit your configuration file located in the \"config\" folder in the server files");
            return;
        }

        Boolean FancyOutput = Fancy.orElse(false);
        String PlayerUUID = String.valueOf(FancyPlayerUUID.orElse(null));

        verboseLog(String.format("Sending \"%s\" to webhook!", MessageContent));

        Thread RequestThread = new Thread(() -> {
            try {
                URL webhookURL = new URL(token);
                HttpURLConnection connection = (HttpURLConnection) webhookURL.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", ConfigHandler.Config.UserAgent);
                connection.setRequestProperty("Host", "discord.com");
                connection.setDoOutput(true);

                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                JsonObject jsonBody = new JsonObject();

                verboseLog(jsonBody.toString());
                if (FancyOutput) {
                    jsonBody.add("content", JsonNull.INSTANCE);
                    JsonArray jsonEmbedsArray = new JsonArray();
                    JsonObject jsonEmbed = new JsonObject();
                    jsonEmbed.addProperty("description", MessageContent);

                    if (FancyPlayerUUID.isPresent()) {
                        JsonObject jsonEmbedThumbnailObject = new JsonObject();
                        jsonEmbedThumbnailObject.addProperty("url", String.format("https://mc-heads.net/head/%s/1", PlayerUUID));
                        jsonEmbed.add("thumbnail", jsonEmbedThumbnailObject);
                    }

                    jsonEmbedsArray.add(jsonEmbed);
                    jsonBody.add("embeds", jsonEmbedsArray);
                } else {
                    jsonBody.addProperty("content", MessageContent);
                }
                if (!ConfigHandler.Config.AllowMentions) {
                    JsonObject jsonAllowedMentions = new JsonObject();
                    JsonArray jsonAllowedMentionsParseArray = new JsonArray();
                    jsonAllowedMentions.add("parse", jsonAllowedMentionsParseArray);
                    jsonBody.add("allowed_mentions", jsonAllowedMentions);
                }

                verboseLog(jsonBody.toString());
                try (OutputStream stream = connection.getOutputStream()) {
                    byte[] buffer = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    stream.write(buffer, 0, buffer.length);
                }

                int httpStatusCode = connection.getResponseCode();

                verboseLog(connection.getResponseMessage());
                if (httpStatusCode == 429) {
                    if (ConfigHandler.Config.DropRequestsIf429) {
                        logger.warn("Being rate limited! Dropping webhook request due to DropRequestsIf429 being enabled...");
                        return;
                    }
                    Integer retries = RetryCounter.orElse(0);
                    if (retries >= ConfigHandler.Config.RetryCountLimit) {
                        logger.warn("Still being rate limited after {} attempts, reached configured limit! Dropping webhook request...", retries);
                        return;
                    }

                    logger.warn("Being rate limited! Waiting to retry after five seconds...");
                    TimeUnit.SECONDS.sleep(5);
                    sendDiscordWebhookRequestThreaded(MessageContent, Fancy, FancyPlayerUUID, Optional.of(retries + 1));
                } else if (httpStatusCode != 204) {
                    logger.warn("The request to send to the webhook failed! Double-check your Webhook URL, got status code {} instead of 204 (If you're having issues pin pointing the issue, enable `EnableWebhookHandlerDebugging` in your configuration file)", httpStatusCode);
                    BufferedReader Buffer = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = Buffer.readLine()) != null) {
                        response.append(inputLine);
                    }

                    logger.warn(response.toString());
                } else {
                    logger.info("Webhook request success!");
                }

                connection.disconnect();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        RequestThread.setName("Webhook Request thread");
        RequestThread.start();
    }
}
