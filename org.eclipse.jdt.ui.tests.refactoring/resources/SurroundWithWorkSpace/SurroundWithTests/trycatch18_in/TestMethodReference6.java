package trycatch18_in;

import java.io.FileNotFoundException;
import java.util.Optional;


public class TestMethodReference6 {
	private static Transformer2 TRANSFORMER = new Transformer2();

	public void test() {
		Optional.ofNullable("10").map(/*]*/TRANSFORMER::transform/*[*/).ifPresent(System.out::print);
	}
}

class Transformer2 {
	Long transform(String number) throws FileNotFoundException {
		return null;
	}
}