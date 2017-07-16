
/**
 *
 * @author Cosimo Sguanci
 */
import java.io.*;

public class Test {

    public static void main(String[] args) throws Exception {
        Grid grid = new Grid();
        if (grid.init()) {

            //FileInputStream fis = new FileInputStream("C:/JGCF/prova7.jpg");
            //grid.sendData(fis, "Project1", grid.IMAGE, "jpg");
            //File result = grid.getResult("jpg");

            FileInputStream fis = new FileInputStream("C:/JGCF/range_num.txt");
            grid.sendData(fis, "Project1", grid.RANGE_NUM_FILE, "txt");
            File result = grid.getResult("txt");
            
            System.out.println("Risultato ricevuto dal server");
        }
    }
}
