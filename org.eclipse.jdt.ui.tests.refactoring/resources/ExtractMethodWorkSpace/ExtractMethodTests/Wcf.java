public class Wcf {
	void hg(){
		class A extends B {
			void m(){
				f();   //<<<<<<<<
			}
		}
	}  
}  

class B {
	void f(){}
}
