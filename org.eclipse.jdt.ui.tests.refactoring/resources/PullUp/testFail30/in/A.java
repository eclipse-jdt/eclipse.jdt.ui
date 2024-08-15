package p;

public class A {
	public int y = 1;
	public class B2 {
		
	}
	public class Outer {
		int x;
		public void f() {}
		
		public class B extends B2 {
			public void foo() {
				System.out.println(x);
			}
		}
	}
}
