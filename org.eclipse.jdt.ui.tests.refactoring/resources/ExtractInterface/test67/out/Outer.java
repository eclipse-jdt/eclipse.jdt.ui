package p;
public class Outer{
	public static class Implementor implements Inter{	
		public void work(I a) {}
	}
	Implementor implementor;
	void f(){
		I a= new A();
		implementor.work(a);
	}
}	
