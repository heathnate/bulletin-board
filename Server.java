import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static final int PORT = 5000;
    private static final int MESSAGE_HISTORY_LIMIT = 2;
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static List<Message> messageHistory = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Part 1 server is running...");
        ServerSocket serverSocket = new ServerSocket(PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket).start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Welcome! Enter a unique username: ");

                while (true) {
                    username = in.readLine();
                    if (username == null || username.trim().isEmpty() || clients.containsKey(username)) {
                        out.println("Username '" + username + "' already taken or invalid. Try again: ");
                    } else {
                        synchronized(clients) {
                            clients.put(username, out);
                        }
                        break;
                    }
                }

                notifyAllClients(username + " has joined the group.");
                sendUserList();
                sendRecentMessages();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("%leave")) {
                        leaveGroup();
                        break;
                    } else if (message.startsWith("%message")) {
                        int messageId;
                        try {
                            messageId = Integer.parseInt(message.split(" ")[1]);
                            out.println(viewMessageContent(messageId));
                        } catch (Exception e) {
                            out.println("Invalid message ID.");
                        }
                    } else {
                        out.println("Please provide a message ID after the '%message' command.");
                    }
                }
            } catch(IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
            } finally {
                leaveGroup();
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error leaving group: " + e.getMessage());
                }
            }
        }

        private void leaveGroup() {
            if (username != null) {
                synchronized (clients) {
                    clients.remove(username);
                }
                notifyAllClients(username + " has left the group.");
            }
        }

        private void notifyAllClients(String notification) {
            synchronized (clients) {
                for (PrintWriter writer: clients.values()) {
                    writer.println("[Notification]: " + notification);
                }
            }
        }

        private void sendUserList() {
            synchronized (clients) {
                out.println("Users in the group: " + String.join(",", clients.keySet()));
            }
        }

        private void sendRecentMessages() {
            synchronized (messageHistory) {
                for (int i = Math.max(0, messageHistory.size() - MESSAGE_HISTORY_LIMIT); i < messageHistory.size(); i++) {
                    out.println(messageHistory.get(i));
                }
            }
        }

        private void postMessage(String content) {
            synchronized (messageHistory) {
                Message message = new Message(messageHistory.size() + 1, username, new Date(), content);
                messageHistory.add(message);
                notifyAllClients(message.convertToString());
            }
        }

        private String viewMessageContent(int messageId) {
            synchronized (messageHistory) {
                return messageHistory.stream()
                    .filter(msg -> msg.getId() == messageId)
                    .map(Message::getContent)
                    .findFirst()
                    .orElse("Message not found.");
                }
            }
        }

    private static class Message {
        private int id;
        private String sender;
        private Date date;
        private String content;

        public Message(int id, String sender, Date date, String content) {
            this.id = id;
            this.sender = sender;
            this.date = date;
            this.content = content;
        }

        public int getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public String convertToString() {
            return "Message " + id + ", " + sender + ", " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) + ", " + content;
        }
    }
}

    

    // private static final int PORT = 5000;
    // private static final List<String> connectedUsers = Collections.synchronizedList(new ArrayList<>());
    // private static final List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());
    // private static final List<String> messageHistory = Collections.synchronizedList(new ArrayList<>());
    // private static final List<Group> groups = Arrays.asList(
    //         new Group(1, "Group A"),
    //         new Group(2, "Group B"),
    //         new Group(3, "Group C"),
    //         new Group(4, "Group D"),
    //         new Group(5, "Group E")
    // );
    // private static int messageId = 0;

    // public static void main(String[] args) {
    //     try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    //         System.out.println("Server listening on port " + PORT);

    //         while (true) {
    //             Socket socket = serverSocket.accept();
    //             System.out.println("A client just connected.");
    //             ClientHandler clientHandler = new ClientHandler(socket);
    //             connectedClients.add(clientHandler);
    //             new Thread(clientHandler).start();
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    // private static class ClientHandler implements Runnable {
    //     private final Socket socket;
    //     private PrintWriter out;
    //     private BufferedReader in;
    //     private String username;
    //     private final List<Group> userGroups = new ArrayList<>();

    //     public ClientHandler(Socket socket) {
    //         this.socket = socket;
    //     }

    //     @Override
    //     public void run() {
    //         try {
    //             out = new PrintWriter(socket.getOutputStream(), true);
    //             in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    //             out.println("Enter username: ");
    //             String clientInput;

    //             while ((clientInput = in.readLine()) != null) {
    //                 if (username == null) {
    //                     username = clientInput.trim();
    //                     connectedUsers.add(username);
    //                     System.out.println("Welcome " + username);
    //                     broadcast("Welcome " + username);
    //                     broadcast("Connected Users: " + connectedUsers);

    //                     groups.forEach(group -> out.println("ID: " + group.id + ", Name: " + group.name));
    //                     out.println("Enter group IDs or names to join:");

    //                     if (!messageHistory.isEmpty()) {
    //                         out.println("Last 2 messages:");
    //                         messageHistory.stream()
    //                                 .skip(Math.max(0, messageHistory.size() - 2))
    //                                 .forEach(out::println);
    //                     }
    //                 } else if ("exit".equalsIgnoreCase(clientInput.trim())) {
    //                     disconnect();
    //                     break;
    //                 } else if (userGroups.isEmpty()) {
    //                     joinGroups(clientInput);
    //                 } else {
    //                     sendMessageToGroups(clientInput);
    //                 }
    //             }
    //         } catch (IOException e) {
    //             System.err.println("Error handling client: " + e.getMessage());
    //         } finally {
    //             disconnect();
    //         }
    //     }

    //     private void joinGroups(String clientInput) {
    //         String[] inputs = clientInput.split(" ");
    //         for (String input : inputs) {
    //             groups.stream()
    //                     .filter(group -> String.valueOf(group.id).equals(input) || group.name.equalsIgnoreCase(input))
    //                     .forEach(group -> {
    //                         if (!userGroups.contains(group)) {
    //                             userGroups.add(group);
    //                             group.addUser(this);
    //                         }
    //                     });
    //         }
    //         out.println("You have joined: " + userGroups.stream().map(g -> g.name).toList());
    //     }

    //     private void sendMessageToGroups(String message) {
    //         String timestamp = new Date().toString();
    //         String formattedMessage = messageId++ + " " + username + " " + timestamp + ": " + message;
    //         System.out.println(formattedMessage);
    //         messageHistory.add(formattedMessage);

    //         userGroups.forEach(group -> group.broadcast(formattedMessage, this));
    //     }

    //     private void disconnect() {
    //         connectedUsers.remove(username);
    //         connectedClients.remove(this);
    //         System.out.println(username + " has disconnected.");
    //         broadcast(username + " has disconnected.");
    //         try {
    //             socket.close();
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     }
    // }

    // private static void broadcast(String message) {
    //     synchronized (connectedClients) {
    //         connectedClients.forEach(client -> client.out.println(message));
    //     }
    // }

    // private static class Group {
    //     private final int id;
    //     private final String name;
    //     private final List<ClientHandler> users = new ArrayList<>();

    //     public Group(int id, String name) {
    //         this.id = id;
    //         this.name = name;
    //     }

    //     public void addUser(ClientHandler user) {
    //         users.add(user);
    //     }

    //     public void broadcast(String message, ClientHandler sender) {
    //         users.stream().filter(user -> user != sender).forEach(user -> user.out.println(message));
    //     }
    // }
