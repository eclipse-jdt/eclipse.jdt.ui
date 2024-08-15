package p;

public class A {
	public int y = 1;
	public class C {
		
	}
	public class Outer {
		int x;
		public void f() {}
		
		public class B extends C {
			public void foo() {
				f();
			}
		}
	}
}
