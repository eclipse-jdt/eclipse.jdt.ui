package base_in;

public class TestPrefixInt {
	int field;
	
	public void foo() {
		++field;
		--field;
		int i;
		i= +field;
		i= - field;
		i= ~field;
	}
}
