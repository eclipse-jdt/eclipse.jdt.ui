package p;
public class A {
	class Inner {
		public void doit() {
			foo();
			fred++;
			new Stat();
		}
	}
	static void foo(){};
	static int fred;
	static class Stat{}
}