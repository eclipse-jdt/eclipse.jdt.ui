package p;

class A {
	public void m() {}
	public void m1() {}
	void t(){
		new A(){
			void g(){
				A.this.m1();
			}
		};	
	}
}