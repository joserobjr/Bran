package br.com.brjdevs.bran.core.audio;

import br.com.brjdevs.bran.Bot;
import br.com.brjdevs.bran.core.audio.impl.TrackContextImpl;
import br.com.brjdevs.bran.core.audio.utils.AudioUtils;
import br.com.brjdevs.bran.core.utils.StringUtils;
import br.com.brjdevs.bran.core.utils.Util;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler implements AudioEventListener {
	
	private static final String announce = "\uD83D\uDD0A Now Playing in **%s**: `%s` (`%s`) added by %s.";
	private static final Random rand = new Random();
	
	private AudioPlayer player;
	private boolean isRepeat;
	private boolean isShuffle;
	private TrackContext currentTrack;
	private TrackContext previousTrack;
	private String lastAnnounce;
	private Long guildId;
	private BlockingQueue<TrackContext> queue;
	private List<String> voteSkips;
	private int shard;
	
	public TrackScheduler(AudioPlayer player, Long guildId, int shard) {
		this.player = player;
		this.isRepeat = false;
		this.isShuffle = false;
		this.currentTrack = null;
		this.previousTrack = null;
		this.lastAnnounce = null;
		this.guildId = guildId;
		this.voteSkips = new ArrayList<>();
		this.shard = shard;
		this.queue = new LinkedBlockingQueue<>();
	}
	
	public String getQueueDuration() {
		long duration = 0;
		duration += currentTrack.getOrigin().getPosition();
		for (TrackContext track : queue)
			duration += track.getOrigin().getInfo().length;
		return AudioUtils.format(duration);
	}
	public List<String> getVoteSkips() {
		return voteSkips;
	}
	
	public AudioPlayer getAudioPlayer() {
		return player;
	}
	
	public boolean isRepeat() {
		return isRepeat;
	}
	
	public boolean isShuffle() {
		return isShuffle;
	}
	
	public TrackContext getCurrentTrack() {
		return currentTrack;
	}
	
	public TrackContext getPreviousTrack() {
		return previousTrack;
	}
	
	public Guild getGuild(JDA jda) {
		return jda.getGuildById(String.valueOf(guildId));
	}
	
	public BlockingQueue<TrackContext> getQueue() {
		return queue;
	}
	
	public int getPosition(TrackContext trackContext) {
		return new ArrayList<>(queue).indexOf(trackContext);
	}
	
	public TrackContext provideNextTrack(boolean isSkipped) {
		try {
			if (isRepeat && !isSkipped && currentTrack != null) {
				return currentTrack.makeClone();
			}
			if (isShuffle && !queue.isEmpty()) {
				int picked = rand.nextInt(queue.size());
				int current = 0;
				for (TrackContext trackContext : queue) {
					if (picked == current) {
						if (currentTrack != null) previousTrack = currentTrack;
						currentTrack = trackContext;
						break;
					}
					current++;
				}
				queue.remove(currentTrack);
				return currentTrack;
			} else {
				if (currentTrack != null) previousTrack = currentTrack;
				currentTrack = queue.poll();
				return currentTrack;
			}
		} finally {
			if (isStopped())
				onSchedulerStop();
		}
	}
	
	public JDA getJDA() {
		return Bot.getInstance().getShard(shard);
	}
	
	public void setLastAnnounce(Message message) {
		if (message == null) {
			lastAnnounce = null;
			return;
		}
		this.lastAnnounce = message.getId();
	}
	
	public boolean isStopped() {
		return currentTrack == null && getQueue().isEmpty();
	}
	
	public void setRepeat(boolean repeat) {
		this.isRepeat = repeat;
	}
	
	public void setShuffle(boolean shuffle) {
		this.isShuffle = shuffle;
	}
	
	public TrackContext restartSong() {
		if (currentTrack != null) {
			currentTrack.getOrigin().setPosition(0);
		} else if (previousTrack != null) {
			play(previousTrack.makeClone(), false);
		}
		return currentTrack;
	}
	
	public boolean isContinuous() {
		return !isShuffle && !isRepeat;
	}
	
	public boolean play(TrackContext trackContext, boolean noInterrupt) {
		return player.startTrack(trackContext != null ? trackContext.getOrigin() : null, noInterrupt);
	}
	
	public void queue(AudioPlaylist playlist, Map<AudioTrack, String> map, User dj, TextChannel context) {
		context.sendMessage("Found playlist `" + playlist.getName() + "`, loading `" + playlist.getTracks().size() + "` tracks...").queue();
		map.forEach((track, string) -> queue.offer(new TrackContextImpl(track, string, dj, context)));
		context.sendMessage("Done! Queued `" + playlist.getTracks().size() + "` tracks!").queue();
		if (player.getPlayingTrack() == null)
			play(provideNextTrack(false), true);
	}
	
	public void queue(TrackContext context) {
		queue.offer(context);
		AudioTrackInfo info = context.getOrigin().getInfo();
		if (context.getDJ(getJDA()) != null && context.getContext(getJDA()) != null)
			context.getContext(getJDA()).sendMessage(context.getDJ(getJDA()).getAsMention() + " has added `" + info.title + "` to the queue. (`" + AudioUtils.format(info.length) + "`)").queue();
		if (player.getPlayingTrack() == null)
			play(provideNextTrack(false), true);
	}
	
	public void queue(AudioTrack track, String url, User user, TextChannel channel) {
		queue(new TrackContextImpl(track, url, user, channel));
	}
	
	public void stop() {
		getGuild(getJDA()).getAudioManager().closeAudioConnection();
		queue.clear();
		play(null, false);
	}
	
	public int getRequiredVotes(VoiceChannel voiceChannel) {
		return (int) (voiceChannel.getMembers().size() - 1 / .65 + 1);
	}
	
	public void skip() {
		voteSkips.clear();
		play(provideNextTrack(true), false);
	}
	
	public TrackContext removeAt(int index) {
		if (getQueue().size() < index) return null;
		else {
			int current = 0;
			for (TrackContext track : queue) {
				if (index == current) {
					queue.remove(track);
					return track;
				}
				current++;
			}
		}
		return null;
	}
	
	public VoiceChannel getVoiceChannel() {
		return getGuild(getJDA()).getAudioManager().getConnectedChannel();
	}
	
	@Override
	public void onEvent(AudioEvent audioEvent) {
		if (audioEvent instanceof TrackEndEvent) {
			voteSkips.clear();
			TrackEndEvent event = (TrackEndEvent) audioEvent;
			AudioTrackEndReason endReason = event.endReason;
			if (!isRepeat && lastAnnounce != null) {
				currentTrack.getContext(getJDA()).getMessageById(lastAnnounce).queue(msg -> {
					if (msg.getEditedTime() == null) msg.deleteMessage().queue();
					setLastAnnounce(null);
				}, throwable -> setLastAnnounce(null));
			}
			if (endReason.mayStartNext) {
				TrackContext nextTrack = provideNextTrack(false);
				play(nextTrack, false);
			}
		} else if (audioEvent instanceof TrackStartEvent) {
			TrackStartEvent event = (TrackStartEvent) audioEvent;
			if (currentTrack == null && getAudioPlayer().getPlayingTrack() != null) {
				Bot.LOG.fatal("Got TrackStartEvent with null AudioTrackContext.");
				if (!getQueue().isEmpty()) {
					provideNextTrack(true);
					if (currentTrack == null) {
						if (previousTrack != null) {
							if (previousTrack.getContext(getJDA()) != null && previousTrack.getContext(getJDA()).canTalk()) {
								previousTrack.getContext(getJDA()).sendMessage("A fatal error has occurred, the current session has been terminated. I'm sorry for this, but a synchronization and irreversible error has \"nulled\" the TrackScheduler current track.");
							} else {
								if (getGuild(getJDA()).getPublicChannel().canTalk()) {
									getGuild(getJDA()).getPublicChannel().sendMessage("A fatal error has occurred, the current session has been terminated. I'm sorry for this, but a synchronization and irreversible error has \"nulled\" the TrackScheduler current track.").queue();
								}
							}
						}
					}
				}
				return;
			}
			if (currentTrack.getContext(getJDA()) != null && currentTrack.getDJ(getJDA()) != null && !isRepeat) {
				currentTrack.getContext(getJDA()).sendMessage(String.format(announce, getVoiceChannel().getName(), currentTrack.getOrigin().getInfo().title, AudioUtils.format(currentTrack.getOrigin()), Util.getUser(currentTrack.getDJ(getJDA())))).queue();
			}
		} else if (audioEvent instanceof TrackExceptionEvent) {
			TrackExceptionEvent event = (TrackExceptionEvent) audioEvent;
			if (currentTrack != null && currentTrack.getContext(getJDA()) != null
					&& currentTrack.getContext(getJDA()).canTalk()) {
				if (currentTrack.getContext(getJDA()) != null && currentTrack.getContext(getJDA()).canTalk()) {
					String string = "\u274c Failed to Load `" + currentTrack.getOrigin().getInfo().title + "`!\n" +
							(event.exception.severity.equals(Severity.COMMON) ? StringUtils.neat(event.track.getSourceManager().getSourceName()) + " said: " : event.exception.severity.equals(Severity.SUSPICIOUS) ? "I don't know what exactly caused it, but I've got this: " : "This error might be caused by the library (Lavaplayer) or an external unidentified factor: ") + "`" + event.exception.getMessage() + "`";
					currentTrack.getContext(getJDA()).getMessageById(lastAnnounce).queue(msg -> msg.editMessage(string).queue(), throwable -> currentTrack.getContext(getJDA()).sendMessage(string).queue());
				}
			}
		} else if (audioEvent instanceof TrackStuckEvent) {
			TrackStuckEvent event = (TrackStuckEvent) audioEvent;
			if (currentTrack != null && currentTrack.getContext(getJDA()) != null)
				currentTrack.getContext(getJDA()).sendMessage("Track got stuck, skipping...").queue();
			play(provideNextTrack(true), false);
		}
	}
	
	private void onSchedulerStop() {
		if (getCurrentTrack() != null) {
			Bot.LOG.fatal("Got onSchedulerStop with current AudioTrackContext not null.");
			return;
		}
		if (getPreviousTrack() != null)
			getPreviousTrack().getContext(getJDA()).sendMessage("Finished playing queue, disconnecting... If you want to play more music use `" + Bot.getInstance().getDefaultPrefixes()[0] + "music play [SONG]`.").queue();
		getGuild(getJDA()).getAudioManager().closeAudioConnection();
	}
}