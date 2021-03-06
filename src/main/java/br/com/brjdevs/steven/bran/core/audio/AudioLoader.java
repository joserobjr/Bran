package br.com.brjdevs.steven.bran.core.audio;

import br.com.brjdevs.steven.bran.core.action.Action;
import br.com.brjdevs.steven.bran.core.action.Action.onInvalidResponse;
import br.com.brjdevs.steven.bran.core.action.ActionType;
import br.com.brjdevs.steven.bran.core.audio.impl.TrackContextImpl;
import br.com.brjdevs.steven.bran.core.audio.utils.AudioUtils;
import br.com.brjdevs.steven.bran.core.utils.MathUtils;
import br.com.brjdevs.steven.bran.core.utils.Util;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AudioLoader implements AudioLoadResultHandler {
	
	public static final long MAX_SONG_LENGTH = 10800000;
	public static final long MAX_PLAYLIST_LENGTH = 108000000;
	public static final int MAX_QUEUE_SIZE = 600;
	
	private TextChannel channel;
	private User user;
	private String trackUrl;
	private MusicManager musicManager;
	
	public AudioLoader(TextChannel channel, User user, String trackUrl, MusicManager musicManager) {
		this.channel = channel;
		this.user = user;
		this.trackUrl = trackUrl;
		this.musicManager = musicManager;
	}
	
	@Override
	public void trackLoaded(AudioTrack track) {
		if (track.getInfo().length > MAX_SONG_LENGTH) {
			channel.sendMessage("This song is too long! The maximum supported length is 3 hours. *" + AudioUtils.format(track) + "/" + AudioUtils.format(MAX_SONG_LENGTH) + "*").queue();
			return;
		}
		if (musicManager.getTrackScheduler().getQueue().size() > MAX_QUEUE_SIZE) {
			channel.sendMessage("Queue has reached its limit! (" + MAX_QUEUE_SIZE + ")").queue();
			return;
		}
		String url = !track.getSourceManager().getSourceName().equalsIgnoreCase("youtube") ? trackUrl : "https://www.youtube.com/watch?v=" + track.getInfo().identifier;
		musicManager.getTrackScheduler().queue(track, url, user, channel);
	}
	
	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		if (musicManager.getTrackScheduler().getQueue().size() > MAX_QUEUE_SIZE) {
			channel.sendMessage("Queue has reached its limit! (" + MAX_QUEUE_SIZE + ")").queue();
			return;
		}
		if (playlist.getTracks().isEmpty()) {
			channel.sendMessage("This playlist is empty!").queue();
		}
		if (playlist.getSelectedTrack() != null) {
			trackLoaded(playlist.getSelectedTrack());
		} else {
			List<TrackContext> playlistTracks = playlist.getTracks().stream().map(track -> new TrackContextImpl(track, !track.getSourceManager().getSourceName().equalsIgnoreCase("youtube") ? trackUrl : "https://www.youtube.com/watch?v=" + track.getInfo().identifier, user, channel)).collect(Collectors.toList());
			if (playlist.isSearchResult()) {
				List<TrackContext> tracks = playlistTracks.stream().filter(track -> playlistTracks.indexOf(track) < 3).collect(Collectors.toList());
				channel.sendMessage(Util.getUser(user) + ", results found by `" + trackUrl.substring(9).trim() + "`:\n" + String.join("\n", tracks.stream().map(track -> "`[" + (tracks.indexOf(track) + 1) + "]` " + track.getTrack().getInfo().title + " (`" + AudioUtils.format(track.getTrack().getInfo().length) + "`)").collect(Collectors.toList())) + "\n*This will expire in 20 seconds*").queue(msg -> {
					List<String> inputs = new ArrayList<>();
					inputs.add("c");
					inputs.add("cancel");
					for (int i = tracks.size(); i > 0; i--) {
						inputs.add(String.valueOf(i));
					}
					Action action = new Action(ActionType.MESSAGE, onInvalidResponse.CONTINUE, msg, (message, args) -> {
						if (channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE))
							message.deleteMessage().queue();
						String response = message.getContent();
						if (MathUtils.isInRange(MathUtils.parseIntOrDefault(response, -1), 0, inputs.size() - 1)) {
							int i = Integer.parseInt(response);
							TrackContext trackContext = tracks.get(i - 1);
							musicManager.getTrackScheduler().queue(trackContext);
							if (msg != null)
								msg.deleteMessage().queue();
						} else {
							if (msg != null)
								msg.editMessage("Query canceled!").queue();
							else
								channel.sendMessage("Query canceled!").queue();
							if (musicManager.getTrackScheduler().isStopped())
								channel.getGuild().getAudioManager().closeAudioConnection();
						}
					}, inputs, musicManager.container, TimeUnit.SECONDS.toMillis(20));
					action.addUser(user);
				});
				return;
			}
			long duration = AudioUtils.getLength(playlist);
			if (duration > MAX_PLAYLIST_LENGTH) {
				channel.sendMessage("This playlist is too long! The maximum supported length is 30 hours. *" + AudioUtils.format(duration) + "/" + AudioUtils.format(MAX_PLAYLIST_LENGTH) + "*").queue();
				return;
			}
			musicManager.getTrackScheduler().queue(playlist, playlistTracks, user, channel);
		}
	}
	
	@Override
	public void noMatches() {
		channel.sendMessage("Nothing found by `" + (trackUrl.startsWith("ytsearch:") ? trackUrl.substring(9).trim() : trackUrl) + "`").queue();
		if (musicManager.getTrackScheduler().isStopped() && channel.getGuild().getAudioManager().isConnected())
			channel.getGuild().getAudioManager().closeAudioConnection();
	}
	
	@Override
	public void loadFailed(FriendlyException exception) {
		channel.sendMessage("Could not play the requested song. `" + exception.getMessage() + " (" + exception.severity + ")`").queue();
		if (musicManager.getTrackScheduler().isStopped())
			channel.getGuild().getAudioManager().closeAudioConnection();
	}
}
