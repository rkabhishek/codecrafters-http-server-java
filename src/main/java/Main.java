import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public class Main {
    private static final String CRLF = "\r\n";
    private static final String STATUS_OK = "HTTP/1.1 200 OK";
    private static final String STATUS_BAD_REQUEST = "HTTP/1.1 400 Bad Request";
    private static final String STATUS_NOT_FOUND = "HTTP/1.1 404 Not Found";
    private static final String STATUS_CREATED = "HTTP/1.1 201 Created";
    private static final String MISSING_HEADER = "Missing User-Agent header";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String TYPE_OCTET_STREAM = "application/octet-stream";
    private static final String TYPE_TEXT = "text/plain";

    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static String baseDirectory;

    public static void main(String[] args) {
        System.out.println("Logs from the program will appear here!");

        if (args.length == 2) {
            if (Objects.equals(args[0], "--directory")) {
                baseDirectory = args[1];
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            // Since the tester restarts the program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientConnection = serverSocket.accept(); // Wait for connection from client.
                System.out.println("accepted new connection");
                new Thread(() -> {
                    try {
                        String response = createResponse(clientConnection.getInputStream());
                        if (response != null) {
                            OutputStream output = clientConnection.getOutputStream();
                            output.write(response.getBytes());
                        }
                        clientConnection.close();
                    } catch (IOException e) {
                        System.out.println("IOException: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    static String createResponse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        String method = parts[0];
        String urlPath = parts.length > 1 ? parts[1] : "/";
        if (Objects.equals(urlPath, "/")) {
            return String.format("%s%s%s", STATUS_OK, CRLF, CRLF);
        } else if (urlPath.startsWith("/echo")) {
            return handleEchoRequest(urlPath);
        } else if (urlPath.startsWith("/user-agent")) {
            return handleUserAgentRequest(reader);
        } else if (urlPath.startsWith("/files")) {
            if (Objects.equals(method, "GET")) {
                return handleGETFileRequest(urlPath);
            } else {
                String pathPrefix = "/files/";
                String fileName = urlPath.substring(pathPrefix.length());
                if (fileName.isEmpty()) {
                    return String.format("%s%s%s", STATUS_BAD_REQUEST, CRLF, CRLF);
                }

                String fullPath = baseDirectory + fileName;
                String line;
                int length = 0;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    System.out.println("line " + line);
                    if (line.startsWith(HEADER_CONTENT_LENGTH)) {
                     length = Integer.parseInt(line.split(":")[1].trim());
                    }
                    System.out.println("length is " + length);
                }

                System.out.println("outside loop");
                char[] body = new char[length];
                reader.read(body, 0, length);
                System.out.println("body length " + body.length);
                System.out.println(new String(body));
                return handlePOSTFileRequest(fullPath, new String(body));
            }
        } else {
            return String.format("%s%s%s", STATUS_NOT_FOUND, CRLF, CRLF);
        }
    }

    private static String handleEchoRequest(String path) {
        String prefix = "/echo/";
        String body = "";
        if (path.startsWith(prefix)) {
            body = path.substring(prefix.length());
        }

        String headers = String.format("%s: %s%s%s: %s%s", HEADER_CONTENT_TYPE, TYPE_TEXT, CRLF, HEADER_CONTENT_LENGTH, body.length(), CRLF);
        return String.format("%s%s%s%s%s", STATUS_OK, CRLF, headers, CRLF, body);
    }

    private static String handleUserAgentRequest(BufferedReader bufferedReader) throws IOException {
        String prefix = "User-Agent:";
        String line;
        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith(prefix)) {
                System.out.println("inside prefix\n");
                String body = line.split(" ")[1];
                System.out.println("body is " + body);
                String headers = String.format("%s: %s%s%s: %s%s", HEADER_CONTENT_TYPE, TYPE_TEXT, CRLF, HEADER_CONTENT_LENGTH, body.length(), CRLF);
                return String.format("%s%s%s%s%s", STATUS_OK, CRLF, headers, CRLF, body);
            }
        }
        return String.format("%s%s%s%s%s", STATUS_BAD_REQUEST, CRLF, MISSING_HEADER, CRLF, MISSING_HEADER.length());
    }

    private static String handleGETFileRequest(String urlPath) throws IOException {
        System.out.println("handling file");
        String pathPrefix = "/files/";
        String fileName = urlPath.substring(pathPrefix.length());
        if (fileName.isEmpty()) {
            return String.format("%s%s%s", STATUS_NOT_FOUND, CRLF, CRLF);
        }

        String fullPath = baseDirectory + fileName;
        Path path = Path.of(fullPath);
        if (!Files.exists(path)) {
            return String.format("%s%s%s", STATUS_NOT_FOUND, CRLF, CRLF);
        }

        String content = new String(Files.readAllBytes(path));
        int contentLength = content.length();

        String headers = String.format("%s: %s%s%s: %s%s", HEADER_CONTENT_TYPE, TYPE_OCTET_STREAM, CRLF, HEADER_CONTENT_LENGTH, contentLength, CRLF);
        return String.format("%s%s%s%s%s", STATUS_OK, CRLF, headers, CRLF, content);
    }

    private static String handlePOSTFileRequest(String filePath, String body) throws IOException {
        System.out.println("handling file creation");
        Path path = Path.of(filePath);
        Files.writeString(path, body, StandardCharsets.UTF_8);
        return String.format("%s%s%s", STATUS_CREATED, CRLF, CRLF);
    }
}