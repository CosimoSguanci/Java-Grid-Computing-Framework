package jgcfserver;

/**
 *
 * @author Cosimo Sguanci
 */
public class JGCFServer {

    public static void main(String[] args) throws Exception {
        ManagementNode management = new ManagementNode();
        management.waitForClients();
    }

}
