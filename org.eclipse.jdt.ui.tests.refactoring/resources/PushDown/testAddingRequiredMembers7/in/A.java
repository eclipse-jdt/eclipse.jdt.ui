//m added to f
package p;
class A{	
	private int m(){
		return 2;
	}
	private int f= m();
}
class B extends A{
}