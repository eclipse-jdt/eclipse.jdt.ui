package import_in;

public class TestUseInArgument {
	public void main() {
		Provider p= null;
		/*]*/p.useInArgument(p.useAsReturn());/*[*/
	}
}
