package p;

public class A {
	public int y = 1;
	public class C {

		public void foo() {
			Outer.x = 7;
		}
		
	}
	public class Outer {
		static int x;
		public void f() {}
		
		public class B extends C {
		}
	}
}