package br.com.brjdevs.steven.bran.core.audio.utils;

import br.com.brjdevs.steven.bran.Bot;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeVideo {
	
	protected String id = null;
	protected String name = null;
	protected String duration = null;
	private String description = null;
	private String channelId = null;
	private String channelTitle = null;
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDuration() {
		return duration;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public String getCannelTitle() {
		return channelTitle;
	}
	
	public int getDurationHours() {
		Pattern pat = Pattern.compile("(\\d+)H");
		Matcher matcher = pat.matcher(duration);
		
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		} else {
			return 0;
		}
	}
	
	public int getDurationMinutes() {
		Pattern pat = Pattern.compile("(\\d+)M");
		Matcher matcher = pat.matcher(duration);
		
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		} else {
			return 0;
		}
	}
	
	public int getDurationSeconds() {
		Pattern pat = Pattern.compile("(\\d+)S");
		Matcher matcher = pat.matcher(duration);
		
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		} else {
			return 0;
		}
	}
	
	public String getDurationFormatted() {
		if (getDurationHours() == 0) {
			return forceTwoDigits(getDurationMinutes()) + ":" + forceTwoDigits(getDurationSeconds());
		} else {
			return forceTwoDigits(getDurationHours()) + ":" + forceTwoDigits(getDurationMinutes()) + ":" + forceTwoDigits(getDurationSeconds());
		}
	}
	
	public String getChannelUrl() {
		return "https://www.youtube.com/channel/" + channelId;
	}
	
	public String getChannelThumbUrl() {
		try {
			JSONObject json = Unirest.get("https://www.googleapis.com/youtube/v3/channels?part=snippet&fields=items(snippet/thumbnails)")
					.queryString("id", channelId)
					.queryString("key", Bot.getConfig().getGoogleKey())
					.asJson()
					.getBody()
					.getObject();
			
			return json.getJSONArray("items")
					.getJSONObject(0)
					.getJSONObject("snippet")
					.getJSONObject("thumbnails")
					.getJSONObject("default")
					.getString("url");
		} catch (UnirestException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String forceTwoDigits(int i) {
		if (i < 10) {
			return "0" + i;
		} else {
			return String.valueOf(i);
		}
	}
	
	@Override
	public String toString() {
		return "[YoutubeVideo:" + id + "]";
	}
	
}
