import java.util.*;

class A_testLocalVarCast_in {
	public void foo(){
		ArrayList x = new ArrayList();
		List o = x;
		List y = (List)o;
	}
}
