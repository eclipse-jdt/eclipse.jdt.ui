package call_out;

import java.util.List;

public class TestParenthesis {
	Object list;
	
	public void main() {
		Object element= ((List)list).get(0);
	}
	
	public List getList() {
		return (List)list;
	}
}
