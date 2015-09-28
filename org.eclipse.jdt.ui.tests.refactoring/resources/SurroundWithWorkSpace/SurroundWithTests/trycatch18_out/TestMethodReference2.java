package trycatch18_out;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.function.Consumer;

public class TestMethodReference2 {
	private static Transformer TRANSFORMER = new Transformer();

	public void test() {
		try {
			/*[*/Consumer<Object> s = (a) -> Optional.ofNullable("10").map(TRANSFORMER::transform).ifPresent(System.out::print);/*]*/
		} catch (Exception e) {
		}
	}
}

class Transformer {
	Long transform(String number) throws FileNotFoundException {
		return null;
	}
}