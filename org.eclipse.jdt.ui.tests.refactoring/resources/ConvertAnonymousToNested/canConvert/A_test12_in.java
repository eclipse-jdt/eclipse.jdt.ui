package p;
class A{
	A(int x){
	}
	void f(){
		final int u= 9;
		int s= 2;
		new A(s){
			int k= u;
		};
	}
}