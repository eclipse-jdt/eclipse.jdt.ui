package p;
public class A {
	class Inner {
		static class InnerInner {
			static class InnerInnerInner {}
		}
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