package p1;
public class A {
	public static void main(String[] args) {
		A a= new A();
		B b= new B("Gugguseli");
		a.print(b);
	}

	public void print(B b) {
		b.print();
	}
}
