package p;

public class A {
	private int stop() {
		return 2;
	}
}

class B extends A {
	public void stop() //<-- pull up this method
	{
		System.out.println("pulled up!");
	}
}