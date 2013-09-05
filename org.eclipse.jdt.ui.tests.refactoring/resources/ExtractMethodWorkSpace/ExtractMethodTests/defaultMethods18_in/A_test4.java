package staticMethods_in;

interface A_test4 {
	default void foo() {
		Runnable r= new Runnable() {
			@Override
			public void run() {
				/*[*/int i = 0;/*]*/
			}
		};		
	}
}