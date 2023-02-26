package p; // 16, 28, 16, 36

public class A {
	String[] Lines;
	int i;

	private void inc() {
		this.i++;
	}

	void foo() {
		String string= Lines[i];
		System.out.println(string);
		for (int i = 0; i < Lines.length; ++i) {
			System.err.print(Lines[i]);
		}
		System.out.println(string);
		inc();
		String string2= Lines[i];
		System.out.println(string2);
	}
}
