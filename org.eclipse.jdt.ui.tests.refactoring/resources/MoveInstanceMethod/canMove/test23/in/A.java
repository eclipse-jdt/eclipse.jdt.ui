package p;
public class A {
	Second s;
	Second s2;
	public void print() {
		s.foo(s);
		int s= 17;
		s= 18;
		this.s.foo(s2);
		this.s2.foo(this.s);
		System.out.println(this.s.str);
		this.getClass();
	}
}

