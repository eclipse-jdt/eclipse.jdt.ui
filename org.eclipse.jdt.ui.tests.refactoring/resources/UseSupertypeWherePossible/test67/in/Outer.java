package p;
public class Outer{
	public static class Implementor implements Inter{	
		public void work(A a) {}
	}
	Implementor implementor;
	void f(){
		A a= new A();
		implementor.work(a);
	}
}	
