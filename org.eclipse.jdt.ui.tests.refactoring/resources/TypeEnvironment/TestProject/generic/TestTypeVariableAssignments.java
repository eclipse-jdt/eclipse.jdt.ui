package generic;

import java.util.Collection;

public class TestTypeVariableAssignments<A, B extends Number & Collection<String>, C extends A> {
	Object o= null;
	
	A a;
	B b;
	C c;
	
	Number number;
	Integer integer;
	Collection<String> coll_string;	
}
