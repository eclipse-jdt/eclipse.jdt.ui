package p;

class A {
	int x;
}

class B extends A {
	void m() { 
		class T extends A{
			void t(){
				super.x++;
			}
		};
	}
}
