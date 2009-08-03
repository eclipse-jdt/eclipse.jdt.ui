package destination_out;

public class A_test1052 {
	public static void foo() {
		Runnable r= new Runnable() {
			public void run() {
				extracted();
			}
		};
	}

	protected static void extracted() {
		/*[*/System.out.println();/*]*/
	}
}
