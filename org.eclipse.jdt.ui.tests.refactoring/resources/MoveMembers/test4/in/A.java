package p;
public class A{
	public static void m(){}
	public void n(){
		A.m();
		this.m();
		p.A.m();
		m();
	}
}