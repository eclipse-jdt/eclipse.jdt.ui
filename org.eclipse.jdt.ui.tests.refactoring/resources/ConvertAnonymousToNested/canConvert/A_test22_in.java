package p;
class A {
	interface I	{
		void bar();
	}
	public static void foo(){
		I foo= new I() {
			public void bar() {
				I foo = new I() {
					public void bar(){
					}
				};
			}
		};
	}
}
