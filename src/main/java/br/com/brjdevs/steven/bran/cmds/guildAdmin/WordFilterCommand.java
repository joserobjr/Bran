package br.com.brjdevs.steven.bran.cmds.guildAdmin;

import br.com.brjdevs.steven.bran.core.command.*;
import br.com.brjdevs.steven.bran.core.managers.Permissions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WordFilterCommand {
	
	@Command
	private static ICommand wordFilter() {
		return new TreeCommandBuilder(Category.GUILD_ADMINISTRATOR)
				.setAliases("wordfilter", "wf")
				.setName("WordFilter Command")
				.setHelp("wordfilter ?")
				.setDescription("Don't want people talking trash in your server? This will help you.")
				.setPrivateAvailable(false)
				.addSubCommand(new CommandBuilder(Category.GUILD_ADMINISTRATOR)
						.setAliases("add")
						.setName("WordFilter Add Command")
						.setDescription("Adds a Word to the WordFilter")
						.setArgs(new Argument<>("word", String.class))
						.setRequiredPermission(Permissions.GUILD_MANAGE)
						.setAction((event, rawArgs) -> {
							if (!event.getDiscordGuild().getWordFilter().isEnabled()) {
								event.sendMessage("Before adding words to the WordFilter you have to enable it! Use `" + event.getPrefix() + "wordfilter toggle` to enable.").queue();
								return;
							}
							String[] words = Arrays.stream(Argument.split((String) event.getArgument("word").get())).filter(w -> !event.getDiscordGuild().getWordFilter().asList().contains(w)).toArray(String[]::new);
							if (words.length == 0) {
								event.sendMessage("No words to filter found.").queue();
								return;
							}
							Arrays.stream(words)
									.forEach(word -> event.getDiscordGuild().getWordFilter().asList().add(word));
							event.sendMessage("\uD83D\uDC4C Added " + words.length + " word(s) to the filter! *(Total: " + event.getDiscordGuild().getWordFilter().asList().size() + ")*").queue();
						})
						.build())
				.addSubCommand(new CommandBuilder(Category.INFORMATIVE)
						.setAliases("list")
						.setName("WordFilter List Command")
						.setDescription("Lists the filtered words in the current guild.")
						.setAction((event) -> {
							if (!event.getDiscordGuild().getWordFilter().isEnabled()) {
								event.sendMessage("The WordFilter is disabled in this guild." + (event.getMember().hasPermission(Permissions.GUILD_MANAGE, event.getJDA()) ? " Use `" + event.getPrefix() + "wf toggle` to enable it." : "")).queue();
								return;
							}
							event.sendPrivate("These are the filtered words in " + event.getGuild().getName()
									+ ":\n" + (String.join(", ", event.getDiscordGuild().getWordFilter().asList().stream()
									.map(w -> "`" + w + "`").collect(Collectors.toList())))).queue();
							event.sendMessage(
									"I've sent you the filtered words as a private message, check your DMs!")
									.queue();
						})
						.build())
				.addSubCommand(new CommandBuilder(Category.GUILD_ADMINISTRATOR)
						.setAliases("toggle")
						.setDescription("Enables/Disables the WordFilter.")
						.setName("WordFilter Toggle Command")
						.setRequiredPermission(Permissions.GUILD_MANAGE)
						.setAction((event) -> {
							event.getDiscordGuild().getWordFilter()
									.setEnabled(!event.getDiscordGuild().getWordFilter().isEnabled());
							boolean isEnabled = event.getDiscordGuild().getWordFilter().isEnabled();
							event.sendMessage(isEnabled ? "The WordFilter is now enabled!" : "The WordFilter is no longer enabled.").queue();
						})
						.build())
				.addSubCommand(new CommandBuilder(Category.GUILD_ADMINISTRATOR)
						.setRequiredPermission(Permissions.GUILD_MANAGE)
						.setAliases("remove")
						.setName("WordFilter Remove Command")
						.setDescription("Removes a word from the WordFilter.")
						.setAction((event, rawArgs) -> {
							if (!event.getDiscordGuild().getWordFilter().isEnabled()) {
								event.sendMessage("The WordFilter is disabled in this Guild.").queue();
								return;
							}
							String[] words = Arrays.stream(Argument.split((String) event.getArgument("word").get())).filter(w -> !event.getDiscordGuild().getWordFilter().asList().contains(w)).toArray(String[]::new);
							if (words.length == 0) {
								event.sendMessage("No words to remove from the filter found.").queue();
								return;
							}
							Arrays.stream(words)
									.forEach(word -> event.getDiscordGuild().getWordFilter().asList().add(word));
							event.sendMessage("\uD83D\uDC4C Removed " + words.length + " word(s) to the filter! *(Total: " + event.getDiscordGuild().getWordFilter().asList().size() + ")*").queue();
						})
						.build())
				.build();
	}
}