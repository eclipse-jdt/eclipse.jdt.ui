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
package locals_in;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class A_test535 {

	public void bar() {
		List allElements= new ArrayList();
		Iterator iter= allElements.iterator();		

		/*[*/while (iter.hasNext()) {
			allElements.add(iter.next());
		}/*]*/
	}
}
