package p;
//disabled: constructor
class A{
	void f(){
		new Object(){
			void g(){
				int i= 0;
			}
		};
	}
}