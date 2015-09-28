package trycatch18_out;

import java.io.FileNotFoundException;
import java.util.Optional;


public class TestMethodReference1 {
	private static Transformer2 TRANSFORMER = new Transformer2();

	public void test() {
		try {
			/*[*/Optional.ofNullable("10").map(TRANSFORMER::transform).ifPresent(System.out::print);/*]*/
		} catch (Exception e) {
		}
	}
}

class Transformer2 {
	Long transform(String number) throws FileNotFoundException {
		return null;
	}
}