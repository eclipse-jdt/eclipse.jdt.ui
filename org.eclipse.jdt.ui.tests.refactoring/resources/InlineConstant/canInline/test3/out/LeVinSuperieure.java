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
// 5, 32 -> 5, 43  replaceAll == true, removeDeclaration == true
package p;

class LeVinSuperieure {
	public LeVinSuperieure(final String appelation) {
		String leNom= appelation == null ? "Pharmacology" : appelation;
		System.out.println("Nous avons cree un superieure vin, appelle " + leNom);
	}
}