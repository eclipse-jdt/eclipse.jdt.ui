package p;

class A{
	void f(){
		new Inner(this){
			void ft(){
				new Inner(A.this);
			}
		};
	}
}