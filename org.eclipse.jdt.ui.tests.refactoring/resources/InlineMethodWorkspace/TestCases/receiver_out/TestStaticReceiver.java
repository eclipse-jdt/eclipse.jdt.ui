package receiver_out;

public class TestStaticReceiver {
	private static class StaticReceiver {
		public static void foo() {
			bar();
		}
		public static void bar() {
		}
	}

	public void main() {
		StaticReceiver.bar();
	}
}