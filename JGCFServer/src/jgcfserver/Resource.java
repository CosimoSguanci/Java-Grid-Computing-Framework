package jgcfserver;

import java.net.*;
import java.io.*;

/**
 *
 * @author Cosimo Sguanci
 */
public class Resource {

    private Socket socket;
    private int state;
    private String projectId;
    private String uniqueID;
    private String ipAddress;
    private DataInputStream input;
    private DataOutputStream output;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public static final int STATE_UNDEFINED = 0;
    public static final int STATE_IDLE = 1;
    public static final int STATE_BUSY = 2;
    public static final int RUNNING_TASK = 3;

    public Resource(Socket socket, String projectId, DataInputStream input, DataOutputStream output, String uniqueID, String ipAddress) {
        this.socket = socket;
        this.projectId = projectId;
        this.input = input;
        this.output = output;
        this.ipAddress = ipAddress;
        this.uniqueID = uniqueID;
        state = STATE_UNDEFINED;

    }

    public Socket getSocket() {
        return socket;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setObjectInputStream(ObjectInputStream ois) {
        this.ois = ois;
    }

    public void setObjectOutputStream(ObjectOutputStream oos) {
        this.oos = oos;
    }

    public ObjectInputStream getObjectInputStream() {
        return ois;
    }

    public DataInputStream getInputStream() {
        return input;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return oos;
    }

    public DataOutputStream getOutputStream() {
        return output;
    }

    public String getIP() {
        return ipAddress;
    }

    public String getUniqueID() {
        return uniqueID;
    }

}
