package br.com.brjdevs.steven.bran.core.utils;

import br.com.brjdevs.steven.bran.Bot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class Util {
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    public static boolean isPrivate(MessageReceivedEvent event) {
        return event.isFromType(ChannelType.PRIVATE);
    }
    public static String getUser(User user) {
        if (user == null) return "Unknown#0000";
        return user.getName() + "#" + user.getDiscriminator();
    }
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
    public static String getAvatarUrl(User user) {
        return user.getAvatarUrl() == null ? user.getDefaultAvatarUrl() : user.getAvatarUrl();
    }
    public static Runnable async(final Runnable doAsync) {
        return new Thread(doAsync)::start;
    }
    public static Runnable async(final String name, final Runnable doAsync) {
        return new Thread(doAsync, name)::start;
    }
    public static boolean isInteger(String str) {
        try {
            int i = Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static String getStackTrace(Throwable e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    public static boolean isEmpty(Object object) {
        return object == null || object.toString().isEmpty();
    }
    public static String randomName(int randomLength) {
        char[] characters = new char[]
                {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
                        'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                        '1','2','3','4','5','6','7','8','9','0'};

        Random rand = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < randomLength; i++) {
            builder.append(characters[rand.nextInt(characters.length)]);
        }
        return builder.toString();
    }
	
	public static <T, K> Entry<T, K> getEntryByIndex(Map<T, K> map, int index) {
		return (Entry<T, K>) map.entrySet().toArray()[index];
	}
	
	public static <E> E random(List<E> list) {
		return list.get(MathUtils.random(list.size()));
	}
	public static boolean containsEqualsIgnoreCase(Collection<String> collection, String s) {
		return collection.stream().anyMatch((item) -> item.equalsIgnoreCase(s));
	}
	
	public static MessageEmbed createShardInfo(Bot shard) {
		EmbedBuilder embedBuilder = new EmbedBuilder();
		JDA jda = shard.getJDA();
		embedBuilder.setTitle("Shard #" + shard.getId());
		embedBuilder.addField("Total Uptime", DateUtils.format(System.currentTimeMillis() - shard.getStartup()), true);
		embedBuilder.addField("Last Reboot", DateUtils.format(System.currentTimeMillis() - shard.getLastReboot()), true);
		embedBuilder.addField("Last Event", DateUtils.format(System.currentTimeMillis() - shard.container.getLastEvents().get(shard.getId())), true);
		embedBuilder.addField("Event Manager Shutdown", String.valueOf(shard.getEventManager().executor.isShutdown()), true);
		embedBuilder.addField("Status", jda.getStatus().name(), true);
		embedBuilder.addField("General", "**Users:** " + jda.getUsers().size() + "\n**Guilds:** " + jda.getGuilds().size() + "\n**Audio Connections:** " + jda.getGuilds().stream().filter(guild -> guild.getAudioManager().isConnected()).count(), true);
		return embedBuilder.build();
	}
	
	public static <T> T deepCopy(T object, Class<T> type) {
		try {
			Gson gson = new GsonBuilder().serializeNulls().create();
			return gson.fromJson(gson.toJson(object, type), type);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}