/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import java.util.Stack;

import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

class JavaParseTreeBuilder extends SourceElementRequestorAdapter implements ICompilationUnit {
	
	private static final boolean SHOW_COMPILATIONUNIT= true;

	private char[] fBuffer;
	private JavaNode fImportContainer;
	private Stack fStack= new Stack();
	
	/**
	 * Parsing is performed on the given buffer and the resulting tree
	 * (if any) hangs below the given root.
	 */
	JavaParseTreeBuilder(JavaNode root, char[] buffer) {
		fImportContainer= null;
		fStack.clear();
		fStack.push(root);
		fBuffer= buffer;
	}
			
	//---- ICompilationUnit
	/* (non Java doc)
	 * @see ICompilationUnit#getContents
	 */
	public char[] getContents() {
		return fBuffer;
	}
	
	/* (non Java doc)
	 * @see ICompilationUnit#getFileName
	 */
	public char[] getFileName() {
		return new char[0];
	}
	
	/* (non Java doc)
	 * @see ICompilationUnit#getMainTypeName
	 */
	public char[] getMainTypeName() {
		return new char[0];
	}
	
	/* (non Java doc)
	 * @see ICompilationUnit#getMainTypeName
	 */
	public char[][] getPackageName() {
		return null;
	}
	
	//---- ISourceElementRequestor
	
	public void enterCompilationUnit() {
		if (SHOW_COMPILATIONUNIT)
			push(JavaNode.CU, null, 0);
	}
	
	public void exitCompilationUnit(int declarationEnd) {
		if (SHOW_COMPILATIONUNIT)
			pop(declarationEnd);
	}
	
	public void acceptPackage(int declarationStart, int declarationEnd, char[] p3) {
		push(JavaNode.PACKAGE, null, declarationStart);
		pop(declarationEnd);
	}
	
	//TODO: (andre) remove on 2003/04/23
	public void acceptImport(int declarationStart, int declarationEnd, char[] name, boolean onDemand) {
		acceptImport(declarationStart, declarationEnd, name, onDemand, 0);
	}
	
	public void acceptImport(int declarationStart, int declarationEnd, char[] name, boolean onDemand, int modifiers) {
		int length= declarationEnd-declarationStart+1;
		if (fImportContainer == null)
			fImportContainer= new JavaNode(getCurrentContainer(), JavaNode.IMPORT_CONTAINER, null, declarationStart, length);
		String nm= new String(name);
		if (onDemand)
			nm+= ".*"; //$NON-NLS-1$
		new JavaNode(fImportContainer, JavaNode.IMPORT, nm, declarationStart, length);
		fImportContainer.setLength(declarationEnd-fImportContainer.getRange().getOffset()+1);
		fImportContainer.setAppendPosition(declarationEnd+2);		// FIXME
	}
	
	public void enterClass(int declarationStart, int p2, char[] name, int p4, int p5, char[] p6, char[][] p7, char[][] typeParameterNames, char[][][] typeParameterBounds) {
		push(JavaNode.CLASS, new String(name), declarationStart);
	}
	
	public void exitClass(int declarationEnd) {
		pop(declarationEnd);
	}

	public void enterInterface(int declarationStart, int p2, char[] name, int p4, int p5, char[][] p6, char[][] typeParameterNames, char[][][] typeParameterBounds) {
		push(JavaNode.INTERFACE, new String(name), declarationStart);
	}
	
	public void exitInterface(int declarationEnd) {
		pop(declarationEnd);
	}
	
	public void enterInitializer(int declarationStart, int modifiers) {
		push(JavaNode.INIT, getCurrentContainer().getInitializerCount(), declarationStart);
	}

	public void exitInitializer(int declarationEnd) {
		pop(declarationEnd);
	}
	
	public void enterConstructor(int declarationStart, int p2, char[] name, int p4, int p5, char[][] parameterTypes, char[][] p7, char[][] p8, char[][] typeParameterNames, char[][][] typeParameterBounds) {
		push(JavaNode.CONSTRUCTOR, getSignature(name, parameterTypes), declarationStart);
	}
	
	public void exitConstructor(int declarationEnd) {
		pop(declarationEnd);
	}
	
	public void enterMethod(int declarationStart, int p2, char[] p3, char[] name, int p5, int p6, char[][] parameterTypes, char[][] p8, char[][] p9, char[][] typeParameterNames, char[][][] typeParameterBounds){
		push(JavaNode.METHOD, getSignature(name, parameterTypes), declarationStart);
	}
	
	public void exitMethod(int declarationEnd) {
		pop(declarationEnd);
	}
	
	public void enterField(int declarationStart, int p2, char[] p3, char[] name, int p5, int p6) {
		push(JavaNode.FIELD, new String(name), declarationStart);
	}
	
	public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
		pop(declarationSourceEnd);
	}
	
	private JavaNode getCurrentContainer() {
		return (JavaNode) fStack.peek();
	}
	
	/**
	 * Adds a new JavaNode with the given type and name to the current container.
	 */
	private void push(int type, String name, int declarationStart) {
						
		while (declarationStart > 0) {
			char c= fBuffer[declarationStart-1];
			if (c != ' ' && c != '\t')
				break;
			declarationStart--;
		}
					
		fStack.push(new JavaNode(getCurrentContainer(), type, name, declarationStart, 0));
	}
	
	/**
	 * Closes the current Java node by setting its end position
	 * and pops it off the stack.
	 */
	private void pop(int declarationEnd) {
		
		JavaNode current= getCurrentContainer();
		if (current.getTypeCode() == JavaNode.CU)
			current.setAppendPosition(declarationEnd+1);
		else
			current.setAppendPosition(declarationEnd);
			
		current.setLength(declarationEnd - current.getRange().getOffset() + 1);

		fStack.pop();
	}
	
	/**
	 * Builds a signature string from the given name and parameter types.
	 */
	private String getSignature(char[] name, char[][] parameterTypes) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(name);
		buffer.append('(');
		if (parameterTypes != null) {
			for (int p= 0; p < parameterTypes.length; p++) {
				String parameterType= new String(parameterTypes[p]);
				
				int pos= parameterType.lastIndexOf('.');
				if (pos >= 0)
					parameterType= parameterType.substring(pos+1);
				
				buffer.append(parameterType);
				if (p < parameterTypes.length-1)
					buffer.append(", "); //$NON-NLS-1$
			}
		}
		buffer.append(')');
		return buffer.toString();
	}
}

