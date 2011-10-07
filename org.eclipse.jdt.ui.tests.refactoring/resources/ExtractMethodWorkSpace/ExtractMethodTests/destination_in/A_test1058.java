package destination_in;

public class A_test1058 {
	private enum B {
		X, Y;
		
		private void extracted() {
			new Runnable() {
				public void run() {
					System.out.println(/*[*/2 + /**/ 3/*]*/);
				}
			}.run();
		}
	}
}
