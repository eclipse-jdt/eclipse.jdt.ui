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
// 7, 35 -> 7, 35  replaceAll == true, removeDeclaration == false;
package cantonzuerich;

public class GrueziWohl {
	private static String gruezi= "Gruezi";
	private static boolean jh= true;
	private static final boolean WOHL= jh && "Gruezi".equals(gruezi);
	
	public String holenGruss() {
		String gruezi= "Gruezi";
		return gruezi + (WOHL ? " Wohl" : "") + "!";
	}
	
	private boolean wohl() {
		return WOHL;
	}
}