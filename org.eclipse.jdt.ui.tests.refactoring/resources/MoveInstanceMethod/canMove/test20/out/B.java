package p;

public class B {
	private String fName;
	
	public B(String name) {
		fName= name;
	}
		
	public String toString() {
		return fName;
	}

	/**
	 * Print
	 */
	public void print() {
		System.out.println(
			new StarDecorator() {
				public String decorate(String in) {
					return "(" + super.decorate(in) + ")";
				}
			}.decorate(toString())
		);
	}
}
