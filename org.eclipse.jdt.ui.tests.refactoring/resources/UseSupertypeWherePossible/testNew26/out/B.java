package p;
public class B {
	public B(A a){
		a.m();
	}
	public B(int y, A a){
		this(a);
	}
	public void m() {	}
}