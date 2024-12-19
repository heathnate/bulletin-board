import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5000;
    // CopyOnWriteArrayList allows safe concurrent modification
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final List<Group> groups = new ArrayList<>();
    private static final ConcurrentHashMap<String, List<Message>> groupMessages = new ConcurrentHashMap<>();

    // Initialize groups when server starts
    static {
        // Add default public group
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
        // Initialize server socket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Bulletin Board Server is running on port " + PORT);

            // Infinite loop to continuously accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Create a new ClientHandler for each connected client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                // Add the client handler to list of active clients
                clients.add(clientHandler);
                // Start a new thread for each client to handle concurrent connections
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Error during client handler initialization: " + e.getMessage());
        }
    }

    // Inner class that handles individual client connections
    private static class ClientHandler implements Runnable {
        // Socket for the individual client connection
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
                // Set up input and output streams for communication
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

                // Send current user list for default public group
                sendUserList(groups.get(0));

                // Show list of possible commands
                out.println(helpMessage);

                // Main command processing loop
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Cleanup when client disconnects
                clients.remove(this);
                broadcastLeave(username);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    out.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        // Process and route client commands
        private void processCommand(String command) throws IOException {
            // Strip the command keyword from the rest of the command 
            String[] mainParts = command.split(" ", 2);
            String cmd = mainParts[0].toLowerCase();
            String[] cmdAuxilary;

            switch (cmd) {
                // Handle posting to public group
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
                // Handle posting to a private group
                case "%grouppost":
                    // Get the group num, subject, and content from the command
                    cmdAuxilary = mainParts[1].split(" ", 3);

                    // Check for invalid command format
                    if (cmdAuxilary.length < 3) {
                        out.println("Invalid command. Format: '%grouppost <group_num> <subject> <content>'\n");
                        break;
                    }

                    Group group = findGroup(cmdAuxilary[0]);
                    if (group != null) {
                        handlePost(cmdAuxilary[1], cmdAuxilary[2], group);
                    }
                    break;
                // Handle getting users in public group
                case "%users":
                    sendUserList(groups.get(0)); // Default public group
                    break;
                // Handle getting users in a private group
                case "%groupusers":
                    // Verify user provided a group number
                    if (mainParts.length != 2) {
                        out.println("Invalid command. Format: '%groupusers <group_num>'\n");
                        break;
                    }

                    Group targetGroup = findGroup(mainParts[1]);
                    if (targetGroup != null) {
                        sendUserList(targetGroup);
                    }
                    break;
                // Handle case for getting a message from the public group
                case "%message":
                    sendMessageContent(mainParts[1], groups.get(0)); // Default public group
                    break;
                // Handle case for getting a message from a private group
                case "%groupmessage":
                    // Check for invalid command format
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
                // Handle case for getting all available groups
                case "%groups":
                    sendGroupList();
                    break;
                // Handle case for joining a private group
                case "%groupjoin":
                    // Verify user provided a group number
                    if (mainParts.length != 2) {
                        out.println("Invalid command. Format: '%groupjoin <group_num>'\n");
                        break;
                    }

                    Group joinGroup = findGroup(mainParts[1]);
                    if (joinGroup != null) {
                        joinGroup(joinGroup);
                    }
                    break;
                // Handle case for leaving a private group
                case "%groupleave":
                    // Verify user provided a group number
                    if (mainParts.length != 2) {
                        out.println("Invalid command. Format: '%groupleave <group_num>'\n");
                        break;
                    }

                    Group leaveGroup = findGroup(mainParts[1]);
                    if (leaveGroup != null) {
                        leaveGroup(leaveGroup);
                    }
                    break;
                // Handle case for leaving default public group
                case "%leave":
                    leaveGroup(groups.get(0)); // Leave public group
                    break;
                // Handle case for exiting server
                case "%exit":
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        out.println("Error disconnecting from server: " + e.getMessage());
                    }
                    break;
                // Handle case for printing possible commands
                case "%help":
                    out.println(helpMessage);
                    break;
                // Handle case for unrecognized command
                default:
                    out.println("Unknown command. Enter %help to see a list of all possible commands.\n");
                    break;
            }
        }

        // Method to handle message posting
        private void handlePost(String subject, String content, Group group) {
            // Create a new message and add it to the group's message list
            Message message = new Message(username, subject, content, group);
            groupMessages.get(group.getId()).add(message);
            // Broadcast the message to all users in the group
            broadcastMessage(message, group);
        }

        // Method to join a group
        private void joinGroup(Group group) {
            // Add group if not already joined and broadcast join event
            if (!joinedGroups.contains(group)) {
                joinedGroups.add(group);
                broadcastJoin(username, group);
            }
        }

        // Method to leave a group
        private void leaveGroup(Group group) {
            if (joinedGroups.remove(group)) {
                // Broadcast leave event if group successfully removed
                broadcastLeave(username, group);
            }
            out.println("You have left " + group.getName());
        }

        // Method to send last 2 messages upon group join
        private void sendLastMessages(Group group) {
            // Get list of previous messages
            List<Message> messages = groupMessages.get(group.getId());

            if (messages.isEmpty()) {
                out.println("No recent messages.\n");
            } else {
                out.println("Recent messages: ");
                int start = Math.max(0, messages.size() - 2);
                // Print last 2 messages
                for (int i = start; i < messages.size(); i++) {
                    out.println(messages.get(i));
                }
                out.println("\n");
            }
        }

        // Method to show current users in a group
        private void sendUserList(Group group) {
            out.println("Users in " + group.getName() + ":");
            // Stream through all clients and filter for those in the specified group
            // This ensures only users in the current group are displayed
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> out.println(c.username));
            out.println(); // Print newline
        }

        // Method to show list of all available groups
        private void sendGroupList() {
            out.println("Available Groups:");
            groups.forEach(g -> out.println(g.getId() + ": " + g.getName()));
        }

        // Method to show message content given an ID
        private void sendMessageContent(String messageId, Group group) {
            // Use stream to find the message with the matching ID in the group's message list
            groupMessages.get(group.getId()).stream()
                // Convert message ID to int and find matching message
                .filter(m -> m.getId() == Integer.parseInt(messageId))
                // Get the first matching message (not always necessary)
                .findFirst()
                // If message found, print its full content
                .ifPresent(m -> out.println(m.getFullContent()));
        }

        // Method to broadcast a message to all users in a specific group
        private void broadcastMessage(Message message, Group group) {
            // Stream through all clients and filter for those in the group
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(message));
        }

        // Method to broadcast a join event to all users in a specific group
        private void broadcastJoin(String username, Group group) {
            // Stream through all clients and filter for those in the group
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(username + " joined " + group.getName() + "\n"));
        }

        // Method to broadcast a leave event to the public grooup users when a user disconnects
        private void broadcastLeave(String username) {
            // Stream through all clients and filter for those in the public group
            clients.stream()
                .filter(c -> c.joinedGroups.contains(groups.get(0)))
                .forEach(c -> c.out.println(username + " left the public group\n"));
        }

        // Method to broadcast a leave event to all users in a specific group
        private void broadcastLeave(String username, Group group) {
            // Stream through all clients and filter for those in the specified group
            clients.stream()
                .filter(c -> c.joinedGroups.contains(group))
                .forEach(c -> c.out.println(username + " left " + group.getName() + "\n"));
        }

        // Method to return Group object given an ID
        private Group findGroup(String groupId) {
            // Stream through all groups and filter for group ID
            return groups.stream()
                .filter(g -> g.getId().equals(groupId) || g.getName().equalsIgnoreCase(groupId))
                .findFirst()
                .orElse(null);
        }
    }
}