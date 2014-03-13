package argument_out;

class Bug {
	{
		int y=4;
		final int x = y;
		new Runnable(){
		public void run(){
			System.out.println(x);
		}};
	}
	void foo(final int x){
		new Runnable(){
			public void run(){
				System.out.println(x);
			}};
	}
}