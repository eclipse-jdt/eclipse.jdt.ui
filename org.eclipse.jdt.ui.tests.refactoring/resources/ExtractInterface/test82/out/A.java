package p;
class A implements I {
	private int x;
	void f(A[] v){
		A[] a= v;
		a[0].x= 0;
	}
}