package receiver_out;

public class TestReceiverWithStatic {
	private static class ThisReceiver {
		public void foo() {
			bar();
			baz();
		}
		public void bar() {
		}
		public static void baz() {
		}
	}

	public void main() {
		ThisReceiver a= new ThisReceiver();
		
		a.bar();
		ThisReceiver.baz();
	}
}