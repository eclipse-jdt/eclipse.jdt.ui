package receiver_in;

public class TestExpressionZeroImplicitReceiver {
	private class ImplicitReceiver {
		public void foo() {
			System.out.println("Eclipse");
		}
	}

	public void main() {
		/*]*/getObject().foo();/*[*/
	}
	
	private ImplicitReceiver getObject() {
		return new ImplicitReceiver();
	}
}