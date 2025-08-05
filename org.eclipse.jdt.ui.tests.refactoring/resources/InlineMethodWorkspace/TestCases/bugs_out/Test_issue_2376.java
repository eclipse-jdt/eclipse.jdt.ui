package bugs_in;

public class Test_issue_2376 {

	@FunctionalInterface
	public interface I {
		int run();
	}

	public void foo2(int k) {
		String x = "abc";
		int a = x.length();
		if (k == 0) {
			String x1 = "abc";
			x1.length();
		}
		I blah= () -> {String x1 = "abc"; return x1.length();};
		I blah2= () -> {String x1 = "abc"; return x1.length();};
		String x1 = "abc";
		int b = x1.length();
	}

}