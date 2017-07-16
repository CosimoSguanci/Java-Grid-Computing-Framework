package jgcfserver;

import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Files;
import java.util.UUID;
import java.awt.*;
import java.nio.*;
import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 * @author Cosimo Sguanci
 */
public class ManagementNode {

    /*
    Costanti per identificare il tipo di file da elaborare.
     */
    public final int IMAGE = 0;
    public final int RANGE_NUM_FILE = 1;
    public final int NUM_FILE = 2;
    public final int IMAGE_ON_SERVER = 3;

    private final int PORT = 8888;
    private final int FINISH_TO_READ = 1;
    private Socket clientSock;
    private ServerSocket serverSock;
    protected List<User> userQueue;
    protected List<Resource> resourceQueue;

    public ManagementNode() {
        try {
            System.setProperty("javax.net.ssl.keyStore", "jgcf.store");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            serverSock = ((SSLServerSocketFactory) SSLServerSocketFactory.getDefault()).createServerSocket(PORT);
            userQueue = new ArrayList();
            userQueue = Collections.synchronizedList(userQueue);
            resourceQueue = new ArrayList();
            resourceQueue = Collections.synchronizedList(resourceQueue);

            File f = new File("C:/JGCF/JGCFServer");
            if (!f.isDirectory()) {
                f.mkdir();
            }

            System.out.println("Avvio JGCF Server...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitForClients() throws IOException {

        new Thread(new ResourceMonitor()).start();
        System.out.println("In Attesa di client...");

        while (true) {
            clientSock = serverSock.accept();
            DataInputStream input = new DataInputStream(clientSock.getInputStream());
            DataOutputStream output = new DataOutputStream(clientSock.getOutputStream());

            String clientType = input.readUTF();
            String projectId = input.readUTF();
            String id = UUID.randomUUID().toString().substring(15);
            
            switch (clientType) { 
                case "Resource":
                    String ipRes = clientSock.getInetAddress().toString();
                    Resource resource = new Resource(clientSock, projectId, input, output, id, ipRes);
                    addToResourceQueue(resource);
                    resource.setObjectOutputStream(new ObjectOutputStream(resource.getOutputStream()));
                    resource.setObjectInputStream(new ObjectInputStream(resource.getInputStream()));
                    new Thread(new ResourceLifeChecker(resource)).start();
                    System.out.println("Risorsa collegata");
                    System.out.println("IP: " + ipRes);
                    System.out.println("ID: " + resource.getUniqueID());
                    break;
                case "User":
                    File f = new File("C:/JGCF/JGCFServer/" + id);
                    if (!f.isDirectory()) {
                        f.mkdirs();
                    }
                    int fileMode = input.readInt();
                    String ipUsr = clientSock.getInetAddress().toString();
                    User user = new User(clientSock, id, projectId, fileMode, f.getAbsolutePath(), ipUsr);
                    addToUserQueue(user);
                    new Thread(new UserService(user)).start();
                    System.out.println("User collegato");
                    System.out.println("IP: " + ipUsr);
                    System.out.println("ID: " + user.getUniqueID());
                    break;
            }

        }
    }

    public synchronized void addToUserQueue(User user) {
        userQueue.add(user);
    }

    public synchronized void addToResourceQueue(Resource resource) {
        resourceQueue.add(resource);
    }

    public synchronized Resource getResource(int index) {
        if (index >= 0) {
            return resourceQueue.get(index);
        } else {
            return null;
        }
    }

    public synchronized void removeFromResourceQueue(Resource resource) {
        resourceQueue.remove(resource);
    }

    public synchronized int getResourceQueueSize() {
        return resourceQueue.size();
    }

    class UserService implements Runnable {

        private User user;
        private List<File> chunksList;
        private ArrayList<Integer> mustBeProcessed;
        private final int NUM_CHUNKS_IMAGE = 9;
        private final int NUM_CHUNKS_NUMRANGE = 10;

        public UserService(User user) {
            this.user = user;

        }

        protected synchronized int getProcessIndexes(int indexRes) {
            return mustBeProcessed.get(indexRes);
        }

        protected synchronized boolean hasToBeProcessed(int indexRes) {
            if (mustBeProcessed.contains(indexRes)) {
                return true;
            } else {
                return false;
            }
        }

        protected synchronized void decreaseProcessedIndexes(Integer remove) {
            mustBeProcessed.remove(remove);
        }

        protected synchronized void increaseProcessedIndexes(int add) {
            mustBeProcessed.add(add);
        }

        private void splitImage(File f, String path, String ext) throws Exception {
            FileInputStream fis = new FileInputStream(f);
            BufferedImage image = ImageIO.read(fis);
            int rows = 3;
            int cols = 3;
            int chunks = rows * cols;
            int chunkWidth = image.getWidth() / cols;
            int chunkHeight = image.getHeight() / rows;
            int count = 0;
            BufferedImage imgs[] = new BufferedImage[chunks];

            for (int x = 0; x < rows; x++) {
                for (int y = 0; y < cols; y++) {
                    imgs[count] = new BufferedImage(chunkWidth, chunkHeight, image.getType());
                    Graphics2D gr = imgs[count++].createGraphics();
                    gr.drawImage(image, 0, 0, chunkWidth, chunkHeight, chunkWidth * y, chunkHeight * x, chunkWidth * y + chunkWidth, chunkHeight * x + chunkHeight, null);
                    gr.dispose();
                }
            }

            for (int i = 0; i < imgs.length; i++) {
                File img = new File(path + "/" + "img" + i + "." + ext);
                ImageIO.write(imgs[i], ext, img);
                chunksList.add(img);
            }
        }

        private void mergeImages(File[] results, String path, String ext) throws Exception {
            int rows = 3;
            int cols = 3;
            int chunks = rows * cols;
            int chunkWidth, chunkHeight;
            int type;
            BufferedImage[] buffImages = new BufferedImage[chunks];
            for (int i = 0; i < chunks; i++) {
                buffImages[i] = ImageIO.read(results[i]);
            }
            type = buffImages[0].getType();
            chunkWidth = buffImages[0].getWidth();
            chunkHeight = buffImages[0].getHeight();
            BufferedImage finalImg = new BufferedImage(chunkWidth * cols, chunkHeight * rows, type);

            int num = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    finalImg.createGraphics().drawImage(buffImages[num], chunkWidth * j, chunkHeight * i, null);
                    num++;
                }
            }
            ImageIO.write(finalImg, ext, new File(path + "/RESULT." + ext));
        }

        private void mergeFiles(File[] files, File into) throws IOException {
            try (BufferedOutputStream mergingStream = new BufferedOutputStream(
                    new FileOutputStream(into))) {
                for (File f : files) {
                    Files.copy(f.toPath(), mergingStream);
                }
            }
        }

        @Override
        public void run() {

            int fileMode = user.getFileMode();
            Socket socket = user.getSocket();
            chunksList = new ArrayList<>();
            mustBeProcessed = new ArrayList<>();
            int indexRes = 0;
            int indexProcessing = 0;
            String fileExt = null;
            String filePath = null;
            File resDir = null;
            int i = 0;
            int k = 0;
            switch (fileMode) {
                case IMAGE:
                    try {
                        user.setupResultsList(NUM_CHUNKS_IMAGE);
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        filePath = user.getFilePath();
                        fileExt = input.readUTF();
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        byte[] buffer = (byte[]) ois.readObject();
                        File file = new File(filePath + "/input." + fileExt);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(buffer);

                        splitImage(file, filePath, fileExt);

                        resDir = new File(filePath + "/Results");
                        if (!resDir.isDirectory()) {
                            resDir.mkdir();
                        }
                        int z = 0;
                        for (File f : chunksList) {
                            increaseProcessedIndexes(z);
                            z++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    /**
                     * mustBeProcessed rappresenta la lista degli input parziali ancora da elaborare.
                     * ResourceQueue è la coda delle risorse connesse, grazie al metodo getState() 
                     * ottengo lo stato attuale della risorsa.
                     * chunksList è una lista che contiene gli effettivi oggetti della classe File, 
                     * che vengono inviati alle risorse libere.
                     * Una volta spedito il dato da elaborare, viene avviato il thread TaskHandler 
                     * che si occuperà di ricevere il risultato dalla risorsa corrispondente.
                     * In questo modo si evita di bloccare il thread principale in attesa di ogni 
                     * singolo risultato, ma si realizza un parallelismo reale che consente di ottenere 
                     * l'effettivo beneficio prestazionale.
                     */
                    while (!mustBeProcessed.isEmpty()) {

                        while (k < getResourceQueueSize()) {
                            try {
                                if (getResource(k).getState() == Resource.STATE_IDLE && 
                                        getResource(k).getProjectId().equals(user.getProjectId())) {
                                    Resource resource = getResource(k);
                                    indexRes = k;
                                    resource.getOutputStream().writeUTF(fileExt);
                                    i = getProcessIndexes(0);
                                    indexProcessing = i;
                                    FileInputStream fis = new FileInputStream(chunksList.get(i));
                                    byte[] data = new byte[fis.available()];
                                    fis.read(data);

                                    if (mustBeProcessed.size() != 1) {
                                        resource.getOutputStream().writeUTF("Normal");
                                    } else {
                                        resource.getOutputStream().writeUTF("Last");
                                    }

                                    resource.getOutputStream().flush();
                                    resource.getObjectOutputStream().writeObject(data);
                                    resource.getObjectOutputStream().flush();

                                    Thread t = new Thread(new TaskHandler(data, resource, user, 
                                            resource.getObjectOutputStream(), resource.getObjectInputStream(), 
                                            filePath, fileExt, i, this));
                                    t.start();
                                    decreaseProcessedIndexes(i);

                                    if (mustBeProcessed.isEmpty()) {
                                        t.join();
                                    }
                                }

                                if (k == (getResourceQueueSize() - 1) && !mustBeProcessed.isEmpty()) {
                                    k = 0;
                                } else {
                                    if (mustBeProcessed.isEmpty()) {
                                        k = getResourceQueueSize();
                                    } else {
                                        k++;
                                    }
                                }
                                Thread.sleep(2500);
                            } catch (SocketException e) {
                                System.out.println("Risorsa con ID " 
                                        + resourceQueue.get(indexRes).getUniqueID() + " disconnessa");
                                try {
                                    getResource(indexRes).getSocket().close();
                                    removeFromResourceQueue(getResource(indexRes));
                                    if (!hasToBeProcessed(indexProcessing)) {
                                        increaseProcessedIndexes(indexProcessing);
                                    }
                                    continue;

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    boolean containsNull = true;
                    while (containsNull) {
                        for (int j = 0; j < user.getResultsList().length; j++) {
                            containsNull = false;
                            if (user.getResultsList()[j] == null) {
                                containsNull = true;
                                break;
                            }
                        }

                    }

                    try {
                        mergeImages(user.getResultsList(), resDir.getPath(), fileExt);
                        FileInputStream fis = new FileInputStream(resDir.getPath() + "/RESULT." + fileExt);
                        byte[] data = new byte[fis.available()];
                        fis.read(data);
                        new ObjectOutputStream(user.getSocket().getOutputStream()).writeObject(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;

                case RANGE_NUM_FILE:
                    try {
                        user.setupResultsList(NUM_CHUNKS_NUMRANGE);
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        filePath = user.getFilePath();
                        fileExt = input.readUTF();
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        byte[] buffer = (byte[]) ois.readObject();
                        File file = new File(filePath + "/input." + fileExt);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(buffer);

                        Scanner scanner = new Scanner(file);
                        ArrayList<Integer> inputNumsChunks = new ArrayList<>();
                        int[] inputNums = new int[2];

                        while (scanner.hasNextInt()) {
                            inputNums[i++] = scanner.nextInt();
                        }

                        int nums = inputNums[1] - inputNums[0];
                        final int numberOfPieces = 10;
                        final int quotient = nums / numberOfPieces;
                        final int remainder = nums % numberOfPieces;
                        int z;
                        int[] results = new int[numberOfPieces];
                        for (z = 0; z < numberOfPieces; z++) {
                            results[z] = z < remainder ? quotient + 1 : quotient;
                        }
                        i = 0;
                        int y = 0;
                        int first = inputNums[0];
                        z--;
                        while (y < nums) {
                            String p = filePath + "/input" + i + "." + fileExt;
                            int second = y + results[z--];
                            try {
                                PrintWriter writer = new PrintWriter(p);
                                writer.println(first);
                                writer.println(second);
                                writer.close();
                            } catch (IOException e) {

                            }
                            chunksList.add(new File(p));
                            y = second;
                            first = second;
                            i++;
                        }

                        resDir = new File(filePath + "/Results");
                        if (!resDir.isDirectory()) {
                            resDir.mkdir();
                        }

                        z = 0;
                        for (File f : chunksList) {
                            increaseProcessedIndexes(z);
                            z++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    while (!mustBeProcessed.isEmpty()) {
                        while (k < resourceQueue.size()) {
                            try {
                                if (resourceQueue.get(k).getState() == Resource.STATE_IDLE && resourceQueue.get(k).getProjectId().equals(user.getProjectId())) {
                                    indexRes = k;
                                    resourceQueue.get(k).getOutputStream().writeUTF(fileExt);
                                    i = getProcessIndexes(0);
                                    FileInputStream fis = new FileInputStream(chunksList.get(i));
                                    byte[] data = new byte[fis.available()];
                                    fis.read(data);
                                    if (mustBeProcessed.size() != 1) {
                                        resourceQueue.get(k).getOutputStream().writeUTF("Normal");
                                    } else {
                                        resourceQueue.get(k).getOutputStream().writeUTF("Last");
                                    }
                                    resourceQueue.get(k).getOutputStream().flush();
                                    resourceQueue.get(k).getObjectOutputStream().writeObject(data);
                                    resourceQueue.get(k).getObjectOutputStream().flush();

                                    Thread t = new Thread(new TaskHandler(data, resourceQueue.get(k), user, resourceQueue.get(k).getObjectOutputStream(), resourceQueue.get(k).getObjectInputStream(), filePath, fileExt, i, this));
                                    t.start();
                                    decreaseProcessedIndexes(i);
                                    try {
                                        if (mustBeProcessed.isEmpty()) {
                                            t.join();
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }

                                if (k == (resourceQueue.size() - 1) && !mustBeProcessed.isEmpty()) {
                                    k = 0;
                                } else {
                                    if (mustBeProcessed.isEmpty()) {
                                        k = resourceQueue.size();
                                    } else {
                                        k++;
                                    }

                                }
                                Thread.sleep(2500);
                            } catch (SocketException e) {
                                
                                System.out.println("Risorsa con ID " + resourceQueue.get(indexRes).getUniqueID() + " disconnessa");
                                try {
                                    getResource(indexRes).getSocket().close();
                                    removeFromResourceQueue(getResource(indexRes));
                                    if (!hasToBeProcessed(indexProcessing)) {
                                        increaseProcessedIndexes(indexProcessing);
                                    }
                                    continue;

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                    
                    boolean cntNull = true;
                    while (cntNull) {
                        for (int j = 0; j < user.getResultsList().length; j++) {
                            cntNull = false;
                            if (user.getResultsList()[j] == null) {
                                cntNull = true;
                                break;
                            }
                        }

                    }

                    try {
                        mergeFiles(user.getResultsList(), new File(resDir.getPath() + "/RESULT." + fileExt));
                        FileInputStream fis = new FileInputStream(resDir.getPath() + "/RESULT." + fileExt);
                        byte[] data = new byte[fis.available()];
                        fis.read(data);
                        new ObjectOutputStream(user.getSocket().getOutputStream()).writeObject(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case NUM_FILE:
                    break;
                case IMAGE_ON_SERVER:
                    break;
            }

        }
    }

    class TaskHandler implements Runnable {

        private byte[] input;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private Resource resource;
        private User user;
        private String filePath;
        private String fileExt;
        private int i;
        private UserService userService;

        public TaskHandler(byte[] input, Resource resource, User user, ObjectOutputStream oos, ObjectInputStream ois, String filePath, String fileExt, int i, UserService userService) {
            this.input = input;
            this.resource = resource;
            this.user = user;
            this.oos = oos;
            this.fileExt = fileExt;
            this.filePath = filePath;
            this.i = i;
            this.ois = ois;
            this.userService = userService;
        }

        @Override
        public void run() {
            try {

                resource.setState(Resource.RUNNING_TASK);
                byte[] buffer = (byte[]) ois.readObject();
                resource.getOutputStream().writeInt(FINISH_TO_READ);
                resource.getOutputStream().flush();
                File file = new File(filePath + "/Results/result" + i + "." + fileExt);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buffer);
                user.addToResultsList(file, i);
                resource.setState(Resource.STATE_UNDEFINED);

            } catch (SocketException e) {
                System.out.println("Risorsa con ID " + resource.getUniqueID() + " disconnessa");
                try {
                    resource.getSocket().close();
                    resourceQueue.remove(resource);
                    if (!userService.hasToBeProcessed(i)) {
                        userService.increaseProcessedIndexes(i);
                    }
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

    }

    class ResourceMonitor implements Runnable {

        private Resource resource;

        public ResourceMonitor() {
            this.resource = resource;
        }

        @Override
        public void run() {
            try {
                DatagramSocket serverSocket = new DatagramSocket(8888);
                while (true) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    String sentence = new String(receivePacket.getData());
                    String udpIP = receivePacket.getAddress().toString();
                    int state = ByteBuffer.wrap(receivePacket.getData()).getInt();

                    switch (state) {
                        case Resource.STATE_IDLE:
                            System.out.println("RICEVUTO STATO IDLE DA " + udpIP);
                            break;
                        case Resource.STATE_BUSY:
                            System.out.println("RICEVUTO STATO OCCUPATO DA " + udpIP);
                    }

                    for (Resource resource : resourceQueue) {
                        if (resource.getIP().equals(udpIP) && resource.getState() != Resource.RUNNING_TASK) {
                            resource.setState(state);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    class FaultToleranceMonitor implements Runnable {

        private InetAddress inetAddress;
        private int i;
        private UserService userService;
        private TaskHandler taskHandler;
        private Resource resource;

        public FaultToleranceMonitor(Resource resource, InetAddress inetAddress, int i, UserService userService) {
            this.inetAddress = inetAddress;
            this.i = i;
            this.userService = userService;
            this.resource = resource;
        }

        @Override
        public void run() {

            String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                while (true) {
                    try {
                        Process p = Runtime.getRuntime().exec("ping " + inetAddress.getHostAddress());
                        BufferedReader inputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));

                        String s = "";
                        String result = "";
                        while ((s = inputStream.readLine()) != null) {
                            result = result + s;
                        }

                        if (result.contains("Richiesta scaduta") || result.contains("Request timed out")) {
                            userService.increaseProcessedIndexes(i);
                            resourceQueue.remove(resource);
                            resource.getSocket().close();
                            Thread.currentThread().interrupt();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

        }
    }

    class ResourceLifeChecker implements Runnable {

        private Resource resource;
        private InetAddress inetAddress;

        public ResourceLifeChecker(Resource resource) {
            this.resource = resource;

            this.inetAddress = resource.getSocket().getInetAddress();
        }

        @Override
        public void run() {

            String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                while (true) {
                    try {
                        Process p = Runtime.getRuntime().exec("ping " + inetAddress.getHostAddress());
                        BufferedReader inputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));

                        String s = "";
                        String result = "";
                        while ((s = inputStream.readLine()) != null) {
                            result = result + s;
                        }

                        if (result.contains("Richiesta scaduta") || result.contains("Request timed out")) {
                            System.out.println("Risorsa con ID " + resource.getUniqueID() + " disconnessa");
                            resource.getSocket().close();
                            resourceQueue.remove(resource);
                            Thread.currentThread().interrupt();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            } else if (os.contains("Linux")) {
                while (true) {
                    ////
                }
            }

        }
    }

}
