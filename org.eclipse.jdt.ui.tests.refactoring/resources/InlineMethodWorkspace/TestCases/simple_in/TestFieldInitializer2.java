package simple_in;

public class TestFieldInitializer2 {
	
	public void foo() {
		class Woo {
			private int field= /*]*/goo()/*[*/;
			
			public int goo() {
				return 1;
			}
		}
	}
}
