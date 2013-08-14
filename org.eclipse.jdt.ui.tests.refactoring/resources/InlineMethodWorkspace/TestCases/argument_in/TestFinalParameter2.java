package argument_in;

class Bug {
	void m(String y) {
		/*]*/foo(y.substring(3, 10));/*[*/
	}
	void foo(final String x){
		new Runnable(){
			public void run(){
				System.out.println(x.trim());
			}};
	    x.trim();
	}
}