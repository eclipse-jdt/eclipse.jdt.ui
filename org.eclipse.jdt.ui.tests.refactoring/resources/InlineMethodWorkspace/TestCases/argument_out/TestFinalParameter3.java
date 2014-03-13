;

class Bug {
	{
		final int y=4;
		new Runnable(){
		public void run(){
			System.out.println(y);
		}};
	}
	void foo(final int x){
		new Runnable(){
			public void run(){
				System.out.println(x);
			}};
	}
}