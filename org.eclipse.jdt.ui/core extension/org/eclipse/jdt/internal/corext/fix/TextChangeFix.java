package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.ltk.core.refactoring.TextChange;

public class TextChangeFix extends AbstractFix {

	private final TextChange fChange;

	public TextChangeFix(String name, ICompilationUnit compilationUnit, TextChange change) {
		super(name, compilationUnit);
		fChange= change;
	}

	public TextChange createChange() throws CoreException {
		return fChange;
	}

}
