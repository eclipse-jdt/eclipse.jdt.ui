package bugs_in;

public class Test_287378 {
	Test_287378 other;
	int x;

	protected void f() {
		other.extracted().x = 5;
		int a = other.extracted().x;
		other.extracted();
	}

	protected Test_287378 /*]*/extracted/*[*/() {
		return /*A*/other/*B*/;
	}
}
