package trycatch18_in;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.function.Consumer;

public class TestMethodReference2 {
	private static Transformer TRANSFORMER = new Transformer();

	public void test() {
		/*[*/Consumer<Object> s = (a) -> Optional.ofNullable("10").map(TRANSFORMER::transform).ifPresent(System.out::print);/*]*/
	}
}

class Transformer {
	Long transform(String number) throws FileNotFoundException {
		return null;
	}
}