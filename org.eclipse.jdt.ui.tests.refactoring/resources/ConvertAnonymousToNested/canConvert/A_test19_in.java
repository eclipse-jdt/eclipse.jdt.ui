package p;
class A{
	void f(){
		final int u= 8;
		new A(){
			int l= 9;
			int p0= 2, k= u, k1= k;
			int l1= l+1, p, q= p+u;
		};
	}
}