package tryresources18_in;

public class TestLambda1 {
	private TestLambda1 log;
	void test() {
		Runnable r = () -> /*[*/log.info("Processing rule #{} {}", "")/*]*/;
	}

	private void info(String string, Object object) throws GridException1 {}
}
@SuppressWarnings("serial")
class GridException1 extends Exception {}