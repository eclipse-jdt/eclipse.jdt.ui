package p;
public class A{
	public static int F;
	public static void m(){
		F= 0;
		new A().F= 0;
		new A().i().F= 0;
		new A().i().i().F= 0;
	}
	A i(){
		return this;
	}
}