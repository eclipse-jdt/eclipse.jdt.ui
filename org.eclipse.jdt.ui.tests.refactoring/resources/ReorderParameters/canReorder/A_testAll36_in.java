package p;
//only visibility change - to private
class A{
	public int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}