package p;

class A {
	int x;
	void m() { 
		new B(){
			void f(){
				super.x++;
			}
		};
	}
}

class B extends A {
}
