package receiver_in;

public class TestNameImplicitReceiver {
	private class ImplicitReceiver {
		public void foo() {
			bar();
		}
		public void bar() {
		}
	}
	
	public void main() {
		ImplicitReceiver a= new ImplicitReceiver();
		
		/*]*/a.foo();/*[*/
	}
}