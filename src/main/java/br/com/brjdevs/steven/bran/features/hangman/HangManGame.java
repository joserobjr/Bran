package br.com.brjdevs.steven.bran.features.hangman;

import br.com.brjdevs.steven.bran.Bot;
import br.com.brjdevs.steven.bran.BotContainer;
import br.com.brjdevs.steven.bran.core.data.bot.settings.Profile;
import br.com.brjdevs.steven.bran.core.utils.MathUtils;
import br.com.brjdevs.steven.bran.core.utils.StringUtils;
import br.com.brjdevs.steven.bran.core.utils.Util;
import br.com.brjdevs.steven.bran.features.hangman.events.*;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HangManGame {
	
	private static final List<HangManGame> sessions;
	
	static {
		sessions = new ArrayList<>();
	}

	private final LinkedHashMap<String, Boolean> guesses;
	private final List<Profile> invitedUsers;
	private final String word;
	private final String channel;
	private final List<String> mistakes;
	private final IEventListener listener;
	public HMProfileListener profileListener;
	private AtomicReference<Message> lastMessage;
	private long lastGuess;
	private Profile creator;
	private int shard;
	private int usedItems;
	
	public HangManGame(Profile profile, String word, TextChannel channel, BotContainer container) {
		this.listener = new EventListener(container);
		this.guesses = new LinkedHashMap<>();
		this.creator = profile;
		this.mistakes = new ArrayList<>();
		this.lastMessage = new AtomicReference<>();
		this.word = word;
		this.invitedUsers = new ArrayList<>();
		this.lastGuess = System.currentTimeMillis();
		this.channel = channel.getId();
		this.shard = container.getShardId(channel.getJDA());
		this.profileListener = new HMProfileListener(this, container);
		this.usedItems = 0;
		Arrays.stream(word.split(""))
				.forEach(split ->
						guesses.put(guesses.containsKey(split) ? split + Util.randomName(3) : split, false));
		if (word.contains(" ")) {
			guesses.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().charAt(0) == ' ').forEach(entry -> guesses.replace(entry.getKey(), true));
		}
		sessions.add(this);
		profile.registerListener(profileListener);
	}
	
	public static HangManGame getSession(Profile profile) {
		return sessions.stream().filter(session -> session.getCreator().equals(profile) || session.getInvitedUsers().contains(profile)).findAny().orElse(null);
	}
	
	public IEventListener getListener() {
		return listener;
	}
	
	public long getLastGuess() {
		return lastGuess;
	}
	
	public TextChannel getChannel(BotContainer container) {
		return getShard(container).getJDA().getTextChannelById(channel);
	}
	
	public Bot getShard(BotContainer container) {
		return container.getShards()[shard];
	}
	
	public boolean isMultiplayer() {
		return !getInvitedUsers().isEmpty();
	}
	
	public void remove(Profile profile) {
		invitedUsers.remove(profile);
	}

	public void invite(Profile profile) {
		profile.registerListener(profileListener);
		this.invitedUsers.add(profile);
	}
	
	public AtomicReference<Message> getLastMessage() {
		return lastMessage;
	}
	
	public void setLastMessage(Message message) {
		this.lastMessage.set(message);
	}
	
	public List<Profile> getInvitedUsers() {
		return invitedUsers;
	}

	public int getMaxErrors() {
		return 5 + (getInvitedUsers().size() * 3);
	}
	
	public void guess(String string, Profile profile, BotContainer container) {
		this.lastGuess = System.currentTimeMillis();
		if (isGuessed(string)) {
			getListener().onEvent(new AlreadyGuessedEvent(this, getShard(container).getJDA(), profile, StringUtils.containsEqualsIgnoreCase(getWord(), string), string));
			return;
		}
		if (isInvalid(string)) {
			mistakes.add(string);
			if (mistakes.size() > getMaxErrors()) {
				getListener().onEvent(new LooseEvent(this, getShard(container).getJDA(), false));
				return;
			}
			getListener().onEvent(new GuessEvent(this, getShard(container).getJDA(), profile, false, string));
			return;
		}
		guesses.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().charAt(0) == String.valueOf(string).toLowerCase().charAt(0)).forEach(entry -> guesses.replace(entry.getKey(), true));
		if (getGuessedLetters().equals(getWord())) {
			getListener().onEvent(new WinEvent(this, getShard(container).getJDA()));
			return;
		}
		getListener().onEvent(new GuessEvent(this, getShard(container).getJDA(), profile, true, string));
	}

	public String getGuessedLetters() {
		return String.join("", guesses.entrySet().stream().map(entry -> (entry.getValue() ? entry.getKey().charAt(0) : "\\_") + "").collect(Collectors.toList()));
	}
	
	public String getWord() {
		return word;
	}
	
	public List<String> getGuesses() {
		return guesses.entrySet().stream().map(entry -> entry.getValue() ? entry.getKey() : "_").collect(Collectors.toList());
	}

	public List<String> getMistakes() {
		return mistakes;
	}

	public boolean isGuessed(String c) {
		return Util.containsEqualsIgnoreCase(getGuesses(), c) || Util.containsEqualsIgnoreCase(getMistakes(), c);
	}
	
	public boolean isInvalid(String c) {
		return !StringUtils.containsEqualsIgnoreCase(getWord(), c);
	}
	
	public Profile getCreator() {
		return creator;
	}
	
	public void setCreator(Profile profile) {
		getInvitedUsers().remove(profile);
		getInvitedUsers().add(creator);
		this.creator = profile;
	}
	
	public List<Profile> getProfiles() {
		List<Profile> profiles = new ArrayList<>();
		profiles.add(getCreator());
		profiles.addAll(getInvitedUsers());
		return profiles;
	}
	
	public Field getCurrentGuessesField(boolean inline) {
		return new Field("_ _", "**These are your current guesses:** " + getGuessedLetters() + "\n**These are your current mistakes:** " + String.join(", ", getMistakes().stream().map(String::valueOf).collect(Collectors.toList())) + "         *Total Mistakes: " + getMistakes().size() + "/" + getMaxErrors() + "*", inline);
	}
	
	public Field getInvitedUsersField(boolean inline, BotContainer container) {
		return new Field("Invited Users", getInvitedUsers().isEmpty() ? "There are no invited users in this session, use `" + container.config.getDefaultPrefixes().get(0) + "hm invite [mention]` to invite someone to play with you!" : "There are " + getInvitedUsers().size() + " users playing in this session.\n" + (String.join(", ", getInvitedUsers().stream().map(profile -> profile.getUser(getShard(container).getJDA()).getName()).collect(Collectors.toList()))), inline);
	}
	
	public int getUsedItems() {
		return usedItems;
	}
	
	public void addUseItem() {
		this.usedItems++;
	}
	
	public String getRandomLetter() {
		int random = MathUtils.random(word.length());
		char c = getGuessedLetters().charAt(random);
		while (c != '_') {
			if (random <= 0) random++;
			else random--;
			c = getGuessedLetters().charAt(random);
		}
		return String.valueOf(getWord().charAt(random));
	}
	
	public void guess(String string) {
		guesses.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().charAt(0) == string.toLowerCase().charAt(0)).forEach(entry -> guesses.replace(entry.getKey(), true));
	}
	
	public EmbedBuilder createEmbed(BotContainer container) {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Hang Man");
		builder.setFooter("Session created by " + getCreator().getUser(getShard(container).getJDA()).getName(), Util.getAvatarUrl(getCreator().getUser(getShard(container).getJDA())));
		builder.setColor(getCreator().getEffectiveColor());
		builder.addField(getCurrentGuessesField(false));
		builder.addField(getInvitedUsersField(false, container));
		return builder;
	}
	public void end() {
		getProfiles().forEach(p -> p.unregisterListener(profileListener));
		sessions.remove(this);
	}
}