package drugsafe.data.logs;

import drugsafe.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * POJO object that stores logged doses
 *
 * @author TechnoVision
 */
public class Log {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d - ");

    private long user;

    private Map<String, List<Entry>> doses;

    public Log() { }

    public Log(long user) {
        this.user = user;
    }

    /**
     * Creates an embed to display log data.
     * @param user the user whose log is being displayed.
     * @param year the year to display logged doses for.
     * @return a list of MessageEmbeds with log data.
     */
    public List<MessageEmbed> getEmbed(User user, String year) {
        // Get entries for the specified year
        List<Entry> dosesThisYear = doses.get(year);
        List<MessageEmbed> pages = new ArrayList<>();

        // Initialize EmbedBuilder for the first page
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle(":pencil: Dose Log ("+year+")")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl());

        // Loop over entries for the current year in reverse order and add to embed
        for (int i = dosesThisYear.size() - 1, fieldCount = 0; i >= 0; i--, fieldCount++) {
            // Check if we've hit the field limit for the current embed
            if (fieldCount != 0 && fieldCount % 5 == 0) {
                // If we have, build the current embed, add it to the list, and start a new one
                pages.add(embed.build());
                embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle(":pencil: Dose Log ("+year+")")
                        .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl());
            }
            // Format date
            Entry entry = dosesThisYear.get(i);
            long timestampTime = entry.getDate().toInstant().getEpochSecond();
            String formattedDate = "**[" + (dosesThisYear.size() - fieldCount) + "] "
                    + "<t:"+timestampTime+":D>"
                    + " - " + "<t:"+timestampTime+":t>**";;

            // Add to embed as field
            //embed.addField(formattedDate, entry.toString(), false);
            embed.appendDescription(formattedDate + "\n" + entry.toString() + "\n\n");
        }
        // Add last page
        pages.add(embed.build());
        return pages;
    }

    /**
     * Helper function for getEmbed() that finds suffix for day of month.
     * @param n the day of the month
     * @return a string suffix for the day of the month.
     */
    private String getDayOfMonthSuffix(final int n) {
        if (n < 1 || n > 31) throw new IllegalArgumentException("Illegal day of month");
        if (n >= 11 && n <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public long getUser() {
        return user;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public Map<String, List<Entry>> getDoses() {
        return doses;
    }

    public void setDoses(Map<String, List<Entry>> doses) {
        this.doses = doses;
    }
}