package p;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

class A {
	/**
	 * @param to {@link #getList}
	 * @version throwaway
	 * @param from 1st param of {@link A#getList(int, long) me}
	 * @see #getList(int, long)
	 * @param from 2nd
	 * @see #getList(int from, long tho)
	 * @param to
	 * @throws java.io.IOException
	 * @return the list
	 * @throws IOException
	 */
	public ArrayList getList(int from, long to) throws IOException {
		//change to: java.util.List getList(long tho, List l, int to) throws Exception
		// (swap parameters, insert new in between, change return type, change Ex.)
		return new ArrayList((int)to-from);
	}
}

interface I {
	public ArrayList getList(int from, long to) throws java.io.IOException;
}

interface J {
	/** Doc: @param t t */
	public ArrayList getList(int f, long t) throws FileNotFoundException;
}

class B extends A implements I, J {
	/**
	 * @return {@inheritDoc} 
	 * @see p.A#getList(int, long)
	 */
	public ArrayList getList(int from, long to) throws FileNotFoundException {
		return new ArrayList() {};
	}
}
