package nameconflict_out;

public class TestSameLocal {
	public void main() {
		int i= 10;
		int i1= 20;
		i1++;
	}
	
	public void foo() {
		int i= 20;
		i++;
	}
}
