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
package import_out;

import java.io.File;
import java.util.Map;

public class TestUseInLocalClass {
	public void main() {
		Provider p= null;
		class Local extends File {
			private static final long serialVersionUID = 1L;
			public Local(String s) {
				super(s);
			}
			public void foo(Map map) {
			}
			public void bar(Byte b) {
			}
		}
	}
}
