package p;
//only visibility change - to public
class A{
	private int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}