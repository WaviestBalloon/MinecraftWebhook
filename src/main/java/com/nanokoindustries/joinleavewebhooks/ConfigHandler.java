package com.nanokoindustries.joinleavewebhooks;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class ConfigHandler {
    private static final Logger logger = JoinLeaveWebhooks.logger;

    public static class Config {
        public static String ConfigurationFullLocation;
        public static Boolean AllowMentions;
        public static String UserAgent;
        public static Boolean DropRequestsIf429;
        public static Integer RetryCountLimit;
        public static Boolean EnableWebhookHandlerDebugging;
        public static Boolean SanitiseChatMessages;

        public static String PlayerJoinLayout;
        public static String DiscordWebhookToken = "";
        public static String PlayerLeaveLayout;
        public static String PlayerChatLayout;
        public static String PlayerDeathLayout;
        public static String PlayerAdvancementLayout;
        public static String ServerClosingLayout;
        public static String ServerInitialisedLayout;

        public static Boolean PlayerJoinLayoutUseEmbed;
        public static Boolean PlayerLeaveLayoutUseEmbed;
        public static Boolean PlayerChatLayoutUseEmbed;
        public static Boolean PlayerDeathLayoutUseEmbed;
        public static Boolean PlayerAdvancementLayoutUseEmbed;
        public static Boolean ServerClosingLayoutUseEmbed;
        public static Boolean ServerInitialisedLayoutUseEmbed;

        public static Boolean TriggerOnPlayerJoin;
        public static Boolean TriggerOnPlayerLeave;
        public static Boolean TriggerOnPlayerChat;
        public static Boolean TriggerOnPlayerDeath;
        public static Boolean TriggerOnPlayerAdvancement;
        public static Boolean TriggerOnServerClosing;
        public static Boolean TriggerOnServerInitialised;
    }

    public static void populateConfigClass(File configurationFile) {
        Configuration configuration = new Configuration(configurationFile);

        String category = "Customise";
        configuration.addCustomCategoryComment(category, "Configure what gets send through the webhooks, you can use Discord formatting outlined here: https://support.discord.com/hc/en-us/articles/210298617-Markdown-Text-101-Chat-Formatting-Bold-Italic-Underline");
        Config.PlayerJoinLayout = configuration.getString("PlayerJoinLayout", category, "➡\uFE0F <t:%epoch%:R>\\n`%playername%` joined the game.", "What to send when a player joins");
        Config.PlayerJoinLayoutUseEmbed = configuration.getBoolean("PlayerJoinLayoutUseEmbed", category, true, "Put the above in a embed instead of a generic message");
        Config.PlayerLeaveLayout = configuration.getString("PlayerLeaveLayout", category, "\uD83D\uDEAA <t:%epoch%:R>\\n`%playername%` left the game.\\nPlayed for %playtime% (`%rawplaytime%ms`)", "What to send when a player leaves");
        Config.PlayerLeaveLayoutUseEmbed = configuration.getBoolean("PlayerLeaveLayoutUseEmbed", category, true, "Put the above in a embed instead of a generic message");
        Config.PlayerChatLayout = configuration.getString("PlayerChatLayout", category, "\uD83D\uDCAC <t:%epoch%:R>\\n`%playername%`: %chatmessage%", "What to send when a player sends a message in chat");
        Config.PlayerChatLayoutUseEmbed = configuration.getBoolean("PlayerChatLayoutUseEmbed", category, true, "Put the above in a embed instead of a generic message");
        Config.PlayerDeathLayout = configuration.getString("PlayerDeathLayout", category, "\uD83D\uDC94 `%deathmessage%` <t:%epoch%:R>", "What to send when a player dies to something");
        Config.PlayerDeathLayoutUseEmbed = configuration.getBoolean("PlayerDeathLayoutUseEmbed", category, false, "Put the above in a embed instead of a generic message");
        Config.PlayerAdvancementLayout = configuration.getString("PlayerAdvancementLayout", category, "\uD83D\uDCD5 `%playername% has made the advancement [%advancementname%]` <t:%epoch%:R>", "What to send when a player dies to something");
        Config.PlayerAdvancementLayoutUseEmbed = configuration.getBoolean("PlayerAdvancementLayoutUseEmbed", category, false, "Put the above in a embed instead of a generic message");
        Config.ServerClosingLayout = configuration.getString("ServerClosingLayout", category, "\uD83D\uDEAA Server shutting down! <t:%epoch%:R>", "What to send when the server starts to shutdown");
        Config.ServerClosingLayoutUseEmbed = configuration.getBoolean("ServerClosingLayoutUseEmbed", category, false, "Put the above in a embed instead of a generic message");
        Config.ServerInitialisedLayout = configuration.getString("ServerInitialisedLayout", category, "✅ Server has fully initialised! <t:%epoch%:R>", "What to send when the server has finished initialising and is ready to accept players");
        Config.ServerInitialisedLayoutUseEmbed = configuration.getBoolean("ServerInitialisedLayoutUseEmbed", category, false, "Put the above in a embed instead of a generic message");

        category = "Toggles";
        configuration.addCustomCategoryComment(category, "Configure what you don't and do want triggering");
        Config.TriggerOnPlayerJoin = configuration.getBoolean("PlayerJoin", category, true, "Trigger the webhook if a player joins");
        Config.TriggerOnPlayerLeave = configuration.getBoolean("PlayerLeave", category, true, "Trigger the webhook if a player leaves");
        Config.TriggerOnPlayerChat = configuration.getBoolean("PlayerChat", category, true, "Trigger the webhook when a player speaks in the chat");
        Config.TriggerOnPlayerDeath = configuration.getBoolean("PlayerDeath", category, true, "Trigger the webhook when a player dies from something");
        Config.TriggerOnPlayerAdvancement = configuration.getBoolean("PlayerAdvancement", category, true, "Trigger the webhook when a player gets a achievement");
        Config.TriggerOnServerClosing = configuration.getBoolean("ServerClosing", category, true, "Trigger the webhook if the server starts shutting down");
        Config.TriggerOnServerInitialised = configuration.getBoolean("ServerInitialised", category, true, "Trigger the webhook when the server finished initialising and is ready to accept players");

        category = "Discord Setup";
        configuration.addCustomCategoryComment(category, "Configure webhooks here");
        Config.DiscordWebhookToken = configuration.getString("DiscordWebhookURL", category, "", "DO NOT SHARE THIS URL WITH ANYONE");
        Config.AllowMentions = configuration.getBoolean("AllowMentions", category, false, "Determines whether the messages have the ability to ping members or ping everyone");
        Config.SanitiseChatMessages = configuration.getBoolean("SanitiseChatMessages", category, true, "Removes Discord markdown from player chat messages");
        Config.UserAgent = configuration.getString("UserAgent", category, "Java Minecraft Forge dedicated server", "The User-Agent identifier used to make the request, you shouldn't need to change this");
        Config.DropRequestsIf429 = configuration.getBoolean("DropRequestsIf429", category, false, "Instead of moving the request into a retry array, just drop it instead");
        Config.RetryCountLimit = configuration.getInt("RetryCountLimit", category, 5, 1, 50, "How many times to retry the request if we get rate limited");
        Config.EnableWebhookHandlerDebugging = configuration.getBoolean("EnableWebhookHandlerDebugging", category, false, "Enables logging outputs for the Webhook handler, might get a bit spammy");

        configuration.save();
    }

    public static File initConfig(FMLPreInitializationEvent event) {
        File modConfigurationDirectory = event.getModConfigurationDirectory().getAbsoluteFile();
        File configurationFile = new File(modConfigurationDirectory + "/joinleavewebhooks.cfg");
        Config.ConfigurationFullLocation = configurationFile.getAbsolutePath();

        populateConfigClass(configurationFile);

        return configurationFile;
    }
}
