import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Main {
    private static final String MISSING_HEADER = "Missing User-Agent header";
  public static void main(String[] args) {
      // You can use print statements as follows for debugging, they'll be visible when running tests.
      System.out.println("Logs from your program will appear here!");

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       Socket clientConnection =  serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");
       BufferedReader reader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream(), StandardCharsets.UTF_8));
       String requestLine = reader.readLine();
       if (requestLine == null) {
           System.out.println("Empty request or connection closed.");
       } else {
           String response = createResponse(requestLine, reader);
           OutputStream output = clientConnection.getOutputStream();
           output.write(response.getBytes());
       }

         serverSocket.close();
         clientConnection.close();
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  static String createResponse(String requestLine, BufferedReader reader) throws IOException {
      String urlPath = requestLine.split(" ")[1];
      if (Objects.equals(urlPath, "/")) {
          return "HTTP/1.1 200 OK\r\n\r\n";
      } else if (urlPath.startsWith("/echo")) {
          return handleEchoRequest(urlPath);
      } else if (urlPath.startsWith("/user-agent")) {
          return handleUserAgentRequest(reader);
      } else {
          return "HTTP/1.1 404 Not Found\r\n\r\n";
      }
  }

  private static String handleEchoRequest(String path) {
      String prefix = "/echo/";
      String body = "";
      if (path.startsWith(prefix)) {
          body = path.substring(prefix.length());
      }

      String headers = String.format("Content-Type: text/plain\r\nContent-Length: %d\r\n", body.length());
      return String.format("HTTP/1.1 200 OK\r\n%s\r\n%s", headers, body);
  }

  private static String handleUserAgentRequest(BufferedReader bufferedReader) throws IOException {
      String prefix = "User-Agent:";
      String line;
      while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
          if (line.startsWith(prefix)) {
              System.out.println("inside prefix\n");
              String body = line.split(" ")[1];
              System.out.println("body is " + body);
              String headers = String.format("Content-Type: text/plain\r\nContent-Length: %d\r\n", body.length());
              return String.format("HTTP/1.1 200 OK\r\n%s\r\n%s", headers, body);
          }
      }
      return String.format("HTTP/1.1 400 Bad Request\r\n%s\r\n%s", MISSING_HEADER, MISSING_HEADER.length());
  }
}
