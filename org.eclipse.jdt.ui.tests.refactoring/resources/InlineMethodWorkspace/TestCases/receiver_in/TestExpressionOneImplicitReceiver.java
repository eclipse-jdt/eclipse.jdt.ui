package receiver_in;

public class TestExpressionOneImplicitReceiver {
	private class ImplicitReceiver {
		public void foo() {
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