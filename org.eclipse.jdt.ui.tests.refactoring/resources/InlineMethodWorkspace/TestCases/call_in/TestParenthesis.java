package call_in;

import java.util.List;

public class TestParenthesis {
	Object list;
	
	public void main() {
		Object element= /*]*/getList()/*[*/.get(0);
	}
	
	public List getList() {
		return (List)list;
	}
}
