package p;

class B {
	protected void k(){}
}
//can't rename m to k
class A extends B {
 	private void m(){}
}