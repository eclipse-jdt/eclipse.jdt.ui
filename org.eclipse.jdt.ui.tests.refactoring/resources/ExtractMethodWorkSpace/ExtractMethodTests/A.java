class A {
	int i(){ return 0;}
	void m(){
		/*[*/i();
		m();/*]*/
	}
}

