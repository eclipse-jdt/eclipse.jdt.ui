package p;
class A {
	private int x;
	void f(A[] v){
		A[] a= v;
		a[0].x= 0;
	}
}