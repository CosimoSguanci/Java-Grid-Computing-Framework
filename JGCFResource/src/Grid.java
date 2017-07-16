
import java.net.*;
import org.hyperic.sigar.*;
import java.io.*;
import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author Cosimo Sguanci
 */
public class Grid {

    private static final double MIN_IDLE_CPU = 50.00;
    private static final int PORT = 8888;
    private static final String IP = "127.0.0.1";

    public static final int IMAGE = 0;
    public static final int RANGE_NUM_FILE = 1;
    public static final int NUM_FILE = 2;
    public static final int IMAGE_ON_SERVER = 3;

    public static final int STATE_UNDEFINED = 0;
    public static final int STATE_IDLE = 1;
    public static final int STATE_BUSY = 2;

    private Socket sock;
    private Sigar sigar;
    private DataInputStream input;
    private DataOutputStream output;
    private Task task;
    private AtomicBoolean threadRunning;
    private Thread mainThread = null;
    private MainThread runnable = null;
    private int currentState;

    public void init(String projectId) {
        try {
            System.setProperty("javax.net.ssl.trustStore", "jgcf.store");
            sock = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(IP, PORT);
            input = new DataInputStream(sock.getInputStream());
            output = new DataOutputStream(sock.getOutputStream());

            output.writeUTF("Resource");
            output.writeUTF(projectId);

            sigar = new Sigar();

            threadRunning = new AtomicBoolean(false);
            
            File rDir = new File("C:/JGCF");
            if (!rDir.isDirectory()) {
                rDir.mkdir();
            }
            
            File f = new File("C:/JGCF/JGCFResource");
            if (!f.isDirectory()) {
                f.mkdir();
            }
            
            System.out.println("Connessione sicura al server eseguita con successo");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void start() throws Exception {
        currentState = ((sigar.getCpuPerc().getIdle() * 100) > MIN_IDLE_CPU) ? STATE_IDLE : STATE_BUSY;
        ObjectOutputStream oos = new ObjectOutputStream(output);
        ObjectInputStream ois = new ObjectInputStream(input);
        new Thread(new MonitorThread(ois, oos)).start();

    }

    public void setTask(Task task) {
        this.task = task;
    }

    protected synchronized void setCurrentState(int state) {
        currentState = state;
    }

    protected synchronized int getCurrentState() {
        return currentState;
    }

    class MonitorThread implements Runnable {

        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public MonitorThread(ObjectInputStream ois, ObjectOutputStream oos) {
            this.ois = ois;
            this.oos = oos;
        }

        @Override
        public void run() {

            new Thread(new StateSender()).start();

            try {
                while (true) {
                    int state = getCurrentState();
                    if (state == STATE_IDLE) {
                        runnable = new MainThread(ois, oos);
                        mainThread = new Thread(runnable);
                        mainThread.start();
                        threadRunning.set(true);
                        mainThread.join();
                        threadRunning.set(false);

                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    class StateSender implements Runnable {

        byte[] sendData = new byte[1024];
        DatagramSocket datagramSocket;
        InetAddress IPAddress;

        public StateSender() {
            try {
                datagramSocket = new DatagramSocket();
                IPAddress = InetAddress.getByName(IP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {

                try {
                    /** 
                     * Grazie alle API Sigar ottengo la percentuale di CPU libera (IDLE).
                     * La costante MIN_IDLE_CPU indica il livello di percentuale minimo 
                     * di CPU libera per poter effettuare elaborazioni.
                     */
                    if ((sigar.getCpuPerc().getIdle() * 100) < MIN_IDLE_CPU) { 

                        Thread.sleep(10000);

                        if ((sigar.getCpuPerc().getIdle() * 100) < MIN_IDLE_CPU) {
                            setCurrentState(STATE_BUSY);
                            sendData = ByteBuffer.allocate(4).putInt(STATE_BUSY).array();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, 
                                    sendData.length, IPAddress, 8888);
                            datagramSocket.send(sendPacket);
                        }

                    } else {
                        setCurrentState(STATE_IDLE);
                        sendData = ByteBuffer.allocate(4).putInt(STATE_IDLE).array();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, 
                                sendData.length, IPAddress, 8888);
                        datagramSocket.send(sendPacket);
                    }

                    Thread.sleep(5000);
                } catch (Exception e) {
                    // Eccezione (e.printStackTrace())
                }

            }

        }
    }

    class MainThread implements Runnable {

        private volatile boolean shouldStop = false;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public MainThread(ObjectInputStream ois, ObjectOutputStream oos) {
            this.ois = ois;
            this.oos = oos;

        }

        public void stopThread() {
            shouldStop = true;
        }

        @Override
        public void run() {
            while (!shouldStop) {
                try {

                    String dataExt = input.readUTF(); 
                    String packetType = input.readUTF();
                    byte[] buffer = (byte[]) ois.readObject(); // Leggo il dato da elaborare
                    System.out.println("Dati Ricevuti");
                    String path = "c:/JGCF/JGCFResource/temp." + dataExt;
                    FileOutputStream fos = new FileOutputStream(path);
                    fos.write(buffer); // Salvo il file di input temporaneo
                    /**
                     * task è un oggetto di una qualsiasi classe estenda la classe astratta Task. 
                     * In questo modo è possibile implementare qualsiasi tipo di task,
                     * e si ottiene una piattaforma il più generica possibile.
                     * 
                     * setInputData(): Imposto i dati di input del task.
                     * run(): Eseguo il codice del task
                     */
                    task.setInputData(new File(path), "C:/JGCF/JGCFResource/");
                    task.run();
                    FileInputStream fis = new FileInputStream(
                            new File("c:/JGCF/JGCFResource/result." + dataExt));
                    byte[] result = new byte[fis.available()];
                    fis.read(result);
                    oos.writeObject(result); // Invio il risultato al server
                    oos.flush();
                    fis.close();
                    System.out.println("Task completato");
                    new File("c:/JGCF/JGCFResource/result." + dataExt).delete();
                    input.readInt();

                    if (packetType.equals("Last")) {
                        
                    }

                    this.shouldStop = true;

                } catch (Exception e) {
                    System.out.println("Errore nella comunicazione con il server");
                    System.exit(-1);
                }
            }

        }
    }

    public void sendResultsToServer(int[] data, int taskID) throws Exception {
        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
        os.writeInt(data.length);

        for (int i = 0; i < data.length; i++) {
            os.writeInt(data[i]);
        }

    }

    public void sendResultsToServer(int data, int taskID) throws Exception {
        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
        os.writeInt(data);
        os.flush();
    }

}
