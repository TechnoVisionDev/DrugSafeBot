package drugsafe.data.logs;

import drugsafe.DrugSafe;
import drugsafe.util.embeds.EmbedColor;
import drugsafe.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * POJO object that stores logged doses
 *
 * @author TechnoVision
 */
public class Log {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d - h:mm a");

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
     * @return a MessageEmbed with log data.
     */
    public MessageEmbed getEmbed(User user, String year) {
        // Get entries for the specified year
        List<Entry> dosesThisYear = doses.get(year);

        // Create log embed
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle("Dose Log ("+year+")")
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/3209/3209027.png")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl());

        // Loop over entries for the current year and add to embed
        for (int i = dosesThisYear.size()-1; i >= 0; i--) {
            // Format date
            Entry entry = dosesThisYear.get(i);
            int id = i+1;
            LocalDateTime localDate = entry.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            String formattedDate = "[" + id + "] " + localDate.format(formatter.withLocale(Locale.US));

            // insert the day suffix after the day of month
            String daySuffix = getDayOfMonthSuffix(localDate.getDayOfMonth());
            int index = formattedDate.indexOf("-");
            formattedDate = new StringBuilder(formattedDate).insert(index - 1, daySuffix).toString();

            // Add to embed as field
            embed.addField(formattedDate, entry.toString(), false);
        }
        return embed.build();
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