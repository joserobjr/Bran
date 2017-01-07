package br.com.brjdevs.bran.jdaLoader.impl;

import br.com.brjdevs.bran.Bot;
import br.com.brjdevs.bran.core.data.bot.Config;
import br.com.brjdevs.bran.core.utils.Util;
import br.com.brjdevs.bran.jdaLoader.JDALoader;
import br.com.brjdevs.bran.jdaLoader.LoaderType;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class JDALoaderImpl implements JDALoader {
	private final LoaderType loaderType;
	
	public JDALoaderImpl (LoaderType loaderType) {
		this.loaderType = loaderType;
	}
	
	@Override
	public LoaderType getType() {
		return loaderType;
	}
	
	@Override
	public Map<Integer, JDA> build(boolean isComplete, int shards) throws LoginException, RateLimitedException {
		if (shards > 1 && loaderType == LoaderType.SINGLE) {
			throw new RuntimeException("Got LoaderType.SINGLE and more then one shard... What the fuck did you do?");
		}
		Config config = Bot.getInstance().getConfig();
		Map<Integer, JDA> out = new HashMap<>();
		JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
				.setToken(config.getToken()).setAudioEnabled(true)
				.setAutoReconnect(true)
				.addListener(new Reflections("br.com.brjdevs.bran")
						.getSubTypesOf(EventListener.class).stream()
						.map(clazz -> {
							try {
								return clazz.newInstance();
							} catch (Exception e) {
								Bot.LOG.log(e);
							}
							return null;
						}).filter(Objects::nonNull).toArray());
		if (loaderType == LoaderType.SHARDED) {
			for (int i = 0; i < shards; i++) {
				Bot.LOG.info("Building Shard " + i + "/" + (shards - 1));
				jdaBuilder.useSharding(i, shards);
				if (config.getGame() != null && !config.getGame().isEmpty())
					jdaBuilder.setGame(config.isGameStream() ? Game.of("[" + i + "] " + config.getGame(), "https://twitch.tv/ ") : Game.of("[" + i + "]" + config.getGame()));
				JDA jda = jdaBuilder.buildAsync();
				while (jda.getStatus() != Status.CONNECTED && isComplete)
					Util.sleep(100);
				out.put(i, jda);
				Bot.LOG.info("Finished loading Shard " + i + "/" + (shards - 1));
				if (i != shards - 1)
					Bot.LOG.info("Waiting 5 seconds until next shard...");
				Util.sleep(TimeUnit.SECONDS.toMillis(5));
			}
			Bot.LOG.info("Finished loading all shards!");
			Bot.LOG.info("Time taken: " + Bot.getInstance().getSession().getUptime());
		} else {
			Bot.LOG.info("Building single JDA instance...");
			if (config.getGame() != null && !config.getGame().isEmpty())
				jdaBuilder.setGame(config.isGameStream() ? Game.of(config.getGame(), "https://twitch.tv/ ") : Game.of(config.getGame()));
			JDA jda = jdaBuilder.buildAsync();
			while (jda.getStatus() != Status.CONNECTED && isComplete)
				Util.sleep(100);
			out.put(0, jda);
			Bot.LOG.info("Finished loading JDA!");
			Bot.LOG.info("Time taken: " + Bot.getInstance().getSession().getUptime());
		}
		return out;
	}
}
