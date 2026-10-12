package p;
record A(int a, String b){
}
class B {
	protected void m(){
		A obj = new A(1, "string");
	}
}
