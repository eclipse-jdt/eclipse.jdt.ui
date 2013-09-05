package destination18_in;

public class A_test204 {
	interface B {
		@interface C {
			int i= /*[*/extracted();/*]*/
		}		
	}

	protected static int extracted() {
		return 0;
	}
}