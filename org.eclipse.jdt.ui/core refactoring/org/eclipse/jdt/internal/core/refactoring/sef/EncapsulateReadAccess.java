/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.sef;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import sun.awt.OrientableFlowLayout;

public class EncapsulateReadAccess extends SimpleReplaceTextChange {

	private static final String READ_ACCESS= "Encapsulate read access";
	
	public EncapsulateReadAccess(String getter, SingleNameReference node) {
		this(getter, node.sourceStart, node.sourceEnd - node.sourceStart + 1);
	}
	
	protected EncapsulateReadAccess(String getter, int offset, int length) {
		super(READ_ACCESS, offset, length, getter + "()");
	}
}

