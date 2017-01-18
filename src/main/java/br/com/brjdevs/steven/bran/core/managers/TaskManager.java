package br.com.brjdevs.steven.bran.core.managers;

import br.com.brjdevs.steven.bran.Bot;
import br.com.brjdevs.steven.bran.core.audio.MusicManager;
import br.com.brjdevs.steven.bran.core.audio.TrackContext;
import br.com.brjdevs.steven.bran.core.audio.TrackScheduler;
import br.com.brjdevs.steven.bran.core.audio.utils.AudioUtils;
import com.google.gson.JsonObject;
import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static br.com.brjdevs.steven.bran.core.audio.utils.VoiceChannelListener.musicTimeout;

public class TaskManager {
    private static final ExecutorService pools =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5);

    public static ExecutorService getThreadPool() {
        return pools;
    }
    public static void startAsyncTask(Runnable run, int seconds) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                run, 0, seconds, TimeUnit.SECONDS);
    }
    public static void startAsyncTasks() {
        final OperatingSystemMXBean os =
                ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());
	    startAsyncTask(
			    () ->
					    Bot.getSession().cpuUsage = (Math.floor(os.getProcessCpuLoad() * 10000) / 100)
			    , 5);
        startAsyncTask(() -> musicTimeout.entrySet().forEach(entry -> {
            try {
	            JDA jda = Bot.getShard(entry.getValue().getAsJsonObject().get("shard").getAsInt());
	            if (jda == null) {
		            Bot.LOG.fatal("Got JDA null in TaskManager.java. Shard: " + entry.getValue().getAsJsonObject().get("shard").getAsInt());
		            return;
	            }
	            Guild guild = jda.getGuildById(entry.getKey());
                if (guild == null) {
                    musicTimeout.remove(entry.getKey());
                    return;
                }
                JsonObject info = musicTimeout.get(guild.getId()).getAsJsonObject();
	            info.addProperty("timeout", info.get("timeout").getAsInt() - 10);
	            musicTimeout.remove(guild.getId());
                if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect())
                    return;
                if (info.get("timeout").getAsInt() == 0) {
	                MusicManager player = AudioUtils.getManager().get(guild);
	                VoiceChannel channel = guild.getVoiceChannelById(info.get("channelId").getAsString());
                    if (channel == null || !AudioUtils.isAlone(channel)) return;
	                TrackScheduler scheduler = player.getTrackScheduler();
	                TrackContext track = scheduler.getCurrentTrack();
	                scheduler.getQueue().clear();
	                scheduler.play(player.getTrackScheduler().provideNextTrack(false), false);
	                if (track.getContext(jda) != null && track.getContext(jda).canTalk())
                        track.getContext(jda).sendMessage("Nobody rejoined in 2 minutes, so I cleaned the queue and stopped the player.").queue();
                    player.getPlayer().setPaused(false);
                    if (guild.getAudioManager().isConnected())
                        guild.getAudioManager().closeAudioConnection();
                    return;
                }
                musicTimeout.add(guild.getId(), info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }), 10);
        /*startAsyncTask(() -> {
            try {
                for (int i = 0; i != Choice.getChoices().size() - 1; i++) {
                    if (Choice.getChoices().isEmpty()) break;
                    Choice c = Choice.getChoices().get(i);
                    if (c.getCreationInMillis() >= (System.currentTimeMillis() + 12000)) continue;
                    if (c.getMessage() != null)
                        c.getMessage().deleteMessage().queue();
                    c.getChannel().sendTyping().queue(success -> c.getChannel().sendMessage("You took to long to pick a song, query canceled!").queue());
                    c.remove();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 10);*/
        //startAsyncTask(() -> );
    }
}
