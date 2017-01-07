package br.com.brjdevs.bran.core.action;

import br.com.brjdevs.bran.Bot;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Action {
	
	private static final List<Action> actions = new ArrayList<>();
	@Getter
	private final List<String> usersId;
	@Getter
	private final List<String> expectedInput;
	private final int shardId;
	@Getter
	private IEvent listener;
	@Getter
	private String messageId;
	@Getter
	private String channelId;
	@Getter
	private ActionType actionType;
	@Getter
	@Setter
	private Object[] extras;
	
	public Action(ActionType actionType, Message message, IEvent listener, List<String> expectedInput) {
		this.usersId = new ArrayList<>();
		this.expectedInput = expectedInput;
		this.listener = listener;
		this.messageId = message.getId();
		this.channelId = message.getChannel().getId();
		this.actionType = actionType;
		this.shardId = Bot.getInstance().getShardId(message.getJDA());
		
		actions.add(this);
	}
	
	public Action(ActionType actionType, Message message, IEvent listener, String... expectedInputs) {
		this(actionType, message, listener, Arrays.asList(expectedInputs));
	}
	
	public static Action getAction(String userId) {
		return actions.stream().filter(action -> action.getUsersId().contains(userId)).findFirst().orElse(null);
	}
	
	public static void remove(Action action) {
		actions.remove(action);
	}
	
	public JDA getJDA() {
		return Bot.getInstance().getShard(shardId);
	}
	
	public MessageChannel getChannel() {
		return getJDA().getTextChannelById(channelId);
	}
	
	public void addUser(User user) {
		usersId.add(user.getId());
	}
}
