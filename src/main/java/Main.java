import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Created by inikolaev on 8/25/16.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String host = "www.google.com";
        String path = "/";

        System.out.println("Resolving hostname");
        InetAddress address = InetAddress.getByName(host);

        System.out.println("Creating client socket");
        SocketFactory factory = SSLSocketFactory.getDefault();
        Socket socket = factory.createSocket(address.getHostAddress(), 443);
        socket.setSoTimeout(250);
        socket.setReceiveBufferSize(256 * 1024);

        System.out.println("Preparing request");
        StringBuilder pipeline = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            String request = createRequest(host, path);
            pipeline.append(request);
        }

        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        DataInputStream is = new DataInputStream(socket.getInputStream());

        byte[] buffer = new byte[64 * 1024];
        StringBuilder response = new StringBuilder();

        System.out.println("Sending request");
        String pipelineString = pipeline.toString();

        long start = System.currentTimeMillis();
        os.writeBytes(pipelineString);

        try {
            while (true) {
                int size = is.read(buffer);

                if (size < 0)
                    break;

                response.append(new String(buffer, 0, size, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {

        }

        long end = System.currentTimeMillis();

        System.out.printf("Elapsed time: %d\n", (end - start));

        System.out.println(response);

        os.close();
        is.close();
        socket.close();
    }

    public static String createRequest(String host, String path) {
        return String.format("GET %s HTTP/1.1\r\nHost: %s\r\n\r\n", path, host);
    }
}
