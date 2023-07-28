public class Foo {

	class Inner {}

	public static void bar(Foo foo) {
		Inner inner= foo.new Inner();
	}
}
