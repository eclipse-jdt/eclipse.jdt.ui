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
import static java.lang.Math.cos;

public class Inner
{
	/** Comment */
	private A a

	Inner(A a) {
		this.a= a;
		int f= this.a.foo();
		int g= this.a.bar();
		double d= cos(0);
	}
}
