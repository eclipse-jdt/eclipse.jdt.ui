package p;
public class A{
	public static A F;
	public static void m(){
		F= null;
		new A().F= null;
		new A().i().F= null;
		new A().i().i().F= null;
		F.F= null;
		F.F.F= null;
	}
	A i(){
		return this;
	}
}