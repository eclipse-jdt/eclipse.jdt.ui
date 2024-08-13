package destination_in;

public class A_test1059 {
	interface B {
		@interface C {
			int i= /*[*/extracted();/*]*/
		}

		static int extracted() {
			return 0;
		}		
	}
}