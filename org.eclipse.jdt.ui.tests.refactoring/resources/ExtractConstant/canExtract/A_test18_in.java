//5, 16 -> 5, 17   AllowLoadtime == false,  qualifyReferencesWithClassName= true
package p;
class ClassName {
	int f() {
		return 0;
	}
	
	class Nested {
		{
			System.out.println(0);	
		}
		
		void f() {
			int i= 0;
		}
	}
}