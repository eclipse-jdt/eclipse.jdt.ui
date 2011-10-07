package destination_in;

public class A_test1058 {
	private enum B {
		X, Y;
		
		private void extracted() {
			new Runnable() {
				public void run() {
					System.out.println(A_test1058.extracted());
				}
			}.run();
		}
	}

	protected static int extracted() {
		return /*[*/2 + /**/ 3/*]*/;
	}
}
