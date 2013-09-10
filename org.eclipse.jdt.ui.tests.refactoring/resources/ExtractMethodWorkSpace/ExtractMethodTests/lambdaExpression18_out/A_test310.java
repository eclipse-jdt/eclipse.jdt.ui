package lambdaExpression18_in;

@FunctionalInterface
public interface I {
	int foo(int a);
}

class C_Test {
	String bar() {
		I i4= a -> {
			class X {
				float bar() {
					return extracted();
				}

				private float extracted() {
					/*[*/ return  100; /*]*/
				}
			}
			return a++;
		};
		
		return "";
	}	
}