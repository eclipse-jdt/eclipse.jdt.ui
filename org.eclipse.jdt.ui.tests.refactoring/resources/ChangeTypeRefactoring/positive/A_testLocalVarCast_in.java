import java.util.*;

class A_testLocalVarCast_in {
	public void foo(){
		ArrayList x = new ArrayList();
		ArrayList o = x;
		List y = (List)o;
	}
}
