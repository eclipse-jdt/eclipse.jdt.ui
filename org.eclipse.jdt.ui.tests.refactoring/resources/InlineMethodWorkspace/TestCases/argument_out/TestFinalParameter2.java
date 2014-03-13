package argument_out;

class Bug {
	void m(String y) {
		final String x = y.substring(3, 10);
		new Runnable(){
			public void run(){
				System.out.println(x.trim());
			}};
		x.trim();
	}
	void foo(final String x){
		new Runnable(){
			public void run(){
				System.out.println(x.trim());
			}};
	    x.trim();
	}
}