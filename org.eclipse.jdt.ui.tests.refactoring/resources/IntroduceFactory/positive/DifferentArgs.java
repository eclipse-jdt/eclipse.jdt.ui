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
package p;

import java.lang.StringBuffer;

public class DifferentArgs_in {
	public void foo(String[] args)
	{
		StringBuffer	buf = createStringBuffer(16);

		buf.append("Args:");
		for(int i=0; i < args.length; i++)
			buf.append(" '")
			   .append(args[i])
			   .append("'");
		System.out.println(buf.toString());
	}
	public void bar(String[] args)
	{
		StringBuffer	buf = createStringBuffer(24);

		buf.append("Args:");
		for(int i=0; i < args.length; i++)
			buf.append(" '")
			   .append(args[i])
			   .append("'");
		System.out.println(buf.toString());
	}

	public static StringBuffer createStringBuffer(int arg0) {
		return new StringBuffer(arg0);
	}
}
