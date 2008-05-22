import java.io.*;

public class A_testVarArg_in {
	public void checkTrue() {
		Object i= linesPass("B", "A"); // generalize i
	}
	
	private Integer linesPass(String... lines) {
		return 1;
	}
}
