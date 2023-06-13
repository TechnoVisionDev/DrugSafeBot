package drugsafe.commands.logging;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import drugsafe.DrugSafe;
import drugsafe.commands.Category;
import drugsafe.commands.Command;
import drugsafe.data.Database;
import drugsafe.data.logs.Entry;
import drugsafe.data.logs.Log;
import drugsafe.listeners.PaginationListener;
import drugsafe.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;

/**
 * Command that logs a drug dose to the user's log.
 *
 * @author TechnoVision
 */
public class LogCommand extends Command {

    public LogCommand(DrugSafe bot) {
        super(bot);
        this.name = "log";
        this.description = "Log commands";
        this.category = Category.LOGGING;
        this.autocomplete = Arrays.asList("Alcohol", "Amphetamine", "Benzodiazepines", "Cannabis", "Cocaine", "DMT", "DXM", "GBL", "GHB", "Ketamine", "Heroin", "Kratom", "LSD", "MDMA", "Methamphetamine", "Mushrooms", "Modafinil", "Nicotine", "Oxycodone", "2C-B");

        // Add subcommand
        this.subCommands.add(new SubcommandData("add", "Add a new drug dose to your log")
                .addOptions(
                        new OptionData(OptionType.STRING, "drug", "The name of the drug taken", true).setAutoComplete(true),
                        new OptionData(OptionType.NUMBER, "dose", "The amount of the drug taken", true),
                        new OptionData(OptionType.STRING, "units", "The unit measured in", true)
                                .addChoice("Micrograms (μg)", "μg")
                                .addChoice("Milligrams (mg)", "mg")
                                .addChoice("Grams (g)", "g")
                                .addChoice("Milliliters (mL)", "mL")
                                .addChoice("Drinks", "drinks")
                                .addChoice("Other", "other"),
                        new OptionData(OptionType.STRING, "route", "The route of administration", true)
                                .addChoice("Oral", "oral")
                                .addChoice("Smoked", "smoked")
                                .addChoice("Insufflated", "insufflated")
                                .addChoice("Rectal", "rectal")
                                .addChoice("Intravenous", "intravenous")
                                .addChoice("Other", "other"),
                        new OptionData(OptionType.BOOLEAN, "hide", "Set to true if you want to hide reply from others", false)
                )
        );

        // View subcommand
        this.subCommands.add(new SubcommandData("view", "View your full dose log")
                .addOptions(
                        new OptionData(OptionType.USER, "user", "See another user's log", false),
                        new OptionData(OptionType.INTEGER, "year", "Specify a year to view logged doses", false).setMinValue(2023)
                )
        );

        // Remove subcommand
        this.subCommands.add(new SubcommandData("remove", "Remove a dose by ID from your log")
                .addOptions(new OptionData(OptionType.INTEGER, "id", "The ID of the logged dose", true).setMinValue(1))
                .addOptions(new OptionData(OptionType.INTEGER, "year", "Specify the year to remove logged dose from", false).setMinValue(2023))
        );

        // Reset subcommand
        this.subCommands.add(new SubcommandData("reset", "Reset your entire log or a specified year")
                .addOptions(new OptionData(OptionType.INTEGER, "year", "Specify which year to reset log data", false).setMinValue(2023))
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        switch(event.getSubcommandName()) {
            case "add" -> executeAdd(event);
            case "view" -> executeView(event);
            case "remove" -> executeRemove(event);
            case "reset" -> executeReset(event);
        }
    }

    /**
     * Adds a new dose to the user's log.
     */
    private void executeAdd(SlashCommandInteractionEvent event) {
        // Get command data and create new entry
        long userID = event.getUser().getIdLong();
        String drug = event.getOption("drug").getAsString();
        double dose = event.getOption("dose").getAsDouble();
        String units = event.getOption("units").getAsString();
        String route = event.getOption("route").getAsString();
        Entry entry = new Entry(drug, dose, units, route);

        // Update log in database
        Bson filter = Filters.eq("user", userID);
        Bson update = Updates.addToSet("doses." + DrugSafe.getCurrentYear(), entry);
        bot.database.logs.updateOne(filter, update, Database.UPSERT);

        // Reply with embed (ephemeral if hidden)
        OptionMapping hide = event.getOption("hide");
        boolean isEphemeral = hide != null && hide.getAsBoolean();
        event.replyEmbeds(entry.getEmbed(userID).build()).setEphemeral(isEphemeral).queue();
    }

    /**
     * Shows the user's log as an embed in chat.
     */
    private void executeView(SlashCommandInteractionEvent event) {
        // Get user
        OptionMapping userOption = event.getOption("user");
        User user = (userOption != null) ? userOption.getAsUser() : event.getUser();

        // Get year
        OptionMapping yearOption = event.getOption("year");
        String year = (yearOption != null) ? yearOption.getAsString() : DrugSafe.getCurrentYear();

        // Get log from database (with error checking)
        Log log = bot.database.logs.find(Filters.eq("user", user.getIdLong())).first();
        if (log == null || log.getDoses().isEmpty()) {
            // Error: User does not yet have any data logged
            String error = (user.getIdLong() == event.getUser().getIdLong()) ? "You have not yet logged any doses!" : "The user <@"+user.getIdLong()+"> has not yet logged any doses!";
            event.replyEmbeds(EmbedUtils.createError(error)).setEphemeral(true).queue();
            return;
        } else if (!log.getDoses().containsKey(year) || log.getDoses().get(year).isEmpty()) {
            // Error: year does not yet have any data logged
            event.replyEmbeds(EmbedUtils.createError("The year **"+year+"** does not yet have any logged doses!")).setEphemeral(true).queue();
            return;
        }

        // Send paginated log
        List<MessageEmbed> embeds = log.getEmbed(user, year);
        ReplyCallbackAction action = event.replyEmbeds(embeds.get(0));
        if (embeds.size() == 1) { action.queue(); }
        else { PaginationListener.sendPaginatedMenu(user.getId(), action, embeds); }
    }

    /**
     * Removes a logged dose from the user's log by ID.
     */
    private void executeRemove(SlashCommandInteractionEvent event) {
        // Get command data
        int index = event.getOption("id").getAsInt() - 1;
        long userID = event.getUser().getIdLong();

        // Get year
        OptionMapping yearOption = event.getOption("year");
        String year = (yearOption != null) ? yearOption.getAsString() : DrugSafe.getCurrentYear();

        // Get log from database
        Bson filter = Filters.eq("user", userID);
        Log log = bot.database.logs.find(filter).first();

        // Error checking
        if (log == null || log.getDoses().isEmpty()) {
            event.replyEmbeds(EmbedUtils.createError("You have not yet logged any doses!")).setEphemeral(true).queue();
            return;
        } else if (!log.getDoses().containsKey(year) || log.getDoses().get(year).isEmpty()) {
            event.replyEmbeds(EmbedUtils.createError("The year **"+year+"** does not yet have any logged doses!")).setEphemeral(true).queue();
            return;
        } else if (log.getDoses().get(year).size() <= index) {
            event.replyEmbeds(EmbedUtils.createError("That ID does not exist! Use `/log view` to see valid dose IDs.")).setEphemeral(true).queue();
            return;
        }

        // Update log in database
        Entry removedEntry = log.getDoses().get(year).remove(index);
        Bson update = Updates.pull("doses." + year, removedEntry);
        bot.database.logs.updateOne(filter, update);

        // Reply with embed of removed dose
        EmbedBuilder embed = removedEntry.getEmbed(userID);
        embed.setTitle("Dose #"+(index+1)+" Removed");
        embed.setThumbnail("https://cdn-icons-png.flaticon.com/512/2427/2427634.png");
        event.replyEmbeds(embed.build()).queue();
    }

    /**
     * Resets a user's entire log or a specific year.
     */
    private void executeReset(SlashCommandInteractionEvent event) {
        // Get command data
        long userID = event.getUser().getIdLong();
        OptionMapping yearOption = event.getOption("year");
        Bson filter = Filters.eq("user", userID);

        // Reset entire log in database (if year not specified)
        if (yearOption == null) {
            bot.database.logs.deleteOne(filter);
            String reply = ":wastebasket: <@"+userID+"> has reset their entire dose log!";
            event.replyEmbeds(EmbedUtils.createDefault(reply)).queue();
            return;
        }

        // Reset a specified year for log in database
        String year = yearOption.getAsString();
        Bson update = Updates.unset("doses."+year);
        bot.database.logs.updateOne(filter, update);
        String reply = ":wastebasket: <@"+userID+"> has reset their dose log for the year "+year+".";
        event.replyEmbeds(EmbedUtils.createDefault(reply)).queue();
    }
}
