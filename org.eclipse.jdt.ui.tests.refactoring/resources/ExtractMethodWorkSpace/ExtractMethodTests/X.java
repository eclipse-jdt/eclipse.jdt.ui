/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public class X {
	class Inner extends Exception {
	}
}
class DD extends X.Inner {
	DD() {
		new X().super();
	}
	public final static boolean DEBUG= true;
	public void foo0() {
		try {
			d();
		} catch (X.Inner e) {
		}
	}

	protected void d() throws X.Inner {
		if (DEBUG)
			throw new X().new Inner();//<<SELECT AND EXTRACT
	}

}