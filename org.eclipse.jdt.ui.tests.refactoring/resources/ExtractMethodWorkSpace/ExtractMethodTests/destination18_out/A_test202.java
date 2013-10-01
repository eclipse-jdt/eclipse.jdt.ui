package destination_in;

public class A_test202 {
	@interface C {
		interface B {
			default void foo() {
				int i= /*[*/extracted();/*]*/				
			}

			default int extracted() {
				return 0;
			}
		}
	}		
}