package duplicates_out;

public class A_test987 {
	A_test987 other;
	int x;
	protected void f() {
		Object o= extracted();
		x = 1;
        extracted().x= 1;
        extracted().extracted().x= 5;
	}
	protected A_test987 extracted() {
		return /*[*/other/*]*/;
	}

}