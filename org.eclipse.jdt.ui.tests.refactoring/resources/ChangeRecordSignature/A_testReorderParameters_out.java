package p;
record A(String b, int a){
}
class B {
	protected void m(){
		A a = new A("string", 1);
	}
}
