package receiver_out;

public class TestExpressionOneImplicitReceiver {
	private class ImplicitReceiver {
		public void foo() {
			bar();
		}
		public void bar() {
		}
	}

	public void main() {
		getObject().bar();
	}
	
	private ImplicitReceiver getObject() {
		return new ImplicitReceiver();
	}
}