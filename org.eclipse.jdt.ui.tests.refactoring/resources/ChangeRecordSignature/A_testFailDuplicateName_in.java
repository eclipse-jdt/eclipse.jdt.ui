package p;
record A(int a, String b){
}
class B {
	protected void m(){
		A a = new A(1, "string");
	}
}
