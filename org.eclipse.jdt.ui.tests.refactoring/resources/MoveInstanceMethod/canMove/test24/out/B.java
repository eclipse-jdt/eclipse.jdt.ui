package p1;
public class B {
	private String fName;
	
	public B(String name) {
		fName= name;
	}
		
	public String toString() {
		return fName;
	}

	public void print() {
		class StarDecorator1 extends StarDecorator{
			public String decorate(String in) {
				return "(" + super.decorate(in) + ")";
			}
		}
		System.out.println(
			new StarDecorator1().decorate(toString())
		);
	}
}


