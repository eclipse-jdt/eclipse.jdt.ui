package p;
public class A{
	public static A i;
	public static A i2;
	void f(){
		A.i.i2= null;
	}
	
}