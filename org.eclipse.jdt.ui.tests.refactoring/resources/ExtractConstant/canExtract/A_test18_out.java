//5, 16 -> 5, 17   AllowLoadtime == false,  qualifyReferencesWithClassName= true
package p;
class ClassName {
	private static final int CONSTANT= 0;

	int f() {
		return ClassName.CONSTANT;
	}
	
	class Nested {
		{
			System.out.println(ClassName.CONSTANT);	
		}
		
		void f() {
			int i= ClassName.CONSTANT;
		}
	}
}