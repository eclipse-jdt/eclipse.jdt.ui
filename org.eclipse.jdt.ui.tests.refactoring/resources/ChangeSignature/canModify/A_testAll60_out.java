package p;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

class A {
	/**
	 * @param tho {@link #getList}
	 * @param tho
	 * @param l TODO
	 * @param to 1st param of {@link A#getList(long, List, int) me}
	 * @param to 2nd
	 * @version throwaway
	 * @throws Exception TODO
	 * @see #getList(long, List, int)
	 * @see #getList(long tho, List l, int to)
	 * @return the list
	 */
	public List getList(long tho, List l, int to) throws Exception {
		//change to: java.util.List getList(long tho, List l, int to) throws Exception
		// (swap parameters, insert new in between, change return type, change Ex.)
		return new ArrayList((int)tho-to);
	}
}

interface I {
	public List getList(long tho, List l, int to) throws Exception;
}

interface J {
	/** Doc: @param t t 
	 * @param l TODO
	 * @throws Exception TODO*/
	public List getList(long t, List l, int f) throws FileNotFoundException, Exception;
}

class B extends A implements I, J {
	/**
	 * @return {@inheritDoc} 
	 * @see p.A#getList(long, List, int)
	 */
	public List getList(long tho, List l, int to) throws FileNotFoundException, Exception {
		return new ArrayList() {};
	}
}
