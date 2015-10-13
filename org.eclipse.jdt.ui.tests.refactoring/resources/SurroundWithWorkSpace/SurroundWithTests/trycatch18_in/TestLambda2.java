package trycatch18_in;

public class TestLambda2 {
	private TestLambda2 log;
	void test() {
		Runnable r = () -> /*[*/log/*]*/.info("Processing rule #{} {}", "");
	}

	private void info(String string, Object object) throws GridException1 {}
}
@SuppressWarnings("serial")
class GridException1 extends Exception {}