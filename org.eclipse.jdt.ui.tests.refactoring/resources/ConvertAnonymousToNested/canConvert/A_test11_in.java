package p;
class A{
	void f(){
		final int u= 9;
		new A(){
			int k= u;
		};
	}
}