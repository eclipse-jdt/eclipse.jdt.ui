package receiver_out;

public class TestImplicitReceiverMethod {
	public void foo() {
		/*]*/faz();
		faz();/*[*/
	}
	public void bar() {
		faz();
		faz();
	}
	public void faz() {
	}
}
