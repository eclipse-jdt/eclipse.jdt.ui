package p;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @see #ZYXteg(List)
 */
interface I {
	Set<Set<Runnable>> ZYXteg(List<Set<Runnable>> arg);
}

class Impl implements I {
	public Set<Set<Runnable>> ZYXteg(List<Set<Runnable>> arg) {
		return null;
	}
}

class User {
	void call(I abc) {
		Set<Set<Runnable>> s= abc.ZYXteg(new ArrayList<Set<Runnable>>());
	}
}
