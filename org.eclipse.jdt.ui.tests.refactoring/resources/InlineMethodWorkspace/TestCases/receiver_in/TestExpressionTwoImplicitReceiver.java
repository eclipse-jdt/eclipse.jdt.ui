package receiver_in;

public class TestExpressionTwoImplicitReceiver {
	private class ImplicitReceiver {
		public void foo() {
			bar();
			bar();
		}
		public void bar() {
		}
	}

	public void main() {
		/*]*/getObject().foo();/*[*/
	}
	
	private ImplicitReceiver getObject() {
		return new ImplicitReceiver();
	}
}