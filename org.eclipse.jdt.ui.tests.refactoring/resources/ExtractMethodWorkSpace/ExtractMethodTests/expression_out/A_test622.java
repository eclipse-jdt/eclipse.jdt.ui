package expression_out;

public class A_test622 {
	public A_test622(int i) {
	}
	static class Inner extends A_test622 {
		public Inner() {
			super(extracted());
		}

		protected static int extracted() {
			return /*[*/5 + 6/*]*/;
		}
	}
}
