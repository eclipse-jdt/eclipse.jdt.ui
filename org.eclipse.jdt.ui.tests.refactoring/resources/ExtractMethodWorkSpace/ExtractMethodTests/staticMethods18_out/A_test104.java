package staticMethods_in;

interface A_test104 {
	static int extracted() {
		/*[*/int i = 0;/*]*/
		return i;
	}

	Runnable r= new Runnable() {
		@Override
		public void run() {
			int i = extracted();
			System.out.println(i);
		}
	};
}