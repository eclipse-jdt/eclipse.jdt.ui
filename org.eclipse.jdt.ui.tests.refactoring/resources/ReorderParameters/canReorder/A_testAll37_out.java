package p;
//to protected
class A{
	protected int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}
class B extends A{
	protected int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}