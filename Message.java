import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Message {
    private static int nextId = 1;
    private final int id;
    private final String sender;
    private final String subject;
    private final String content;
    private final LocalDateTime postDate;
    private final Group group;

    public Message(String sender, String subject, String content, Group group) {
        this.id = nextId++;
        this.sender = sender;
        this.subject = subject;
        this.content = content;
        this.postDate = LocalDateTime.now();
        this.group = group;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("Message ID: %s, Sender: %s, Post Date: %s, Subject: %s, Group: %s", 
            id, sender, postDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), subject, group.getName());
    }

    public String getFullContent() {
        return toString() + "\nContent: " + content;
    }
}