/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

public class CodeGenerationSettings {

	public boolean createComments= true;
	public boolean useKeywordThis= false;

	public boolean importIgnoreLowercase= true;
	public boolean overrideAnnotation= false;

	public int tabWidth;
	public int indentWidth;


	public void setSettings(CodeGenerationSettings settings) {
		settings.createComments= createComments;
		settings.useKeywordThis= useKeywordThis;
		settings.importIgnoreLowercase= importIgnoreLowercase;
		settings.overrideAnnotation= overrideAnnotation;
		settings.tabWidth= tabWidth;
		settings.indentWidth= indentWidth;
	}

}

