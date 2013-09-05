package staticMethods_in;

interface A_test104 {
	Runnable r= new Runnable() {
		@Override
		public void run() {
			/*[*/int i = 0;/*]*/
			System.out.println(i);
		}
	};
}