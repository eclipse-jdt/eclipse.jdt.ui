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

import java.util.Iterator;
import java.util.Stack;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

class JavaParseTreeBuilder extends ASTVisitor {

	private static final boolean SHOW_COMPILATIONUNIT= true;

	//private char[] fBuffer;
    private ASTParser fParser;
	private Stack fStack= new Stack();
	private JavaNode fImportContainer;
   
	JavaParseTreeBuilder() {
	}

	/*
	 * Parsing is performed on the given buffer and the resulting tree
	 * (if any) hangs below the given root.
	 */
    public void parse(JavaNode root, char[] buffer) {
        
        //fBuffer= buffer;
        
		fStack.clear();
		fStack.push(root);

		if (fParser == null)
	        fParser= ASTParser.newParser(AST.JLS3);
	    fParser.setSource(buffer);
	    fParser.setFocalPosition(0);
	    CompilationUnit cu= (CompilationUnit) fParser.createAST(null);
	    cu.accept(this);
    }

    public boolean visit(PackageDeclaration node) {
        	new JavaNode(getCurrentContainer(), JavaNode.PACKAGE, null, node.getStartPosition(), node.getLength());
		return false;
	}

	public boolean visit(CompilationUnit node) {
	    if (SHOW_COMPILATIONUNIT)
	        push(JavaNode.CU, null, 0, node.getLength());
	    return true;
	}
	public void endVisit(CompilationUnit node) {
	    if (SHOW_COMPILATIONUNIT)
	        pop();
	}

	public boolean visit(TypeDeclaration node) {
		push(node.isInterface() ? JavaNode.INTERFACE : JavaNode.CLASS, node.getName().toString(), node.getStartPosition(), node.getLength());
		return true;
	}
	public void endVisit(TypeDeclaration node) {
		pop();
	}
	
	public boolean visit(EnumDeclaration node) {
		push(JavaNode.ENUM, node.getName().toString(), node.getStartPosition(), node.getLength());
		return true;
	}
	public void endVisit(EnumDeclaration node) {
		pop();
	}

	public boolean visit(MethodDeclaration node) {
	    String signature= getSignature(node);
		push(node.isConstructor() ? JavaNode.CONSTRUCTOR : JavaNode.METHOD, signature, node.getStartPosition(), node.getLength());
		return false;
	}
	public void endVisit(MethodDeclaration node) {
		pop();
	}
	
	public boolean visit(Initializer node) {
		push(JavaNode.INIT, getCurrentContainer().getInitializerCount(), node.getStartPosition(), node.getLength());
		return false;
	}
	public void endVisit(Initializer node) {
		pop();
	}

	
	public boolean visit(ImportDeclaration node) {
	    int s= node.getStartPosition();
	    int l= node.getLength();
	    int declarationEnd= s+l;
		if (fImportContainer == null)
			fImportContainer= new JavaNode(getCurrentContainer(), JavaNode.IMPORT_CONTAINER, null, s, l);
		String nm= node.getName().toString();
		if (node.isOnDemand())
		    nm+= ".*"; //$NON-NLS-1$
		new JavaNode(fImportContainer, JavaNode.IMPORT, nm, s, l);
		fImportContainer.setLength(declarationEnd-fImportContainer.getRange().getOffset()+1);
		fImportContainer.setAppendPosition(declarationEnd+2);		// FIXME
		return false;
	}
	
	public boolean visit(VariableDeclarationFragment node) {
	    ASTNode parent= node.getParent();
		push(JavaNode.FIELD, node.getName().toString(), parent.getStartPosition(), parent.getLength());
		return false;
	}
	public void endVisit(VariableDeclarationFragment node) {
		pop();
	}
 
	public boolean visit(EnumConstantDeclaration node) {
		push(JavaNode.FIELD, node.getName().toString(), node.getStartPosition(), node.getLength());
		return false;
	}
	public void endVisit(EnumConstantDeclaration node) {
		pop();
	}

	/**
	 * Adds a new JavaNode with the given type and name to the current container.
	 */
	private void push(int type, String name, int declarationStart, int length) {
	    
	    /*
		while (declarationStart > 0) {
			char c= fBuffer[declarationStart-1];
			if (c != ' ' && c != '\t')
				break;
			declarationStart--;
		}
		*/
	    
		JavaNode node= new JavaNode(getCurrentContainer(), type, name, declarationStart, length);
		if (type == JavaNode.CU)
		    node.setAppendPosition(declarationStart+length+1);
		else
		    node.setAppendPosition(declarationStart+length);
		
		fStack.push(node);
	}
	
	/**
	 * Closes the current Java node by setting its end position
	 * and pops it off the stack.
	 */
	private void pop() {		
		fStack.pop();
	}

	private JavaNode getCurrentContainer() {
		return (JavaNode) fStack.peek();
	}
	
	private String getSignature(MethodDeclaration node) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(node.getName().toString());
		buffer.append('(');
		boolean first= true;
	    Iterator iterator= node.parameters().iterator();
		while (iterator.hasNext()) {
		    Object object= iterator.next();
		    
		    if (object instanceof SingleVariableDeclaration) {
		        SingleVariableDeclaration svd= (SingleVariableDeclaration) object;
				String parameterType= svd.getType().toString();
				
				int pos= parameterType.lastIndexOf('.');
				if (pos >= 0)
					parameterType= parameterType.substring(pos+1);
				
				if (! first)
					buffer.append(", "); //$NON-NLS-1$
				first= false;
				buffer.append(parameterType);
		    }
		}
		buffer.append(')');
		return buffer.toString();
	}
}
