package p;
import static java.lang.Math.cos;
public class A {
	class Inner {
		static class InnerInner {
			static class InnerInnerInner {}
		}
		public void doit() {
			foo();
			fred++;
			double d= cos(0);
			new Stat();
		}
	}
	static void foo(){};
	static int fred;
	static class Stat{}
}