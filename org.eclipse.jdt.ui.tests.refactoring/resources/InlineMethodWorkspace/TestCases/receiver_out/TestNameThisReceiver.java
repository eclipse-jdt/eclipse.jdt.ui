package receiver_out;

public class TestNameThisReceiver {
	class ThisReceiver {
		public void foo() {
			this.bar();
		}
		public void bar() {
		}
	}

	public void main() {
		ThisReceiver a= new ThisReceiver();
		
		a.bar();
	}
}