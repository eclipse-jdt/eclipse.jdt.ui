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
package try_in;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public abstract class A_test460 {
	public void foo() throws InvocationTargetException {
		/*[*/InputStreamReader in= null;
		try {
			bar();
		} catch (IOException e) {
			throw new InvocationTargetException(null);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}/*]*/
	}

	public abstract void bar() throws IOException;
}

