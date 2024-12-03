import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final List<Group> groups = new ArrayList<>();
    private static final ConcurrentHashMap<String, List<Message>> groupMessages = new ConcurrentHashMap<>();

    // Initialize default public group
    static {
        Group publicGroup = new Group("0", "Public Group");
        groups.add(publicGroup);
        groupMessages.put(publicGroup.getId(), new ArrayList<>());

        // Add 5 private groups for Part 2
        for (int i = 1; i <= 5; i++) {
            Group privateGroup = new Group(String.valueOf(i), "Private Group " + i);
            groups.add(privateGroup);
            groupMessages.put(privateGroup.getId(), new ArrayList<>());
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Bulletin Board Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private final Set<Group> joinedGroups = new HashSet<>();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Username registration
                out.println("Welcome to the Bulletin Board Server. Please enter your username:");
                username = in.readLine();

                // Join public group by default
                Group publicGroup = groups.get(0);
                joinGroup(publicGroup);

                // Send last 2 messages to new user
                sendLastMessages(publicGroup);

                // Protocol handling
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clients.remove(this);
                broadcastLeave(username);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processCommand(String command) throws IOException {
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "%post":
                    handlePost(parts[1], parts[2], groups.get(0)); // Default public group
                    break;
                case "%grouppost":
                    Group group = findGroup(parts[1]);
                    if (group != null) {
                        handlePost(parts[1], parts[2], group);
                    }
                    break;
                case "%users":
                    sendUserList(groups.get(0));
                    break;
                case "%groupusers":
                    Group targetGroup = findGroup(parts[1]);
                    if (targetGroup != null) {
                        sendUserList(targetGroup);
                    }
                    break;
                case "%message":
                    sendMessageContent(parts[1], groups.get(0));
                    break;
                case "%groupmessage":
                    Group msgGroup = findGroup(parts[1]);
                    if (msgGroup != null) {
                        sendMessageContent(parts[2], msgGroup);
                    }
                    break;
                case "%groups":
                    sendGroupList();
                    break;
                case "%groupjoin":
                    Group joinGroup = findGroup(parts[1]);
                    if (joinGroup != null) {
                        joinGroup(joinGroup);
                    }
                    break;
                case "%groupleave":
                    Group leaveGroup = findGroup(parts[1]);
                    if (leaveGroup != null) {
                        leaveGroup(leaveGroup);
                    }
                    break;
                case "%leave":
                    leaveGroup(groups.get(0)); // Leave public group
                    break;
                case "%exit":
                    clientSocket.close();
                    break;
            }
        }

        private void handlePost(String subject, String content, Group group) {
            Message message = new Message(username, subject, content, group);
            groupMessages.get(group.getId()).add(message);
            broadcastMessage(message, group);
        }

        private void joinGroup(Group group) {
            if (!joinedGroups.contains(group)) {
                joinedGroups.add(group);
                broadcastJoin(username, group);
            }
        }

        private void leaveGroup(Group group) {
            if (joinedGroups.remove(group)) {
                broadcastLeave(username, group);
            }
        }

        private void sendLastMessages(Group group) {
            List<Message> messages = groupMessages.get(group.getId());
            int start = Math.max(0, messages.size() - 2);
            for (int i = start; i < messages.size(); i++) {
                out.println(messages.get(i));
            }
        }

        private void sendUserList(Group group) {
            out.println("Users in " + group.getName() + ":");
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> out.println(c.username));
        }

        private void sendGroupList() {
            out.println("Available Groups:");
            groups.forEach(g -> out.println(g.getId() + ": " + g.getName()));
        }

        private void sendMessageContent(String messageId, Group group) {
            groupMessages.get(group.getId()).stream()
                .filter(m -> m.getId() == Integer.parseInt(messageId))
                .findFirst()
                .ifPresent(m -> out.println(m.getFullContent()));
        }

        private void broadcastMessage(Message message, Group group) {
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(message));
        }

        private void broadcastJoin(String username, Group group) {
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(username + " joined " + group.getName()));
        }

        private void broadcastLeave(String username) {
            clients.stream()
                .filter(c -> c.joinedGroups.contains(groups.get(0)))
                .forEach(c -> c.out.println(username + " left the public group"));
        }

        private void broadcastLeave(String username, Group group) {
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(username + " left " + group.getName()));
        }

        private Group findGroup(String groupId) {
            return groups.stream()
                .filter(g -> g.getId().equals(groupId) || g.getName().equalsIgnoreCase(groupId))
                .findFirst()
                .orElse(null);
        }
    }

    // Additional helper classes will be in separate artifacts
}