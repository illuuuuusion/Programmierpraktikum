import java.net.*;
import java.io.*;
import java.util.*;
public class MySimpleServer {
    public static void main(String args[]) {
        boolean run = true;
        try {
            ServerSocket listener = new ServerSocket(1234);
            while(run) {
                Socket client = listener.accept(); // wait for connection
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();
// read a byte
                byte someByte = (byte) in.read();

                // read a newline or carriage return delimited string
                DataInputStream din = new DataInputStream(in);
                String someString = din.readUTF();
// read a serialized Java object
                ObjectInputStream oin = new ObjectInputStream(in);
                Date date = (Date) oin.readObject();
                client.close();
            }
            listener.close();
        } catch (Exception e) {}
    }
}