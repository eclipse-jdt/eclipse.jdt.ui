package p;

class A {
	int x;

	public void m() { 
		new B<String>(){
			void f(){
				super.x++;
			}
		};
	}
}

class B<T> extends A {
}
