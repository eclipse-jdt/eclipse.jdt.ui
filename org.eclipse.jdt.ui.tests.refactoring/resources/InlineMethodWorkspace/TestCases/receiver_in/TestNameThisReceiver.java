package receiver_in;

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
		
		/*]*/a.foo();/*[*/
	}
}