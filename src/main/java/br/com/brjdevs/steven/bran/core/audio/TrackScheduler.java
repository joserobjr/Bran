package br.com.brjdevs.steven.bran.core.audio;

import br.com.brjdevs.steven.bran.Bot;
import br.com.brjdevs.steven.bran.core.audio.impl.TrackContextImpl;
import br.com.brjdevs.steven.bran.core.audio.utils.AudioUtils;
import br.com.brjdevs.steven.bran.core.utils.StringUtils;
import br.com.brjdevs.steven.bran.core.utils.Util;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.SimpleLog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TrackScheduler implements AudioEventListener {
	
	private static final SimpleLog LOG = SimpleLog.getLog("Track Scheduler");
	private static final String announce = "\uD83D\uDD0A Now Playing in **%s**: `%s` (`%s`) added by %s.";
	private static final Random rand = new Random();
	
	private AudioPlayer player;
	private boolean isRepeat;
	private boolean isShuffle;
	private TrackContext currentTrack;
	private TrackContext previousTrack;
	private Long guildId;
	private BlockingQueue<TrackContext> queue;
	private List<String> voteSkips;
	private int shard;
	private AtomicReference<Message> messageReference;
	
	public TrackScheduler(AudioPlayer player, Long guildId, int shard) {
		this.player = player;
		this.isRepeat = false;
		this.isShuffle = false;
		this.currentTrack = null;
		this.previousTrack = null;
		this.messageReference = new AtomicReference<>();
		this.guildId = guildId;
		this.voteSkips = new ArrayList<>();
		this.shard = shard;
		this.queue = new LinkedBlockingQueue<>();
	}
	
	public String getQueueDuration() {
		long duration = 0;
		duration += currentTrack.getTrack().getPosition();
		for (TrackContext track : queue)
			duration += track.getTrack().getInfo().length;
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
	
	public void setRepeat(boolean repeat) {
		this.isRepeat = repeat;
	}
	
	public boolean isShuffle() {
		return isShuffle;
	}
	
	public void setShuffle(boolean shuffle) {
		this.isShuffle = shuffle;
	}
	
	public TrackContext getCurrentTrack() {
		return currentTrack;
	}
	
	public TrackContext getPreviousTrack() {
		return previousTrack;
	}
	
	public Guild getGuild() {
		return getJDA().getGuildById(String.valueOf(guildId));
	}
	
	public BlockingQueue<TrackContext> getQueue() {
		return queue;
	}
	
	public int getPosition(TrackContext trackContext) {
		return new ArrayList<>(queue).indexOf(trackContext);
	}
	
	public TrackContext getByPosition(int index) {
		LinkedList<TrackContext> list = new LinkedList<>(queue);
		if (index >= list.size()) return null;
		return list.get(index);
	}
	
	public boolean isPaused() {
		return getAudioPlayer().isPaused();
	}
	
	public void setPaused(boolean paused) {
		getAudioPlayer().setPaused(paused);
	}
	
	public TrackContext provideNextTrack(boolean isSkipped) {
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
	}
	
	public JDA getJDA() {
		return Bot.getShard(shard);
	}
	
	public boolean isStopped() {
		return currentTrack == null && getQueue().isEmpty();
	}
	
	public TrackContext restartSong() {
		TrackContext context = currentTrack != null ? currentTrack.makeClone() : previousTrack.makeClone();
		if (context == null) return null;
		currentTrack = context;
		play(currentTrack, false);
		return currentTrack;
	}
	
	public boolean isContinuous() {
		return !isShuffle && !isRepeat;
	}
	
	public void play(TrackContext trackContext, boolean noInterrupt) {
		player.startTrack(trackContext != null ? trackContext.getTrack() : null, noInterrupt);
		if (isStopped())
			onSchedulerStop();
	}
	
	public void queue(AudioPlaylist playlist, List<TrackContext> trackContexts, User dj, TextChannel context) {
		Message msg = context.sendMessage("Found playlist `" + playlist.getName() + "` with `" + playlist.getTracks().size() + "` tracks, give me a second to queue them...").complete();
		for (TrackContext trackContext : trackContexts) {
			if (getQueue().size() > 600) {
				context.sendMessage("Queue has reached its limit! (" + 600 + ")").queue();
				return;
			}
			queue.offer(trackContext);
		}
		msg.editMessage(dj.getAsMention() + " has added the Playlist `" + playlist.getName() + "` (`" + AudioUtils.format(AudioUtils.getLength(playlist)) + "`)").queue();
		if (player.getPlayingTrack() == null)
			play(provideNextTrack(false), true);
	}
	
	public void queue(TrackContext context) {
		queue.offer(context);
		AudioTrackInfo info = context.getTrack().getInfo();
		if (context.getDJ(getJDA()) != null && context.getContext(getJDA()) != null)
			context.getContext(getJDA()).sendMessage(context.getDJ(getJDA()).getAsMention() + " has added `" + info.title + "` to the queue. (`" + AudioUtils.format(info.length) + "`)").queue();
		if (player.getPlayingTrack() == null)
			play(provideNextTrack(false), true);
	}
	
	public void silentQueue(TrackContext context) {
		queue.offer(context);
		if (player.getPlayingTrack() == null)
			play(provideNextTrack(false), true);
	}
	
	public void queue(AudioTrack track, String url, User user, TextChannel channel) {
		queue(new TrackContextImpl(track, url, user, channel));
	}
	
	public List<TrackContext> getTracksBy(User user) {
		return queue.stream()
				.filter(track -> track.getDJId().equals(user.getId())).collect(Collectors.toList());
	}
	
	public void stop() {
		queue.clear();
		play(provideNextTrack(true), false);
		getGuild().getAudioManager().closeAudioConnection();
	}
	
	public int getRequiredVotes() {
		int listeners = (int) getVoiceChannel().getMembers().stream()
				.filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened()).count();
		return (int) Math.ceil(listeners * .55);
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
	
	public LinkedList<TrackContext> getRemainingTracks() {
		LinkedList<TrackContext> tracks = new LinkedList<>();
		tracks.add(currentTrack);
		tracks.addAll(queue);
		return tracks;
	}
	
	public VoiceChannel getVoiceChannel() {
		return getGuild().getAudioManager().getConnectedChannel();
	}
	
	@Override
	public void onEvent(AudioEvent audioEvent) {
		if (audioEvent instanceof TrackEndEvent) {
			voteSkips.clear();
			TrackEndEvent event = (TrackEndEvent) audioEvent;
			AudioTrackEndReason endReason = event.endReason;
			if (endReason.mayStartNext) {
				play(provideNextTrack(false), false);
			}
		} else if (audioEvent instanceof TrackStartEvent) {
			TrackStartEvent event = (TrackStartEvent) audioEvent;
			if (currentTrack == null && getAudioPlayer().getPlayingTrack() != null) {
				LOG.fatal("Got TrackStartEvent with null AudioTrackContext in Guild " + guildId + ", finished session.");
				AudioUtils.getManager().getMusicManagers().remove(guildId);
				return;
			}
			if (currentTrack.getContext(getJDA()) != null && !isRepeat) {
				currentTrack.getContext(getJDA()).sendMessage(String.format(announce, getVoiceChannel().getName(), currentTrack.getTrack().getInfo().title, AudioUtils.format(currentTrack.getTrack()), Util.getUser(currentTrack.getDJ(getJDA())))).queue(message -> {
					Message msg = messageReference.get();
					if (msg != null && !msg.isEdited()) msg.deleteMessage().queue();
					messageReference.set(message);
				});
			}
		} else if (audioEvent instanceof TrackExceptionEvent) {
			TrackExceptionEvent event = (TrackExceptionEvent) audioEvent;
			if (currentTrack != null && currentTrack.getContext(getJDA()) != null
					&& currentTrack.getContext(getJDA()).canTalk()) {
				if (currentTrack.getContext(getJDA()) != null && currentTrack.getContext(getJDA()).canTalk()) {
					String string = "\u274c Failed to Load `" + currentTrack.getTrack().getInfo().title + "`!\n" +
							(event.exception.severity.equals(Severity.COMMON) ? StringUtils.neat(event.track.getSourceManager().getSourceName()) + " said: " : event.exception.severity.equals(Severity.SUSPICIOUS) ? "I don't know what exactly caused it, but I've got this: " : "This error might be caused by the library (Lavaplayer) or an external unidentified factor: ") + "`" + event.exception.getMessage() + "`";
					Message msg = messageReference.get();
					if (msg != null)
						msg.editMessage(string).queue();
					else
						currentTrack.getContext(getJDA()).sendMessage(string).queue();
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
			LOG.fatal("Got onSchedulerStop with currentTrack not null.");
			return;
		}
		if (getPreviousTrack() != null && getPreviousTrack().getContext(getJDA()) != null && getPreviousTrack().getContext(getJDA()).canTalk())
			getPreviousTrack().getContext(getJDA()).sendMessage("Finished playing queue, disconnecting... If you want to play more music use `" + Bot.getDefaultPrefixes()[0] + "music play [SONG]`.").queue();
		getGuild().getAudioManager().closeAudioConnection();
		//AudioUtils.getManager().unregister(guildId);
	}
}
