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
        private final String helpMessage = "Commands:\n"
                            + "%exit - Disconnect from the server\n" + "%post - Post a message to a message board\n"
                            + "%users - View a list of all users in a group\n" + "%leave - Leave the current group\n"
                            + "%message - View the content of a certain message\n" + "%groups - View a list of all groups\n"
                            + "%groupjoin - Join a specific group\n" + "%grouppost - Post a message to a specific group\n"
                            + "%groupusers - View a list of all users within a specific group\n" + "%groupleave - Leave a specific group\n"
                            + "%groupmessage - View the content of a message within a specific group\n" + "%help - Repeat this message\n";

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

                // Send current user list for default group
                sendUserList(groups.get(0));

                // Show list of possible commands
                out.println(helpMessage);

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
            // Strip the command keyword from the rest of the command 
            String[] mainParts = command.split(" ", 2);
            String cmd = mainParts[0].toLowerCase();

            String[] cmdAuxilary;

            switch (cmd) {
                case "%post":
                    // Get the subject and content from the command
                    cmdAuxilary = mainParts[1].split(" ", 2);

                    // Check for invalid command format
                    if (cmdAuxilary.length < 2) {
                        out.println("Invalid command. Format: '%post <subject> <content>'\n");
                        break;
                    }

                    handlePost(cmdAuxilary[0], cmdAuxilary[1], groups.get(0)); // Default public group
                    break;
                case "%grouppost":
                    cmdAuxilary = mainParts[1].split(" ", 3);
                    if (cmdAuxilary.length < 3) {
                        out.println("Invalid command. Format: '%grouppost <group_num> <subject> <content>'\n");
                        break;
                    }

                    Group group = findGroup(cmdAuxilary[1]);
                    if (group != null) {
                        handlePost(cmdAuxilary[0], cmdAuxilary[1], group);
                    }
                    break;
                case "%users":
                    sendUserList(groups.get(0));
                    break;
                case "%groupusers":
                    Group targetGroup = findGroup(mainParts[1]);
                    if (targetGroup != null) {
                        sendUserList(targetGroup);
                    }
                    break;
                case "%message":
                    sendMessageContent(mainParts[1], groups.get(0));
                    break;
                case "%groupmessage":
                    cmdAuxilary = mainParts[1].split(" ", 2);
                    if (cmdAuxilary.length < 2) {
                        out.println("Invalid command. Format: '%groupmessage <group_num> <message_ID>'\n");
                        break;
                    }
                    Group msgGroup = findGroup(cmdAuxilary[0]);
                    if (msgGroup != null) {
                        sendMessageContent(cmdAuxilary[1], msgGroup);
                    }
                    break;
                case "%groups":
                    sendGroupList();
                    break;
                case "%groupjoin":
                    Group joinGroup = findGroup(mainParts[1]);
                    if (joinGroup != null) {
                        joinGroup(joinGroup);
                    }
                    break;
                case "%groupleave":
                    Group leaveGroup = findGroup(mainParts[1]);
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
                case "%help":
                    out.println(helpMessage);
                    break;
                default:
                    out.println("Unknown command. Enter %help to see a list of all possible commands.\n");
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

            if (messages.isEmpty()) {
                out.println("No recent messages.\n");
            } else {
                out.println("Recent messages: ");
                int start = Math.max(0, messages.size() - 2);
                for (int i = start; i < messages.size(); i++) {
                    out.println(messages.get(i));
                }
                out.println("\n");
            }
        }

        private void sendUserList(Group group) {
            out.println("Users in " + group.getName() + ":");
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> out.println(c.username));
            out.println(); // Print newline
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
                .forEach(c -> c.out.println(username + " joined " + group.getName() + "\n"));
        }

        private void broadcastLeave(String username) {
            clients.stream()
                .filter(c -> c.joinedGroups.contains(groups.get(0)))
                .forEach(c -> c.out.println(username + " left the public group\n"));
        }

        private void broadcastLeave(String username, Group group) {
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(username + " left " + group.getName() + "\n"));
        }

        private Group findGroup(String groupId) {
            return groups.stream()
                .filter(g -> g.getId().equals(groupId) || g.getName().equalsIgnoreCase(groupId))
                .findFirst()
                .orElse(null);
        }
    }
}