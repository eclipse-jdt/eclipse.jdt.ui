package p;
//to public
class A{
	public int m(boolean jj, int[] x, int i){
		return m(false, null, m(jj, null, i));
	}
}
class B extends A{
	public int m(boolean j, int[] x, int iii){
		return m(false, null, m(j, null, iii));
	}
}