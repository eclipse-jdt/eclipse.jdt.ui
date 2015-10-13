package trycatch18_out;

import java.io.FileNotFoundException;
import java.util.Optional;


public class TestMethodReference6 {
	private static Transformer2 TRANSFORMER = new Transformer2();

	public void test() {
		Optional.ofNullable("10").map(/*]*/arg0 -> {
			try {
				return TRANSFORMER.transform(arg0);
			} catch (FileNotFoundException e) {
			}
		}/*[*/).ifPresent(System.out::print);
	}
}

class Transformer2 {
	Long transform(String number) throws FileNotFoundException {
		return null;
	}
}