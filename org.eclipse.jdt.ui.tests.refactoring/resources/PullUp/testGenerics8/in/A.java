package p;

class A {
	int x;
}

class B<T> extends A {
	public void m() { 
		new B<String>(){
			void f(){
				super.x++;
			}
		};
	}
}
