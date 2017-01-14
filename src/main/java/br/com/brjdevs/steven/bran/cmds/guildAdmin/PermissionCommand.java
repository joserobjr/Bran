package br.com.brjdevs.steven.bran.cmds.guildAdmin;

import br.com.brjdevs.steven.bran.core.command.*;
import br.com.brjdevs.steven.bran.core.data.guild.configs.GuildMember;
import br.com.brjdevs.steven.bran.core.managers.Permissions;
import br.com.brjdevs.steven.bran.core.operations.ResultType;
import br.com.brjdevs.steven.bran.core.operations.ResultType.OperationResult;
import br.com.brjdevs.steven.bran.core.quote.Quotes;
import br.com.brjdevs.steven.bran.core.utils.Util;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import javax.xml.ws.Holder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionCommand {
	
	@Command
	private static ICommand permission() {
		return new TreeCommandBuilder(Category.GUILD_ADMINISTRATOR)
				.setAliases("perms", "perm")
                .setName("Permission Command")
                .setHelp("perms ?")
				.setDescription("Get & Set Permissions in your guild.")
				.setExample("perms set -MUSIC <@219186621008838669>")
                .setPrivateAvailable(false)
				.addSubCommand(new CommandBuilder(Category.GUILD_ADMINISTRATOR)
						.setAliases("set")
                        .setName("Permission Set Command")
                        .setDescription("Sets the Permission of an User")
						.setArgs(new Argument<>("perm", String.class), new Argument<>("user", String.class))
						.setExample("perms set -MUSIC <@219186621008838669>")
                        .setRequiredPermission(Permissions.PERMSYS_GM)
						.setAction((event) -> {
							Argument permArg = event.getArgument("perm");
							Argument userArg = event.getArgument("user");
							boolean isEveryone = ((String) userArg.get()).matches("^(\\*|all|everyone)$");
							User user = event.getMessage().getMentionedUsers().isEmpty() ? event.getJDA().getUserById((String) userArg.get()) : event.getMessage().getMentionedUsers().get(0);
							if (user == null && !isEveryone) {
		                        event.sendMessage(Quotes.FAIL, "You have to mention a User or give me a Valid ID!").queue();
		                        return;
	                        }
	                        if (!isEveryone && (user.isBot() || user.isFake())) {
		                        event.sendMessage(Quotes.FAIL, "You can't change permissions for Bots.").queue();
		                        return;
	                        }
							String[] all = ((String) permArg.get()).split("\\s+", -1);
							int toBeSet = 0, toBeUnset = 0;
	                        for (String each : all) {
		                        if (each.charAt(0) == '+') {
			                        String p = each.substring(1).toUpperCase();
			                        if (Permissions.perms.containsKey(p)) {
				                        toBeSet |= Permissions.perms.get(p);
			                        }
		                        } else if (each.charAt(0) == '-') {
			                        String p = each.substring(1).toUpperCase();
			                        if (Permissions.perms.containsKey(p)) {
				                        toBeUnset |= Permissions.perms.get(p);
			                        }
		                        }
	                        }
	                        Holder<Integer> holder = new Holder<>(0);
							List<Member> members = event.getGuild().getMembers().stream().filter(member -> member.getUser() != event.getAuthor() && !member.getUser().isBot() && !member.getUser().isFake()).collect(Collectors.toList());
							OperationResult operationResult = null;
	                        if (!isEveryone) {
		                        operationResult = event.getDiscordGuild().getMember(user).setPermission(event, toBeSet, toBeUnset);
	                        }
	                        else {
		                        long fToBeSet = toBeSet;
		                        long fToBeUnset = toBeUnset;
		                        members.forEach(member -> {
			                        if (event.getDiscordGuild().getMember(member).setPermission(event, fToBeSet, fToBeUnset).getResult() == ResultType.SUCCESS)
				                        holder.value++;
		                        });
	                        }
	                        String s;
	                        if (!isEveryone) {
	                        	if (operationResult.getResult() == ResultType.SUCCESS)
			                        s = "Updated " + Util.getUser(user) + " permissions!";
		                        else if (operationResult.getResult() == ResultType.INVALID) {
			                        s = "How the hell did you manage to update a Fake Member permission?";
		                        } else {
			                        s = operationResult.getExtras()[0].toString();
		                        }
	                        }
	                        else
		                        s = "Updated " + holder.value + " members permissions! Could not update " + (members.size() - holder.value) + " members permissions.";
	                        event.sendMessage(Quotes.SUCCESS, s).queue();
                        })
                        .build())
				.addSubCommand(new CommandBuilder(Category.INFORMATIVE)
						.setAliases("get")
                        .setName("Permission Get Command")
                        .setDescription("Returns you the Permission of an User.")
						.setArgs(new Argument<>("user", String.class, true))
						.setExample("perms get <@219186621008838669>")
                        .setAction((event, a) -> {
	                        User user = event.getMessage().getMentionedUsers().isEmpty() ? event.getJDA().getUserById((String) event.getArgument("user").get()) : event.getMessage().getMentionedUsers().get(0);
	                        if (user == null) user = event.getAuthor();
	                        GuildMember member = event.getDiscordGuild().getMember(user);
	                        EmbedBuilder builder = new EmbedBuilder();
                            builder.setTitle("Permissions for " + Util.getUser(user));
                            builder.setDescription((String.join(", ", member.getPermissions(event.getJDA()))) + "\n\nRaw: " + member.getRawPermissions(event.getJDA()));
                            builder.setThumbnail(Util.getAvatarUrl(user));
                            builder.setFooter("Requested by " + Util.getUser(event.getAuthor()), Util.getAvatarUrl(event.getAuthor()));
                            builder.setColor(Color.decode("#9318E6"));
                            event.sendMessage(builder.build()).queue();

                        })
                        .build())
				.build();
	}
}