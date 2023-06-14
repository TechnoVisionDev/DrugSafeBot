package drugsafe.commands.information;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import drugsafe.DrugSafe;
import drugsafe.commands.Category;
import drugsafe.commands.Command;
import drugsafe.util.embeds.EmbedColor;
import drugsafe.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Command that displays harm reduction info for a substance as an embed
 *
 * @author TechnoVision
 */
public class InfoCommand extends Command {

    private final static OkHttpClient httpClient = new OkHttpClient();
    private final static Gson gson = new Gson();
    private final static DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    public InfoCommand(DrugSafe bot) {
        super(bot);
        this.name = "info";
        this.description = "View harm reduction info for substances";
        this.category = Category.INFORMATION;
        this.args.add(new OptionData(OptionType.STRING, "substance", "The substance to get info about").setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get substance name and query payload
        String query = event.getOption("substance").getAsString();
        JsonObject payload = new JsonObject();
        payload.addProperty("query", String.format(
                """
                        {
                            substances(query: "%s") {
                                name
                                url
                            	   class {
                                    chemical
                                    psychoactive
                                }
                            	   effects {
                                    name
                                }
                                roas {
                                    name
                                    dose {
                                        units
                                        threshold
                                        heavy
                                        common { min max }
                                        light { min max }
                                        strong { min max }
                                    }
                                    duration {
                                        afterglow { min max units }
                                        comeup { min max units }
                                        duration { min max units }
                                        offset { min max units }
                                        onset { min max units }
                                        peak { min max units }
                                        total { min max units }
                                    }
                                    bioavailability {
                                        min max
                                    }
                                }
                                addictionPotential
                                uncertainInteractions {
                                    name
                                }
                                unsafeInteractions {
                                    name
                                }
                            	   dangerousInteractions {
                                    name
                                }
                                tolerance {
                                    full
                                    half
                                    zero
                                }
                              	images {
                                    image
                                }
                            }
                        }""", query));

        // Fetch data for substance from API
        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), gson.toJson(payload));
        Request request = new Request.Builder().url("https://api.psychonautwiki.org/?").post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            // Check that request was successfully
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Get first substance from list
            JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray substances = jsonResponse.getAsJsonObject("data").getAsJsonArray("substances");
            if (substances.size() == 0) {
                // List was empty, send error embed
                String error = "The substance you entered does not exist! Try a different name.";
                event.replyEmbeds(EmbedUtils.createError(error)).setEphemeral(true).queue();
                return;
            }

