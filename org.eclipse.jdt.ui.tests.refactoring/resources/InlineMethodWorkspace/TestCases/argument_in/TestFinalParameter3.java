package argument_in;

class Bug {
	{
		final int y=4;
		/*]*/foo(y);/*[*/
	}
	void foo(final int x){
		new Runnable(){
			public void run(){
				System.out.println(x);
			}};
	}
}