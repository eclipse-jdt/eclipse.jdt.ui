import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

class A_testFieldName_in {
	void foo(){
		list = new ArrayList();
		List list2 = list;
	}
	
	public static AbstractList list;
}
