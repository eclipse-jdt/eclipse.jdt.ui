package p;
class Second {
	public String str;
	public void foo(Second s) {
	}
	public void print(A a) {
		foo(this);
		int s= 17;
		s= 18;
		foo(a.s2);
		a.s2.foo(this);
		System.out.println(str);
		a.getClass();
	}
}
