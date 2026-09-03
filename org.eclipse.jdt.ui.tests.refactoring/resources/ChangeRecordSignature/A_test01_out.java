package p;
record A(int a, String b, String c){
}
class B {
	protected void m(){
		A a = new A(1, "string", "default");
	}
}
