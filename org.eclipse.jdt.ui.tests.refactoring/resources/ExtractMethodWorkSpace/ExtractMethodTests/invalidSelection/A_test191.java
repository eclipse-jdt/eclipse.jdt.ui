package invalidSelection;

public class A_test191 {
	private boolean foo(){
		A_test191 m= new A_test191();
		/*[*/if (m == null)
			return true;
		A_test191 d= m;/*]*/
		if (d == null)
			return true;	
		return true;
	}

}

