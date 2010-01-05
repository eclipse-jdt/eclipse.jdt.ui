package p;

public class Thing {
	private Thing[] subthings;

	public Thing() {
		subthings = new Thing[] {};
	}

	public /*[*//*]*/Thing(Thing... subthings) {
		this.subthings = subthings;
	}

	public static void main(String args[]) {
		System.out.println(new Thing(new Thing(new Thing()), new Thing(
				new Thing())).subthings.length);
	}
}