            // Successfully retrieved substance data
            JsonObject firstSubstance = substances.get(0).getAsJsonObject();
            event.replyEmbeds(createInfoEmbed(firstSubstance)).queue();
        } catch (Exception e) {
            // Error occurred during request, send error embed
            String error = "An error occurred while trying to fetch data!";
            event.replyEmbeds(EmbedUtils.createError(error)).setEphemeral(true).queue();
        }
    }

    private MessageEmbed createInfoEmbed(JsonObject substance) throws Exception {
        // Build embed template
        String name = substance.get("name").getAsString();
        String url = substance.get("url").getAsString();
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Drug Information", null, "https://cdn-icons-png.flaticon.com/512/4320/4320344.png")
                .setTitle(name, url)
                .setColor(EmbedColor.DEFAULT.color);

        // Add image as thumbnail (if it exists)
        String image = getValidImage(substance.getAsJsonArray("images"));
        if (image != null) embed.setThumbnail(image);

        // Add drug class data (if it exists)
        String drugClass = "";
        JsonObject substanceClass = substance.getAsJsonObject("class");
        if (substanceClass.has("chemical") && !substanceClass.get("chemical").isJsonNull()) {
            drugClass += "**Chemical:** " + substanceClass.getAsJsonArray("chemical").get(0).getAsString();
        }
        if (substanceClass.has("psychoactive") && !substanceClass.get("psychoactive").isJsonNull()) {
            drugClass += "\n**Psychoactive:** " + substanceClass.getAsJsonArray("psychoactive").get(0).getAsString();
        }
        embed.addField(":telescope: __Class__", drugClass, true);

        // Add addiction potential (if it exists)
        String addictionPotential = substance.get("addictionPotential").getAsString();
        embed.addField(":warning: __Addiction Potential__", addictionPotential, false);

        // Add ROA and duration data (if it exists)
        try {
            JsonArray routesArray = substance.getAsJsonArray("roas");
            String routes = getRoutesData(routesArray);
            embed.addField(":scales: __Dosages__", routes, true);

            String duration = getDurationData(routesArray);
            embed.addField(":clock2: __Duration__", duration, true);
        } catch (Exception ignored) { }

        // Add tolerance data (if it exists)
        String tolerance = "";
        JsonObject toleranceObject = substance.getAsJsonObject("tolerance");
        if (toleranceObject.has("full") && !toleranceObject.get("full").isJsonNull()) {
            tolerance += "**Full:** " + toleranceObject.get("full").getAsString();
        }
        if (toleranceObject.has("half") && !toleranceObject.get("half").isJsonNull()) {
            tolerance += "\n**Half:** " + toleranceObject.get("half").getAsString();
        }
        if (toleranceObject.has("zero") && !toleranceObject.get("zero").isJsonNull()) {
            tolerance += "\n**Zero:** " + toleranceObject.get("zero").getAsString();
        }
        embed.addField(":chart_with_upwards_trend: __Tolerance__", tolerance, false);

        // Add important links
        String links = "[PsychonautWiki]("+url+")"
                    + " - [Effect Index](https://www.effectindex.com/)"
                    + " - [Drug Combinations](https://wiki.tripsit.me/images/3/3a/Combo_2.png)";
        embed.addField(":globe_with_meridians: __Links__", links, false);

        // Add footer
        embed.setFooter("Please use drugs responsibly", "https://cdn-icons-png.flaticon.com/512/4320/4320344.png");
        embed.setTimestamp(new Date().toInstant());

        return embed.build();
    }

    private String getRoutesData(JsonArray routesArray) {
        StringBuilder routes = new StringBuilder();
        for (JsonElement element : routesArray) {
            JsonObject route = element.getAsJsonObject();
            if (!route.has("dose") || route.get("dose").isJsonNull()) continue;

            String routeName = route.get("name").getAsString();
            routes.append("__(").append(routeName).append(")__\n");

            JsonObject doses = route.getAsJsonObject("dose");
            String units = doses.get("units").getAsString();
            appendDoseInfo(routes, doses, units, "threshold");
            appendDoseInfo(routes, doses, units, "light");
            appendDoseInfo(routes, doses, units, "common");
            appendDoseInfo(routes, doses, units, "strong");
            appendDoseInfo(routes, doses, units, "heavy");
            routes.append("\n");
        }
        return routes.toString();
    }

    private String getDurationData(JsonArray routesArray) {
        StringBuilder duration = new StringBuilder();
        for (JsonElement element : routesArray) {
            JsonObject route = element.getAsJsonObject();
            if (!route.has("duration") || route.get("duration").isJsonNull()) continue;

            String routeName = route.get("name").getAsString();
            duration.append("__(").append(routeName).append(")__\n");

            JsonObject durations = route.getAsJsonObject("duration");
            appendDurationInfo(duration, durations, "onset");
            appendDurationInfo(duration, durations, "comeup");
            appendDurationInfo(duration, durations, "peak");
            appendDurationInfo(duration, durations, "offset");
            appendDurationInfo(duration, durations, "afterglow");
            appendDurationInfo(duration, durations, "total");
            duration.append("\n");
        }
        return duration.toString();
    }

    private void appendDoseInfo(StringBuilder builder, JsonObject doses, String units, String doseType) {
        if (doses.has(doseType) && !doses.get(doseType).isJsonNull()) {
            builder.append("**").append(Character.toUpperCase(doseType.charAt(0))).append(doseType.substring(1)).append(":** ");
            JsonElement doseElement = doses.get(doseType);
            if (doseElement.isJsonObject()) {
                JsonObject doseRange = doseElement.getAsJsonObject();
                String min = DECIMAL_FORMAT.format(doseRange.get("min").getAsDouble());
                String max = DECIMAL_FORMAT.format(doseRange.get("max").getAsDouble());
                builder.append(min).append(" - ").append(max).append(units).append("\n");
            } else {
                String dose = DECIMAL_FORMAT.format(doseElement.getAsDouble());
                builder.append(dose).append(units).append("\n");
            }
        }
    }

    private void appendDurationInfo(StringBuilder builder, JsonObject durations, String durationType) {
        if (durations.has(durationType) && !durations.get(durationType).isJsonNull()) {
            builder.append("**").append(Character.toUpperCase(durationType.charAt(0))).append(durationType.substring(1)).append(":** ");
            JsonObject durationObject = durations.getAsJsonObject(durationType);
            String units = durationObject.get("units").getAsString();
            String min = DECIMAL_FORMAT.format(durationObject.get("min").getAsDouble());
            String max = DECIMAL_FORMAT.format(durationObject.get("max").getAsDouble());
            builder.append(min).append(" - ").append(max).append(" ").append(units).append("\n");
        }
    }

    private String getValidImage(JsonArray imagesArray) {
        for (JsonElement element :  imagesArray) {
            String image = element.getAsJsonObject().get("image").getAsString();
            if (image.endsWith(".png") || image.endsWith(".jpg") || image.endsWith(".jpeg")) {
                return image;
            }
        }
        return null;
    }
}
