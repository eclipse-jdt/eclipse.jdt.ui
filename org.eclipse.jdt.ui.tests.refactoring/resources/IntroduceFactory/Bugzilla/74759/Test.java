package p;

public class Test {

	public/*[*/Test/*]*/(Test tal) {
		return;
	}

	public static void main(String[] args) {
		final Test test= new Test(new Test(null));

		System.out.println(new Test(decorate(new Test(null))));
	}

	private static Test decorate(Test test) {
		return test;
	}
}
