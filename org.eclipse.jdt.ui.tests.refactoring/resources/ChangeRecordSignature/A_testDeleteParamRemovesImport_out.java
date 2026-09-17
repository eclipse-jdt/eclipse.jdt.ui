package p;

record A(int a){
}
class B {
	protected void m(){
		A a = new A(1);
	}
}
