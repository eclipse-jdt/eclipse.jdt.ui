package import_in;

import java.util.List;

public class TestUseInDeclClash {
	List fList;
	public void main() {
		Provider p= null;
		/*]*/p.useInDecl();/*[*/
	}
}
