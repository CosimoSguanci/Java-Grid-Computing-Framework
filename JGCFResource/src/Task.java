
import java.io.File;

/**
 *
 * @author Cosimo Sguanci
 */
public abstract class Task {

    public abstract void run();

    public abstract void setInputData(File input, String path);
}
