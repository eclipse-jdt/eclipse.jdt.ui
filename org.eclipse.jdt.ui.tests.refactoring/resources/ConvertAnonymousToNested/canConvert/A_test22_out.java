package p;
class A {
	private static final class Inner implements I {
		public void bar(){
		}
	}
	interface I	{
		void bar();
	}
	public static void foo(){
		I foo= new I() {
			public void bar() {
				I foo = new Inner();
			}
		};
	}
}
