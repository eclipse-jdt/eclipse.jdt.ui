package simple_in;

public class TestSurroundingCallers {
	void callOne() {
		toInline();
	}
	
	void toInline() {
		System.out.println("Hello Eclipse");
	}
	
	void callTwo() {
		toInline();
	}
}
