package error_in;

public class A_test804{
	public void foo1(){
		System.out.println("Hello world");
	}
	
	public void foo2(){
		extracted();
	}

	protected void extracted() {
		/*[*/{
			System.out.println("Hello world");
		}/*]*/
	}
}
