package p;
public class A{
	public static A f;
	public void n(){
		f= null;
		A.f= null;
		p.A.f= null;
	}
}