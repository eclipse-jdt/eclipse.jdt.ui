package p;

class A {
	int x;
}

class B extends A {
	void m() { 
		new B(){
			void f(){
				super.x++;
			}
		};
	}
}
