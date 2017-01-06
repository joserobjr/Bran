package br.com.brjdevs.bran.cmds.misc;

import br.com.brjdevs.bran.Bot;
import br.com.brjdevs.bran.core.command.Category;
import br.com.brjdevs.bran.core.command.CommandBuilder;
import br.com.brjdevs.bran.core.command.CommandManager;
import br.com.brjdevs.bran.core.command.RegisterCommand;
import br.com.brjdevs.bran.core.utils.MathUtils;
import br.com.brjdevs.bran.core.utils.StringUtils;
import br.com.brjdevs.bran.core.utils.Util;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Game.GameType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RegisterCommand
public class UserInfoCommand {
	public UserInfoCommand() {
		CommandManager.addCommand(new CommandBuilder(Category.INFORMATIVE)
				.setAliases("user", "userinfo", "uinfo")
				.setName("User Info Command")
				.setDescription("Gives you info on the mentioned user")
				.setArgs("<user ID>")
				.setExample("user " + Bot.getInstance().getSelfUser(Bot.getInstance().getShard(0)).getId())
				.setPrivateAvailable(false)
				.setAction((event, args) -> {
					Member member = event.getMessage().getMentionedUsers().isEmpty() ? event.getOriginMember() : event.getOriginGuild().getMember(event.getMessage().getMentionedUsers().get(0));
					OffsetDateTime date = member.getJoinDate();
					String joinDate = StringUtils.neat(date.getDayOfWeek().toString().substring(0, 3)) + ", " + date.getDayOfMonth() + " " + StringUtils.neat(date.getMonth().toString().substring(0, 3)) + " " + date.getYear() + " " + MathUtils.toOctalInteger(date.getHour()) + ":" + MathUtils.toOctalInteger(date.getMinute()) + ":" + MathUtils.toOctalInteger(date.getSecond()) + " GMT";
					OffsetDateTime creation = member.getUser().getCreationTime();
					String createdAt = StringUtils.neat(creation.getDayOfWeek().toString().substring(0, 3)) + ", " + creation.getDayOfMonth() + " " + StringUtils.neat(creation.getMonth().toString().substring(0, 3)) + " " + creation.getYear() + " " + MathUtils.toOctalInteger(creation.getHour()) + ":" + MathUtils.toOctalInteger(creation.getMinute()) + ":" + MathUtils.toOctalInteger(creation.getSecond()) + " GMT";
					User user = member.getUser();
					EmbedBuilder embed = new EmbedBuilder();
					embed.addField("User", Util.getUser(user) + " (ID: " + user.getId() + ")", false);
					if (member.getNickname() != null)
						embed.addField("Nickname", member.getNickname(), true);
					if (member.getGame() != null)
						embed.addField(member.getGame().getType() == GameType.TWITCH ? "Streaming" : "Playing", member.getGame().getName(), true);
					embed.addField("Member since", joinDate, true);
					embed.addField("Account Created At", createdAt, true);
					embed.addField("Shared Guilds", String.valueOf(event.getJDA().getGuilds().stream().filter(guild -> guild.isMember(user)).count()), true);
					embed.addField("Roles", String.valueOf(member.getRoles().size()), true);
					embed.addField("Status", member.getOnlineStatus().toString(), true);
					List<Member> joins = new ArrayList<>(event.getOriginGuild().getMembers());
					joins.sort(Comparator.comparing(Member::getJoinDate));
					int index = joins.indexOf(member);
					index -= 3;
					if(index < 0)
						index = 0;
					String str = "";
					if (joins.get(index).equals(member))
						str += "**" + joins.get(index).getUser().getName() + "**";
					else
						str += joins.get(index).getUser().getName();
					for (int i = index + 1; i < index + 7; i++)
					{
						if(i >= joins.size())
							break;
						Member u = joins.get(i);
						String name = u.getUser().getName();
						if(u.equals(member))
							name = "**" + name + "**";
						str += " > "+ name;
					}
					Color color = member.getColor() == null ? Color.decode("#FFA300") : member.getColor();
					embed.setColor(color);
					embed.addField("Join Order", str, false);
					embed.setThumbnail(Util.getAvatarUrl(user));
					embed.setFooter("Requested by " + Util.getUser(event.getAuthor()), Util.getAvatarUrl(event.getAuthor()));
					event.sendMessage(embed.build()).queue();
				})
				.build());
	}
}