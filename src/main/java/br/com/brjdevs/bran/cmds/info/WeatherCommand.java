package br.com.brjdevs.bran.cmds.info;

import br.com.brjdevs.bran.core.Permissions;
import br.com.brjdevs.bran.core.WeatherSearch;
import br.com.brjdevs.bran.core.command.Category;
import br.com.brjdevs.bran.core.command.CommandBuilder;
import br.com.brjdevs.bran.core.command.CommandManager;
import br.com.brjdevs.bran.core.command.RegisterCommand;
import br.com.brjdevs.bran.core.utils.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.core.EmbedBuilder;

import java.awt.*;

@RegisterCommand
public class WeatherCommand {
	public WeatherCommand() {
		CommandManager.addCommand(new CommandBuilder(Category.INFORMATIVE)
				.setAliases("weather")
				.setName("Weather Command")
				.setDescription("Gives you weather information on a place.")
				.setArgs("<location>")
				.setRequiredPermission(Permissions.BOT_ADMIN)
				.setAction((event) -> {
					String query = event.getArgs(2)[1];
					JsonElement element;
					try {
						element = WeatherSearch.search(query);
					} catch (RuntimeException e) {
						if (e.getMessage().equals("Yahoo API didn't respond."))
							event.sendMessage("The API took too long to respond, maybe it's offline?").queue();
						else
							event.sendMessage("Could not connect, try again later please.").queue();
						return;
					}
					try {
						if (element.isJsonNull()) {
							event.sendMessage("Nothing found by `" + query + "`.").queue();
							return;
						}
						JsonObject result = element.getAsJsonObject();
						if (result == null) {
							event.sendMessage("Nothing found by `" + query + "`").queue();
							return;
						}
						result = result.get("channel").getAsJsonObject();
						//embed title
						JsonObject locationObject = result
								.get("location")
								.getAsJsonObject();
						String location = locationObject.get("city").getAsString() + " " + locationObject.get("region").getAsString() + ", " + locationObject.get("country").getAsString();
						//wind speed field
						JsonObject windObject = result.get("wind").getAsJsonObject();
						String windSpeed = windObject.get("speed").getAsString() + " mph";
						String windDirecton = windObject.get("direction").getAsString();
						String windChill = windObject.get("chill").getAsString();
						//atmosphere field
						JsonObject atmosphereObject = result.get("atmosphere").getAsJsonObject();
						String humidity = atmosphereObject.get("humidity").getAsString() + "%";
						String pressure = atmosphereObject.get("pressure").getAsString() + " in";
						//temp stuff
						JsonObject item = result.get("item").getAsJsonObject();
						JsonObject conditionObject = item.get("condition").getAsJsonObject();
						String lastUpdate = conditionObject.get("date").getAsString();
						double f = Double.parseDouble(conditionObject.get("temp").getAsString());
						String c = Util.DECIMAL_FORMAT.format((5./9.) * (f - 32.));
						String temp = Util.DECIMAL_FORMAT.format(f) + "ºF/" + c + "ºC";
						String text = conditionObject.get("text").getAsString();
						
						EmbedBuilder embedBuilder = new EmbedBuilder();
						embedBuilder.setTitle("Weather for " + location);
						embedBuilder.addField("Last updated at", lastUpdate, false);
						embedBuilder.addField("Wind", "**Speed:** " + windSpeed + "     **Direction:** " + windDirecton + "     **Chill:** " + windChill + "\n", false);
						embedBuilder.addField("Atmosphere", "**Humidity**: " + humidity + "     **Pressure:** " + pressure + "\n", false);
						embedBuilder.addField("Weather", "**Temperature:** " + temp + "     **Description:** " + text, false);
						
						embedBuilder.setColor(event.getSelfMember().getColor() == null ? Color.decode("#F1AC1A") : event.getSelfMember().getColor());
						event.sendMessage(embedBuilder.build()).queue();
					} catch (Exception e) {
						event.sendMessage("There was a parsing error while building the info. `" + e.getMessage() + "`").queue();
						e.printStackTrace();
						return;
					}
				})
				.build());
	}
}
