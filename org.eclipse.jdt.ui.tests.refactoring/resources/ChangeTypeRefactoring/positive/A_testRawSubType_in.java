public class A_testRawSubType_in {
	void foo(Interface i){
		Comparable c= i.getName();
	}
}
interface Interface {
	String getName();
}
