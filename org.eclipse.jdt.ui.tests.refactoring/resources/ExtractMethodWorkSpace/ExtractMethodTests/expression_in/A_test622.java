package expression_in;

public class A_test622 {
	public A_test622(int i) {
	}
	static class Inner extends A_test622 {
		public Inner() {
			super(/*[*/5 + 6/*]*/);
		}
	}
}
