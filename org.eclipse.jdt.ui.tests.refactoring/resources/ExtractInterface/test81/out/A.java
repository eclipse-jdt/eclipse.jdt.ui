package p;
class A implements I {
	private int x;
	private A[] v2= null; 
	void f(A[] v){
		A[] a= v;
		a[0].x= 0;
		A[] v1= null;
		a= v1;
		a= v2;
	}
}