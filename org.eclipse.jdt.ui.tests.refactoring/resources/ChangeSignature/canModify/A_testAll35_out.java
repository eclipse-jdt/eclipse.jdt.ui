package p;
//only visibility change - to public
class A{
	public int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}