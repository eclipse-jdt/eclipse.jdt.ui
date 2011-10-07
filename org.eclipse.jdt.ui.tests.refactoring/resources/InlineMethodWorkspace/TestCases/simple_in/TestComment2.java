package simple_in;

public class TestComment2 {
	public int toInline(int arg) {
		return 42 * arg;
	}
	public void ref() {
		toInline(5 /*op1*/ * /*op2*/ 2);
	}
}
