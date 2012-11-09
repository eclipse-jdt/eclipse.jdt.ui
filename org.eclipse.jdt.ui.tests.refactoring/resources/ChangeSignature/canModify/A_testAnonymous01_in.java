//swap par1 and par2
package p;

class A {
	public A(Object obj) { }

	public void m() {
		new A(new Object() {
			public void a(Object par1, Object par2) { }
		});
	}
}
