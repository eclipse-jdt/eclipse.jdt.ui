package p;

class A {
	int x;
}

class B extends A {
	protected void m() { 
		class T extends A{
			void t(){
				super.x++;
			}
		};
	}
}
