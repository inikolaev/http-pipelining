import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Created by inikolaev on 8/25/16.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String host = "www.facebook.com";
        String path = "/";

        System.out.println("Resolving hostname");
        InetAddress address = InetAddress.getByName(host);

        System.out.println("Address: " + address.getHostAddress());

        System.out.println("Creating client socket");
        SocketFactory factory = SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(address.getHostAddress(), 443);
        socket.setSoTimeout(250);
        socket.setSendBufferSize(256 * 1024);
        socket.setReceiveBufferSize(256 * 1024);
        socket.startHandshake();

        System.out.println("Preparing request");
        StringBuilder requests = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            boolean close = i == 99;
            requests.append(createRequest(host, path, close));
        }

        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

        byte[] buffer = new byte[64 * 1024];
        StringBuilder response = new StringBuilder();

        System.out.println("Sending request");
        byte[] sendBuffer = requests.toString().getBytes();

        long send = System.currentTimeMillis();
        os.write(sendBuffer);

        long recv = System.currentTimeMillis();
        long ttfb = 0;

        try {
            while (true) {
                int size = is.read(buffer);

                if (size < 0)
                    break;

                if (ttfb == 0) {
                    ttfb = System.currentTimeMillis();
                }

                response.append(new String(buffer, 0, size, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        os.close();
        is.close();
        socket.close();

        System.out.printf("Responses received: %d\n", countResponses(response));
        System.out.printf("Send time:          %d\n", (recv - send));
        System.out.printf("Time to first byte: %d\n", (ttfb - recv));
        System.out.printf("Receive time:       %d\n", (end - recv));
        System.out.printf("Elapsed time:       %d\n", (end - send));
    }

    public static String createRequest(String host, String path, boolean close) {
        return String.format("GET %s HTTP/1.1\r\nHost: %s\r\nConnection: %s\r\n\r\n", path, host, close ? "close" : "keep-alive");
    }

    public static int countResponses(StringBuilder responses) {
        int pos = 0;
        int count = 0;

        while (true) {
            pos = responses.indexOf("HTTP/1.1 302 Found", pos);

            if (pos == -1)
                break;

            pos += 15;
            count++;
        }

        return count;
    }
}
