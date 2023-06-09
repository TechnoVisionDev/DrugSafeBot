package drugsafe.data.logs;

import java.util.List;

/**
 * POJO object that stores logged doses
 *
 * @author TechnoVision
 */
public class Log {

    private long user;

    private List<Entry> doses;

    public Log() { }

    public Log(long user) {
        this.user = user;
    }

    public long getUser() {
        return user;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public List<Entry> getDoses() {
        return doses;
    }

    public void setDoses(List<Entry> doses) {
        this.doses = doses;
    }
}