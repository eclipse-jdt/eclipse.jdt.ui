package p;
import java.util.*;
class A {
	/**
	 * @param from 1st param of {@link A#getList(int, long) me}
	 * @see getList
	 * @see #getList(int, long)
	 * @see #getList(int from, tho long)
	 * @see #getList(int from, long tho)
	 * @param to
	 * @return list
	 * @param bogus{@link #getList}
	 */
	public ArrayList getList(int from, long to) {
		return new ArrayList((int)to-from);
	}
	
	/** start here
	 * Doesn't call {@linkplain #getList(int, long)}
	 *
	 * @see getList
	 * @see #getList
	 * @see A#getList(int, long)
	 * @see A#getList(int, long, Object[][])
	 * @see A#getList (
	 *   int fro,long tho
	 * )
	 * @see #getList(..)
	 * @see p.A#getList(int, int, boolean)
	 * @see <a href="spec.html#section">Java Spec</a>
	 * @see A# getList(int, long)
	 */
	public ArrayList getList(int from, long to, Object[] arr[]) {
		return new ArrayList(Arrays.asList(arr).subList(from, (int)to));
	}
}
