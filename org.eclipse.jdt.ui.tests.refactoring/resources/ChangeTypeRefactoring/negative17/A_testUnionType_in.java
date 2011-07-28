import java.io.FileNotFoundException;
import java.io.InterruptedIOException;

public class A_testUnionType_in {

	void foo(int a) {
		try {
			if (a < 10)
				throw new FileNotFoundException();
			else if (a < 20)
				throw new InterruptedIOException();
		} catch (FileNotFoundException | InterruptedIOException ex) {
			ex.printStackTrace();
		}
	}
}
