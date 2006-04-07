package receiver_in;

public class TestImplicitReceiverMethod {
	public void foo() {
		/*]*/bar();/*[*/
	}
	public void bar() {
		faz();
		faz();
	}
	public void faz() {
	}
}
