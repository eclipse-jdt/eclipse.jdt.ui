package p;

class A {
	int x;

	public void m() { 
		new B(){
			void f(){
				super.x++;
			}
		};
	}
}

class B extends A {
}
