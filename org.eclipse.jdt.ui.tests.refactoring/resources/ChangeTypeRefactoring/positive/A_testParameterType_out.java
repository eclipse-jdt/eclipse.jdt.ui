import java.util.*;

class A_testParameterType_in {
	public void foo(Dictionary table){
		table = new Hashtable();
		table.put("foo", "bar");
	}
}
