import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// Helper type 
public class Message {
    private static int nextId = 1;
    private final int id;
    private final String sender;
    private final String subject;
    private final String content;
    private final LocalDateTime postDate;
    private final Group group;

    public Message(String sender, String subject, String content, Group group) {
        // Increment message IDs for each one that is created
        this.id = nextId++;
        this.sender = sender;
        this.subject = subject;
        this.content = content;
        // Get current time
        this.postDate = LocalDateTime.now();
        this.group = group;
    }

    public int getId() {
        return id;
    }

    // Overridden method to convert the message data to one string
    @Override
    public String toString() {
        return String.format("Message ID: %s, Sender: %s, Post Date: %s, Subject: %s, Group: %s", 
            id, sender, postDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), subject, group.getName());
    }

    // Method to get the content of a message
    public String getFullContent() {
        return toString() + "\nContent: " + content;
    }
}