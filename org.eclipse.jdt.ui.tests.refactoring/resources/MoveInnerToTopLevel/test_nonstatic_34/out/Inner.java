package p;
public class Inner {

	private final A a;

	public Inner(A a) {
		super();
		this.a= a;
		System.out.println(getName());
	}

	public String getName() {
		return this.a.getTopName() + ".Inner";
	}
}