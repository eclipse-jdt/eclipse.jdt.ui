package receiver_out;

public class TestNoImplicitReceiver {
	class NoImplicitReceiver {
		public void foo() {
			System.out.println("Eclipse");
		}
	}

	public void main() {
		NoImplicitReceiver a= new NoImplicitReceiver();
		
		System.out.println("Eclipse");
	}
}