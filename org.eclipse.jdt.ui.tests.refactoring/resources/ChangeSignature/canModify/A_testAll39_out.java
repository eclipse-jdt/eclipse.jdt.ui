package p;
//to public
class A{
	public int m(boolean jj, Object x, int i){
		return m(false, x, m(jj, x, i));
	}
}
class B extends A{
	public int m(boolean jj, Object x, int i){
		return m(false, x, m(jj, x, i));
	}
}