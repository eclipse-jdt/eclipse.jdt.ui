package p;

class A{
	class Inner {
	}
	void f(){
		new Inner(){
			void ft(){
				new Inner();
			}
		};
	}
}