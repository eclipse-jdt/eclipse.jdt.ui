package p; // 16, 28, 16, 36

public class A {
	String[] Lines;
	int i;

	private void inc() {
		this.i++;
	}

	void foo() {
		System.out.println(Lines[i]);
		for (int i = 0; i < Lines.length; ++i) {
			System.err.print(Lines[i]);
		}
		System.out.println(Lines[i]);
		inc();
		System.out.println(Lines[i]);
	}
}
