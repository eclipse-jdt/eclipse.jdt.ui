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
// 14, 16 -> 14, 32  replaceAll == true, removeDeclaration == false
package schweiz.zuerich.zuerich;

public abstract class Froehlichkeit {
	static class MeineFroehlichkeit extends Froehlichkeit {
		MeineFroehlichkeit(Object o) {}
	}
	private static Object something= new Object();
	private static final Froehlichkeit dieFroehlichkeit= new MeineFroehlichkeit(something);

	public Froehlichkeit holenFroehlichkeit() {
		class MeineFroehlichkeit {
		}
		return new Froehlichkeit.MeineFroehlichkeit(something);
	}

	public Froehlichkeit deineFroehlichkeit() {
		Object something= "";
		return new MeineFroehlichkeit(Froehlichkeit.something);
	}
}