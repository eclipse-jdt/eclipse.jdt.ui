package p;

class A {
	int x;
	protected void m() { 
		new B(){
			void f(){
				super.x++;
			}
		};
	}

}

class B extends A {
	}
