package drugsafe.commands.util;

import drugsafe.DrugSafe;
import drugsafe.commands.Category;
import drugsafe.commands.Command;
import drugsafe.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

/**
 * Creates button links to invite bot and join the server.
 *
 * @author TechnoVision
 */
public class InviteCommand extends Command {

    public InviteCommand(DrugSafe bot) {
        super(bot);
        this.name = "invite";
        this.description = "Invite the bot to your servers";
        this.category = Category.UTILITY;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Button b1 = Button.link("https://discord.com/oauth2/authorize?client_id=1117987531535949836&scope=bot&permissions=2147795009", "Invite Bot");
        Button b2 = Button.link("https://discord.gg/SWrb3MHVCd", "DrugSafe Server");
        event.replyEmbeds(EmbedUtils.createDefault(":robot: Click the button below to add me to your server!"))
                .addActionRow(b1, b2).queue();
    }
}
