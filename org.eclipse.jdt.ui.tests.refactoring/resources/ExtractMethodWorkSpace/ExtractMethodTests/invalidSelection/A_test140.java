package invalidSelection;

public class A_test140 {
	public boolean flag;
	public int foo() {
		int i= 10;
		/*]*/switch(i) {
			case 1:
				if (flag)
					break;
				foo();	
			case 2:
				return 10;
			default:
				throw new NullPointerException();
		}/*[*/
			
		return 10;	
	}
}