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
