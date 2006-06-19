import java.io.*;

public class A_testCatchClause_in {
    public void f(File f) {
        try {
            FileInputStream is = new FileInputStream(f);
        } catch (IOException ex) {
            System.err.println("file not found");
        }
    }
}
