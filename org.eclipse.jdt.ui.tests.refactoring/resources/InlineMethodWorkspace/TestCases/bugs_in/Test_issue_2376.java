package bugs_in;

public class Test_issue_2376 {

	@FunctionalInterface
	public interface I {
		int run();
	}

	public int /*]*/method()/*[*/ {
		String x = "abc";
		return x.length();
	}

	public void foo2(int k) {
		int a = method();
		if (k == 0) {
			method();
		}
		I blah= this::method;
		I blah2= this::method;
		int b = method();
	}

}