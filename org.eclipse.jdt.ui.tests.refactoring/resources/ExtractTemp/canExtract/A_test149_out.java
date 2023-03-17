package p; // 6, 13, 6, 29

public class A {
	void m(int x) {
		if (x > 1) {
			int calculateCount= calculateCount();
			System.out.println(calculateCount + 1);
		} else
			System.out.println(calculateCount());
		{
			int y= calculateCount();
		}

		calculateCount();
	}

	private int calculateCount() {
		return 0;
	}
}
