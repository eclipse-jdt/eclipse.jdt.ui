public class Test {
	public void foo() {
		class Inner {
			void main() {
				bar();
				
				bar();
			}
			void bar() {
				System.out.println("Eclipse");
			}
		}
	}
}
