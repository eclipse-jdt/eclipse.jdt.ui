package p;

public class B {
	private int count;

	public void f() {
		Inner s= new Inner();
		print(s);
	}
	
	public void print(Inner s) {
		s.print(this);
	}	

	public class Inner {

		public void print(B b) {
			b.count++;
			System.out.println(this);
		}
	}
}
