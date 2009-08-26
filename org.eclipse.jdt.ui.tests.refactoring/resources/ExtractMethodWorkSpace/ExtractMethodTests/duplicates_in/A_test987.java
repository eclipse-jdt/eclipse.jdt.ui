package duplicates_in;

public class A_test987 {
	A_test987 other;
	int x;
	protected void f() {
		Object o= /*[*/other/*]*/;
		x = 1;
        other.x= 1;
        other.other.x= 5;
	}

}