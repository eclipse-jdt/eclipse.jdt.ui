package p;

class A{
	class Inner {
	}
	void f(){
		new Inner(this){
			void ft(){
				new Inner(A.this);
			}
		};
	}
}