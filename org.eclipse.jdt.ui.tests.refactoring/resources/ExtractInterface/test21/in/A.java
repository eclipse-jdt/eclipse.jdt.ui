package p;

class A extends Exception {
	public void m() {}
	void e() throws A{}
	void g() {
		try{
			e();
		} catch (A a){
		}
	}
}