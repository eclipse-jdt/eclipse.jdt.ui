package receiver_in;

public class TestNoImplicitReceiver {
	class NoImplicitReceiver {
		public void foo() {
			System.out.println("Eclipse");
		}
	}

	public void main() {
		NoImplicitReceiver a= new NoImplicitReceiver();
		
		/*]*/a.foo();/*[*/
	}
}