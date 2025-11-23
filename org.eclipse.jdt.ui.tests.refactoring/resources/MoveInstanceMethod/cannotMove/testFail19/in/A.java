package p1;

public class A {
	B b;

	public void method() {
		new ArrayList<Integer>().stream().filter(this::isPositive).collect(Collectors.toList());
	}

	private boolean isPositive(Integer number) {
		return number != null && number > 0;
	}
}
class B {

}