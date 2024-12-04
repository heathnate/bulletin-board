import java.util.UUID;

// Helper type
public class Group {
    private final String id;
    private final String name;

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Equals operator override
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return id.equals(group.id);
    }

    // Overridden hashCode getter
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}