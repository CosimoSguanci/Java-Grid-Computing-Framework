package jgcfserver;

import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Cosimo Sguanci
 */
public class User {

    private Socket socket;
    private String uniqueID;
    private String projectId;
    private int fileMode;
    private String filePath;
    private String ipAddress;
    private File[] resultsList;

    public User(Socket socket, String uniqueID, String projectId, int fileMode, String filePath, String IP) {
        this.socket = socket;
        this.uniqueID = uniqueID;
        this.fileMode = fileMode;
        this.filePath = filePath;
        this.projectId = projectId;
        this.ipAddress = IP;
        
    }

    public int getFileMode() {
        return fileMode;
    }

    public String getFilePath() {
        return filePath;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getIP() {
        return ipAddress;
    }

    public String getUniqueID() {
        return uniqueID;
    }
    
    public void setupResultsList(int elements){
        resultsList = new File[elements];
    }

    public synchronized void addToResultsList(File result, int index) {
        resultsList[index] = result;
    }

    public File[] getResultsList() {
        return resultsList;
    }

}
