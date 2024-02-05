package p;

public class SubClass extends SuperClass {
	int intParent= 10;

	public static void bar(SubClass subClass) {
		subClass.intParent++;
	}
}
