import java.io.IOException;

public class Loo {
	void foo() throws IOException {
		try {
			throw new IOException();
		} catch (IOException e) {
		}
	}
	void goo() {
		try {
			foo();
		} catch (IOException e) {
			System.err.println();
		}
	}
}
