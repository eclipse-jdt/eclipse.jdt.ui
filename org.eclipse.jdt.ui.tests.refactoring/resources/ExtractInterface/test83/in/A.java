package p;
class A {
	private int x;
	void f(A[] v){
		A[] a= v;
		g(a);
	}
	void g(A[] x){
		x[0].x= 0;
	}
}