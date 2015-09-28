package trycatch18_out;

public class TestLambda1 {
	private TestLambda1 log;
	void test() {
		Runnable r = () -> {
			try {
				/*[*/log.info("Processing rule #{} {}", "")/*]*/;
			} catch (GridException1 e) {
			}
		};
	}

	private void info(String string, Object object) throws GridException1 {}
}
@SuppressWarnings("serial")
class GridException1 extends Exception {}