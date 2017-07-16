
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.*;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author Cosimo Sguanci
 */
public class Grid {

    /*
    Costanti per identificare il tipo di file inviato al server.
     */
    public final int IMAGE = 0;
    public final int RANGE_NUM_FILE = 1;
    public final int NUM_FILE = 2;
    public final int IMAGE_ON_SERVER = 3;

    private static final int PORT = 8888;
    private static final String IP = "127.0.0.1";
    private Socket socket;
    OutputStream socketOutputStream;
    InputStream socketInputStream;

    public boolean init() {
        try {
            System.setProperty("javax.net.ssl.trustStore", "jgcf.store");
            socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(IP, PORT);
            
            File f = new File("C:/JGCF/JGCFUser");
            if (!f.isDirectory()) {
                f.mkdir();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void sendData(FileInputStream fis, String projectId, int fileMode, String ext) 
            throws IOException {
        DataOutputStream d = new DataOutputStream(socket.getOutputStream());
        String s = "User"; // Specifico che sono un utilizzatore della piattaforma
        d.writeUTF(s);
        d.flush();
        d.writeUTF(projectId); // Invio dell'identificativo del progetto
        d.flush();
        d.writeInt(fileMode);
        d.flush();
        d.writeUTF(ext);
        byte[] data = new byte[fis.available()];
        fis.read(data);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(data); // Invio dei dati di input
    }

    public File getResult(String ext) 
            throws Exception {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        byte[] buffer = (byte[]) ois.readObject(); // Leggo il risultato dell'elaborazione
        File file = new File("C:/JGCF/JGCFUser/Risultato." + ext);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(buffer);
        return file;
    }

}
