/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Extract to local variable may result in NullPointerException. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;

public class NullChecker {
	private ASTNode expression;
	private int startOffset;
	private int endOffset;

	private ASTNode commonNode;
	private Set<String> invocationSet;
	public NullChecker(ASTNode commonNode,ASTNode expression,int startOffset,int endOffset) {
		this.commonNode=commonNode;
		this.expression=expression;
		this.startOffset=startOffset;
		this.endOffset=endOffset;
		this.invocationSet=new HashSet<>();
		InvocationVisitor iv=new InvocationVisitor( );
		this.expression.accept(iv);
		this.invocationSet=iv.invocationSet;
	}

	public boolean isExistNull( ) {
		NullMiddleCodeVisitor nullMiddleCodeVisitor=new NullMiddleCodeVisitor(this.invocationSet,this.startOffset,this.endOffset);
        this.commonNode.accept(nullMiddleCodeVisitor);
		return nullMiddleCodeVisitor.isNull();
	}

	private class InvocationVisitor extends ASTVisitor{
		Set<String> invocationSet;
		InvocationVisitor(){
			this.invocationSet=new HashSet<>();
		}
		@Override
		public void preVisit(ASTNode node) {
			if(node instanceof MethodInvocation) {
				MethodInvocation mi=( MethodInvocation)node;
				Expression temp = mi.getExpression();
				if(temp!=null)
				this.invocationSet.add(temp.toString());
			}else if(node instanceof FieldAccess) {
				FieldAccess fa=( FieldAccess)node;
				Expression temp = fa.getExpression();
				if(temp!=null)
				this.invocationSet.add(temp.toString());
			}else if(node instanceof QualifiedName) {
				QualifiedName qn=( QualifiedName)node;
				Name temp = qn.getQualifier();
				if(temp!=null)
				this.invocationSet.add(temp.toString());
			}else if(node instanceof ArrayAccess) {
				ArrayAccess aa=( ArrayAccess)node;
				Expression temp = aa.getArray();
				if(temp!=null)
				this.invocationSet.add(temp.toString());
			}
		}
	}

	private class NullMiddleCodeVisitor extends ASTVisitor{
		int startPosition;
	    int endPosition;
	    boolean nullFlag;
	    Set<String> set;
	    public NullMiddleCodeVisitor(Set<String> invocationSet,int startPosition, int endPosition) {
	        this.set=invocationSet;
	    	this.startPosition=startPosition;
	        this.endPosition=endPosition;
	        this.nullFlag=false;
	    }

	    public boolean isNull() {
	    	return this.nullFlag;
	    }

	    @Override
	    public boolean preVisit2(ASTNode node) {
	        int sl= node.getStartPosition() ;
	        int el= node.getStartPosition()+node.getLength() ;
	        if(el<startPosition||sl>endPosition||this.nullFlag==true) {
	        	return false;
	        }
	        if(sl>=startPosition && el <=endPosition && node instanceof InfixExpression){
	        	InfixExpression infixExpression=(InfixExpression)node;
	            Operator op=infixExpression.getOperator();
		    	if(Operator.toOperator(op.toString())==Operator.EQUALS||Operator.toOperator(op.toString())==Operator.NOT_EQUALS) {
		    		Expression leftExpression = infixExpression.getLeftOperand();
		    		Expression rightExpression = infixExpression.getRightOperand();
		    		Expression target=null;
		    		if( rightExpression.getNodeType()==ASTNode.NULL_LITERAL) {
		    			target=leftExpression;
		    		}else if(leftExpression.getNodeType()==ASTNode.NULL_LITERAL) {
		    			target=rightExpression;
		    		}
		    		if(target!=null&&this.set.contains(target.toString())) {
		    			this.nullFlag=true;
		    			return false;
		    		}
		    	}
	        }
	        return super.preVisit2(node);
	    }
	}

}