package p;
import static java.lang.Math.E;
public class A {
	class Inner {
		static class InnerInner {
			static class InnerInnerInner {}
		}
		public void doit() {
			foo();
			fred++;
			double e= E;
			new Stat();
		}
	}
	static void foo(){};
	static int fred;
	static class Stat{}
}