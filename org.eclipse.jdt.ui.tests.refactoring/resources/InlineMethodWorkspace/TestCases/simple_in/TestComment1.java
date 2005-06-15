package simple_in;

public class TestComment1 {
	public void toInline() {
		// comment
		bar();
		// comment
		bar();
		// comment
	}
	private void bar() {
	}
	public void ref() {
		toInline();
	}
}
