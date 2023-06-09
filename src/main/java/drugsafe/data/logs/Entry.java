package drugsafe.data.logs;

import drugsafe.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * POJO object that stores a single logged dose
 *
 * @author TechnoVision
 */
public class Entry {

    private static final DecimalFormat numberFormat = new DecimalFormat("#,###.##");

    private String drug;

    private double dose;

    private String units;

    private String route;

    private Date date;

    public Entry() { }

    public Entry(String drug, double dose, String units, String route) {
        this.drug = drug;
        this.dose = dose;
        this.units = units;
        this.route = route;
        this.date = new Date();
    }

    public Entry(String drug, double dose, String units, String route, Date date) {
        this.drug = drug;
        this.dose = dose;
        this.units = units;
        this.route = route;
        this.date = date;
    }

    public EmbedBuilder getEmbed(long userID) {
        return new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle("New Dose Logged")
                .addField("User", "<@"+ userID +">", false)
                .addField("Drug", drug, false)
                .addField("Amount", numberFormat.format(dose) + " " + units, true)
                .addField("Route", route.substring(0, 1).toUpperCase() + route.substring(1), true)
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/6134/6134622.png")
                .setTimestamp(date.toInstant());
    }

    @Override
    public String toString() {
        return drug + " " + numberFormat.format(dose) + " " + units
                + " (" + route.substring(0, 1).toUpperCase() + route.substring(1) + ")";
    }

    public String getDrug() {
        return drug;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public double getDose() {
        return dose;
    }

    public void setDose(double dose) {
        this.dose = dose;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
