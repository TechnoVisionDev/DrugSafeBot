package drugsafe;

import drugsafe.commands.CommandRegistry;
import drugsafe.data.Database;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.Calendar;
import java.util.Date;

/**
 * Main class for DrugSafe Discord Bot.
 * Initializes shard manager, database, and listeners.
 *
 * @author TechnoVision
 */
public class DrugSafe {

    public final @NotNull Dotenv config;
    public final @NotNull Database database;
    public final @NotNull ShardManager shardManager;

    /**
     * Builds bot shards and registers commands and modules.
     *
     * @throws LoginException throws if bot token is invalid.
     */
    public DrugSafe() throws LoginException {
        //Setup Database
        config = Dotenv.configure().ignoreIfMissing().load();
        String uri = config.get("MONGODB_URI", System.getenv("MONGODB_URI"));
        String databaseName = config.get("MONGODB_DB", System.getenv("MONGODB_DB"));
        database = new Database(uri, databaseName);

        //Build JDA shards
        String botToken = config.get("TOKEN", System.getenv("TOKEN"));
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(botToken);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("drugsafe.info"));

        CommandRegistry commandRegistry = new CommandRegistry(this);
        builder.addEventListeners(commandRegistry);

        shardManager = builder.build();

        // Register the commands as global commands
        shardManager.getShards().forEach(jda -> {
            jda.updateCommands().addCommands(commandRegistry.unpackCommandData()).queue();
        });
    }

    /**
     * Initialize bot.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        try {
            DrugSafe bot = new DrugSafe();
        } catch (LoginException e) {
            System.out.println("ERROR: Provided bot token is invalid!");
        }
    }

    /**
     * Formats the current year as a string.
     * @return current year as string
     */
    public static String getCurrentYear() {
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }
}
