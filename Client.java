import java.net.*;
import java.io.*;
import java.util.*;
public class Client {
    public static void main(String args[]) {
        try {

            Socket server = new Socket("iaxp16.inf.uni-jena.de", 1234);
            InputStream in = server.getInputStream();
            OutputStream out = server.getOutputStream();
// write a byte
            out.write(42);
// write a newline or carriage return delimited string
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeUTF("Hello!");

//....
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(new java.util.Date()); oout.flush();
            server.close();
        } catch(UnknownHostException e) {
            System.out.println("Can´t find host.");
        } catch (IOException e) {
            System.out.println("Error connecting to host."); }
    }
}