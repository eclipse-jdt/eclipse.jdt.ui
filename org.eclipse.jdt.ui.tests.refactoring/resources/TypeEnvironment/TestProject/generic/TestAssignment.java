package generic;

import java.util.List;

public class TestAssignment<T, E extends T> {
	List<? extends T> lhs;
	List<E> rhs;
	{
		lhs= rhs;
	}
}
