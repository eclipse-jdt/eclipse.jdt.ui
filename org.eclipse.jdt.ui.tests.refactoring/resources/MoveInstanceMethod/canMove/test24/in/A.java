package p1;
public class A {
	public static void main(String[] args) {
		A a= new A();
		B b= new B("Gugguseli");
		a.print(b);
	}

	public void print(B b) {
		class StarDecorator1 extends StarDecorator{
			public String decorate(String in) {
				return "(" + super.decorate(in) + ")";
			}
		}
		System.out.println(
			new StarDecorator1().decorate(b.toString())
		);
	}
}
