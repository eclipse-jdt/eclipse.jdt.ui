package p;

public class SelectConstructor_in {
	public /*[*/SelectConstructor_in/*]*/() {
	}
	public void test(String msg) {
	}
	public static void main(String[] args) {
		SelectConstructor_in sc= createSelectConstructor_in();

		sc.test("hello");
	}
	public static SelectConstructor_in createSelectConstructor_in() {
		return new SelectConstructor_in();
	}
}
