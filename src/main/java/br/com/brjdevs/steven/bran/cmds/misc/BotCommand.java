package br.com.brjdevs.steven.bran.cmds.misc;

import br.com.brjdevs.steven.bran.Bot;
import br.com.brjdevs.steven.bran.BotManager;
import br.com.brjdevs.steven.bran.core.command.*;
import br.com.brjdevs.steven.bran.core.data.DataManager;
import br.com.brjdevs.steven.bran.core.quote.Quotes;
import br.com.brjdevs.steven.bran.core.utils.Hastebin;
import br.com.brjdevs.steven.bran.core.utils.ListBuilder;
import br.com.brjdevs.steven.bran.core.utils.ListBuilder.Format;
import br.com.brjdevs.steven.bran.core.utils.RequirementsUtils;
import br.com.brjdevs.steven.bran.core.utils.Util;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collectors;

import static br.com.brjdevs.steven.bran.core.managers.Permissions.BOT_ADMIN;

public class BotCommand {
	
	@Command
	private static ICommand bot() {
		return new TreeCommandBuilder(Category.MISCELLANEOUS)
				.setName("Bot Command")
				.setAliases("bot")
				.setHelp("bot ?")
				.setExample("bot stats")
				.setDescription("Multiple options and informations on me!")
				.addSubCommand(new CommandBuilder(Category.INFORMATIVE)
						.setAliases("info")
						.setName("Info Command")
						.setDescription("Gives you information about me!")
						.setAction((event) -> event.sendMessage(Bot.getInstance().getInfo()).queue())
						.build())
				.addSubCommand(new CommandBuilder(Category.INFORMATIVE)
						.setAliases("inviteme", "invite")
						.setName("InviteMe Command")
						.setDescription("Gives you my OAuth URL!")
						.setAction((event) -> {
							MessageEmbed embed = new EmbedBuilder()
									.setAuthor("Bran's OAuth URL", null, Util.getAvatarUrl(event.getJDA().getSelfUser()))
									.setDescription("You can invite me to your server by [clicking here](https://discordapp.com/oauth2/authorize?client_id=219186621008838669&scope=bot&permissions=0)")
									.setColor(Color.decode("#2759DB"))
									.build();
							event.sendMessage(embed).queue();
						})
						.build())
				.addSubCommand(new CommandBuilder(Category.INFORMATIVE)
						.setAliases("stats", "status")
						.setName("Stats Command")
						.setDescription("Gives you my current statistics!")
						.setAction((event) -> event.sendMessage(Bot.getInstance().getSession().toString(event.getJDA())).queue())
						.build())
				.addSubCommand(new CommandBuilder(Category.INFORMATIVE)
						.setAliases("ping")
						.setName("Ping Command")
						.setDescription("Gives you my ping!")
						.setAction((event) -> {
							long time = System.currentTimeMillis();
							event.getChannel().sendTyping().queue(success -> {
								long ping = System.currentTimeMillis() - time;
								event.getChannel().sendMessage("Pong: `" + ping + "ms`").queue();
							});
						})
						.build())
				.addSubCommand(new TreeCommandBuilder(Category.BOT_ADMINISTRATOR)
						.setAliases("admin")
						.setName("Bot Admin Command")
						.setHelp("bot admin ?")
						.setRequiredPermission(BOT_ADMIN)
						.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
								.setAliases("save")
								.setName("Save Command")
								.setDescription("Saves Guild and Bot Data.")
								.setAction((event) -> {
									try {
										DataManager.saveData();
										event.sendMessage(Quotes.SUCCESS, "Successfully saved Bot and Guild data.").queue();
									} catch (Exception e) {
										event.sendMessage(Quotes.FAIL, "There was an error while saving the data: " + Hastebin.post(Util.getStackTrace(e))).queue();
									}
								})
								.build())
						.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
								.setAliases("stop", "shutdown")
								.setName("Shutdown Command")
								.setDescription("Saves Guild and Bot Data and stops the bot.")
								.setAction((event, args) -> {
									BotManager.preShutdown();
									event.sendMessage("\uD83D\uDC4B").complete();
									BotManager.shutdown(false);
								})
								.build())
						.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
								.setAliases("updatestats")
								.setName("Update Stats Command")
								.setDescription("Updates the current guild amount in DiscordBots.")
								.setAction((event) -> {
									Bot.getInstance().updateStats();
									event.sendMessage(Quotes.getQuote(Quotes.SUCCESS)).queue();
								})
								.build())
						.addSubCommand(new TreeCommandBuilder(Category.BOT_ADMINISTRATOR)
								.setAliases("account", "acc")
								.setName("Bot Account Command")
								.setHelp("bot admin account ?")
								.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
										.setAliases("avatar")
										.setName("Account Avatar Command")
										.setDescription("Updates my Avatar.")
										.setArgs(new Argument<>("avatar", String.class))
										.setAction((event, args) -> {
											URL url;
											try {
												url = new URL((String) event.getArgument("avatar").get());
											} catch (MalformedURLException e) {
												event.sendMessage(Quotes.FAIL, "`" + event.getArgument("avatar").get() + "` is not a valid URL.").queue();
												return;
											}
											try {
												event.getJDA().getSelfUser().getManager().setAvatar(Icon.from(url.openStream())).queue(success ->
																event.sendMessage(Quotes.SUCCESS, "Updated my Avatar! Yay, I've got a new avatar! :smile:").queue(),
														fail -> {
															String hastebin = Hastebin.post(Util.getStackTrace(fail));
															event.sendMessage(Quotes.FAIL, "Something went wrong while updating my avatar. (`" + hastebin + "`)").queue();
														});
											} catch (IOException e) {
												event.sendMessage("Failed to open InputStream.").queue();
											}
										})
										.build())
								.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
										.setAliases("name")
										.setName("Account Name Command")
										.setDescription("Updates my name.")
										.setArgs(new Argument<>("name", String.class))
										.setAction((event, args) -> {
											String name = (String) event.getArgument("name").get();
											Bot.getInstance().getShards().forEach((i, shard) -> shard.getSelfUser().getManager().setName(name).queue());
											event.sendMessage("Updated my name! Yay, I've got a new name, cool! :smile:").queue();
										})
										.build())
								.build())
						.addSubCommand(new TreeCommandBuilder(Category.BOT_ADMINISTRATOR)
								.setAliases("guilds")
								.setHelp("bot admin guilds ?")
								.setName("Guilds Command")
								.setDefault("list")
								.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
										.setAliases("botc", "botcollections")
										.setName("Bot Collection Guilds Command")
										.setDescription("Lists you the Bot Collection Guilds.")
										.setArgs(new Argument<>("page", Integer.class, true))
										.setAction((event, args) -> {
											if (RequirementsUtils.getBotCollections().isEmpty()) {
												event.sendMessage("Oh yeah, I'm not in any Bot Collection Guilds!").queue();
												return;
											}
											Argument pageArg = event.getArgument("amount");
											int page = pageArg.isPresent() ? (int) pageArg.get() : 1;
											ListBuilder listBuilder = new ListBuilder(RequirementsUtils.getBotCollections().stream().map(g -> g.getName() + " (" + g.getId() + "[" + Bot.getInstance().getShardId(g.getJDA()) +"]) Bots: " + Util.DECIMAL_FORMAT.format(RequirementsUtils.getBotsPercentage(g))).collect(Collectors.toList()), page, 15);
											listBuilder.setName("Bot Collection Guilds").setFooter("Total Guilds: " + RequirementsUtils.getBotCollections().size());
											event.sendMessage(listBuilder.format(Format.CODE_BLOCK, "md")).queue();
										})
										.build())
								.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
										.setAliases("list")
										.setName("Guilds List Command")
										.setDescription("Lists you all my guilds.")
										.setArgs(new Argument<>("page", Integer.class, true))
										.setAction((event, args) -> {
											Argument pageArg = event.getArgument("page");
											int page = pageArg.isPresent() ? (int) pageArg.get() : 1;
											ListBuilder listBuilder = new ListBuilder(Bot.getInstance().getGuilds().stream().map(g -> g.getName() + " (" + g.getId() + "[" + Bot.getInstance().getShardId(g.getJDA()) + "]) | Owner: " + Util.getUser(g.getOwner().getUser())).collect(Collectors.toList()), page, 15);
											listBuilder.setName("Bran Server List").setFooter("Total Servers: " + Bot.getInstance().getGuilds().size());
											event.sendMessage(listBuilder.format(Format.CODE_BLOCK, "md")).queue();
										})
										.build())
								.addSubCommand(new CommandBuilder(Category.BOT_ADMINISTRATOR)
										.setAliases("leave")
										.setName("Leave Guild Command")
										.setDescription("Leaves a Guild.")
										.setArgs(new Argument<>("guildId", String.class))
										.setAction((event, args) -> {
											String guildId = (String) event.getArgument("guildId").get();
											Guild guild = event.getJDA().getGuildById(guildId);
											if (guild == null) {
												event.sendMessage("`" + guildId + "` is not a valid Guild ID or it's unknown for me...").queue();
												return;
											}
											guild.leave().queue();
											event.sendMessage(Quotes.SUCCESS, "Left " + guild.getName() + "." +
													(RequirementsUtils.getBotCollections().contains(guild) ? " *Finally, I really wanted to leave that Bot Collection Guild...*" : " But... Why?! That Guild wasn't a Bot Collection!")).queue();
										})
										.build())
								.build())
						.build())
				.build();
	}
}