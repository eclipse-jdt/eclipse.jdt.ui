/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;

public class ImportAnalyzer {

	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return false;
	}
}

