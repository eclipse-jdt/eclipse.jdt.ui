package p;
class A{
	void f(){
		int u= 9;
		new A(){
			void g(){
				int uj= u;
			}
		};
	}
}