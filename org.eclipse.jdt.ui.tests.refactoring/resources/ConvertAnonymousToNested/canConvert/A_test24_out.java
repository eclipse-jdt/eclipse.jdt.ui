package p;
class A {
	private static final class Inner extends A {
		public void foo(){
		}
	}

	static A a = new Inner();
}
