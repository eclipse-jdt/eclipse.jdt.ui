package p;

public class B {
	private static interface I {
		public void foo();
	}
	private static class CC implements I {

		public void foo() {
		}
	}
	
	public static A createA() {
		return new A();
	}

	public void foo() {
		I i= new I() {
			public void foo() {
			}
			public void bar() {
				foo();
			}
		};
		
		CC c= new CC() {};
		B b;
	}
	
	public void bar() {
		class X {
			public void baz() {
				
			}
		}
		
		class Y extends X {
			public void baz() {
				
			}
		}
	}
}
