package simple_out;

public class TestFieldInitializerAnonymous {

	Object field = new Object() {
		{
			if (0 < hashCode())
				;
			else {
				toString();
				toString();
			}
		}
	};

	void foo() {
		toString();
		toString();
	}
}
