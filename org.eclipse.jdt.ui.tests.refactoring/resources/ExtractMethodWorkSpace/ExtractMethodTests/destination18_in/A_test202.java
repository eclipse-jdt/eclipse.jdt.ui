package destination_in;

public class A_test202 {
	@interface C {
		interface B {
			default void foo() {
				int i= /*[*/0;/*]*/				
			}
		}
	}		
}