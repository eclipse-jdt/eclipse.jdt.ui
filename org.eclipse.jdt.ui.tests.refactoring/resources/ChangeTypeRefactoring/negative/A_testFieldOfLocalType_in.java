public class A_testFieldOfLocalType_in {
	
	public void foobar() {
		class Listener3 {
			private A_testFieldOfLocalType_in fTest;
			
			private Listener3() {
				fTest= new A_testFieldOfLocalType_in();
			}
			
			public int bar() {
				return foo();
			}
			
			public int foo() {
				return 1;
			}
			
			private String getProperty() {
				return null;
			}
		}
		
		this.addListener(new Listener3() {
			public int bar() {
				return 1;
			}
		});
	}
	
	
	public void addListener(Object o) {
	}

}
