package p;
//disabled: constructor, field, method, final
class A{
	void f(){
		new Object(){
			void g(){
				int i;
			}
		};
	}
}