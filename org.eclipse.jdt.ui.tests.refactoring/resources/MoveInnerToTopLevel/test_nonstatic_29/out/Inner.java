package p;
public class Inner extends A.OtherInner {
	private A a;
	Inner(A a) {
		a.super();
		this.a = a;
	}}