package p;
public class A{
	public static A f;
	public void n(){
		f= null;
		this.f= null;
		A.f= null;
		p.A.f= null;
		f.f= null;
	}
}