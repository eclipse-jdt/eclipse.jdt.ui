package p;
//to public
class A{
	public int m(boolean jj, Object x, int i){
		return m(false, null, m(jj, null, i));
	}
}
class B extends A{
	public int m(boolean j, Object x, int iii){
		return m(false, null, m(j, null, iii));
	}
}