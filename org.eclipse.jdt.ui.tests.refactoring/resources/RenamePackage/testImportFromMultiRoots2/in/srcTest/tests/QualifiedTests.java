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
package tests;

public class QualifiedTests {
	static {
		p.p.ATest aQualifiedTest;
		p.p.A aQualified;
	}
	static {
		p.
		//comment
		p/*internal*/.ATest aQualifiedTest;
		p.
		p //unreadable
		/*stuff*/.A aQualified;
	}
}
