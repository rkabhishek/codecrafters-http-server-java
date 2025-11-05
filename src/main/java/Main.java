import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Main {
    private static final String CRLF = "\r\n";
    private static final String STATUS_OK = "HTTP/1.1 200 OK";
    private static final String STATUS_BAD_REQUEST = "HTTP/1.1 400 Bad Request";
    private static final String STATUS_NOT_FOUND = "HTTP/1.1 404 Not Found";
    private static final String MISSING_HEADER = "Missing User-Agent header";
    private static final String HEADER_TEMPLATE = "Content-Type: text/plain%sContent-Length: %d%s";
  public static void main(String[] args) {
      System.out.println("Logs from the program will appear here!");
      try (ServerSocket serverSocket = new ServerSocket(4221)) {
          // Since the tester restarts the program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          while (true) {
              Socket clientConnection = serverSocket.accept(); // Wait for connection from client.
              System.out.println("accepted new connection");
              new Thread(() -> {
                  try {
                      BufferedReader reader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream(), StandardCharsets.UTF_8));
                      String requestLine = reader.readLine();
                      if (requestLine == null) {
                          System.out.println("Empty request or connection closed.");
                      } else {
                          String response = createResponse(requestLine, reader);
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

  static String createResponse(String requestLine, BufferedReader reader) throws IOException {
      String[] parts = requestLine.split(" ");
      String urlPath = parts.length > 1 ? parts[1] : "/";
      if (Objects.equals(urlPath, "/")) {
          return String.format("%s%s%s", STATUS_OK, CRLF, CRLF);
      } else if (urlPath.startsWith("/echo")) {
          return handleEchoRequest(urlPath);
      } else if (urlPath.startsWith("/user-agent")) {
          return handleUserAgentRequest(reader);
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

      String headers = String.format(HEADER_TEMPLATE, CRLF, body.length(), CRLF);
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
              String headers = String.format(HEADER_TEMPLATE, CRLF, body.length(), CRLF);
              return String.format("%s%s%s%s%s", STATUS_OK, CRLF, headers, CRLF, body);
          }
      }
      return String.format("%s%s%s%s%s", STATUS_BAD_REQUEST, CRLF, MISSING_HEADER, CRLF, MISSING_HEADER.length());
  }
}
