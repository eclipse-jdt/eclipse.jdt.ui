package p;

class A {
	int x;
}

class B extends A {
	public void m() { 
		new B(){
			void f(){
				super.x++;
			}
		};
	}
}
