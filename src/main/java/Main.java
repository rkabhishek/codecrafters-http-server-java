import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Main {
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

       String response = createResponse(clientConnection);
       OutputStream output = clientConnection.getOutputStream();
       output.write(response.getBytes());
       clientConnection.close();
       serverSocket.close();
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  static String createResponse(Socket clientConnection) throws IOException {
      InputStream in = clientConnection.getInputStream();
      InputStreamReader isReader = new InputStreamReader(in, StandardCharsets.UTF_8);
      BufferedReader bufferedReader = new BufferedReader(isReader);

      String line = bufferedReader.readLine();
      String urlPath = line.split(" ")[1];
      if (Objects.equals(urlPath, "/")) {
          return "HTTP/1.1 200 OK\r\n\r\n";
      } else if (urlPath.startsWith("/echo")) {
          return handleEchoRequest(urlPath);
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
}
