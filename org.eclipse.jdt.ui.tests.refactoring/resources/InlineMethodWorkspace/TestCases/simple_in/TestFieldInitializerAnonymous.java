package simple_in;

public class TestFieldInitializerAnonymous {

	Object field = new Object() {
		{
			if (0 < hashCode())
				;
			else
				/*]*/foo()/*[*/;
		}
	};

	void foo() {
		toString();
		toString();
	}
}
