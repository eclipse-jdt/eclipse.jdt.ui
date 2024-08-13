package p1;

public class A {
	public B b;

	public void m() {
		System.out.println("d");
	}
}

class B {

}

class C {

	public void m() {
		System.out.println("c");
	}

	public void foo() {
		class D extends B {
			public void t() {
				m();
			}
		}
	}

}


