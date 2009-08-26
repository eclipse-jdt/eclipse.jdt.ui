package bugs_out;

public class Test_287378 {
	Test_287378 other;
	int x;

	protected void f() {
		other./*A*/other/*B*/.x = 5;
		int a = other./*A*/other/*B*/.x;
	}
}
