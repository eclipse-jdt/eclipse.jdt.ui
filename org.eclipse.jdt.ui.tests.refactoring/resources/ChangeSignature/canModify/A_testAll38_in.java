package p;
//to protected
class A{
	int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}
class B extends A{
	public int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}