package duplicates_out;

public class A_test972 {
	class X {
		public String toString() {
			extracted();
			return null;
		}

		protected void extracted() {
			/*[*/System.out.println("hello world");/*]*/
		}
	};
	
	void f(){
		System.out.println("hello world");
	}
}
