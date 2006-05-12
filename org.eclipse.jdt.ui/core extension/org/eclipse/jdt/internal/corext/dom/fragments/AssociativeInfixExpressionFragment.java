/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom.fragments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.JdtASTMatcher;

class AssociativeInfixExpressionFragment extends ASTFragment implements IExpressionFragment {
	
	private List fOperands;
	private InfixExpression fGroupRoot;
	
	public static IExpressionFragment createSubPartFragmentBySourceRange(InfixExpression node, SourceRange range, ICompilationUnit cu) throws JavaModelException {
		Assert.isNotNull(node);
		Assert.isNotNull(range);
		Assert.isTrue(!range.covers(node));
		Assert.isTrue(new SourceRange(node).covers(range));

		if(!isAssociativeInfix(node))
			return null;
		
		InfixExpression groupRoot= findGroupRoot(node);
		Assert.isTrue(isAGroupRoot(groupRoot));
		
		List groupMembers= GroupMemberFinder.findGroupMembersInOrderFor(groupRoot);
		List subGroup= findSubGroupForSourceRange(groupMembers, range);
		if(subGroup.isEmpty() || rangeIncludesExtraNonWhitespace(range, subGroup, cu))
			return null;
		
		return new AssociativeInfixExpressionFragment(groupRoot, subGroup);
	}

	public static IExpressionFragment createFragmentForFullSubtree(InfixExpression node) {
		Assert.isNotNull(node);
		
		if(!isAssociativeInfix(node))
			return null;
		
		InfixExpression groupRoot= findGroupRoot(node);
		Assert.isTrue(isAGroupRoot(groupRoot));
		
		List groupMembers= GroupMemberFinder.findGroupMembersInOrderFor(node);
		
		return new AssociativeInfixExpressionFragment(groupRoot, groupMembers);
	}
	
	private static InfixExpression findGroupRoot(InfixExpression node) {
		Assert.isTrue(isAssociativeInfix(node));
		
		while(!isAGroupRoot(node)) {
			ASTNode parent= node.getParent();
			
			Assert.isNotNull(parent);
			Assert.isTrue(isAssociativeInfix(parent));
			Assert.isTrue(((InfixExpression) parent).getOperator() == node.getOperator());
		
			node= (InfixExpression) parent;
		}
		
		return node;
	}

	private static List findSubGroupForSourceRange(List group, SourceRange range) {
		Assert.isTrue(!group.isEmpty());
				
		List subGroup= new ArrayList();
		
		boolean entered= false, exited= false;
		if(range.getOffset() == ((ASTNode)group.get(0)).getStartPosition())
			entered= true;
		for(int i= 0; i < group.size() - 1; i++) {
			ASTNode member= (ASTNode) group.get(i);
			ASTNode nextMember= (ASTNode) group.get(i + 1);
			
			if(entered) {
				subGroup.add(member);
				if(rangeEndsBetween(range, member, nextMember)) {
					exited= true;
					break;
				}
				
			} else {
				if(rangeStartsBetween(range, member, nextMember))
					entered= true;
			}
		}
		ASTNode lastGroupMember= (ASTNode)group.get(group.size() - 1);
		if(range.getEndExclusive() == new SourceRange(lastGroupMember).getEndExclusive()) {
			subGroup.add(lastGroupMember);
			exited= true;	
		}
			
		if(!exited)
			return new ArrayList(0);
		return subGroup;
	}
	private static boolean rangeStartsBetween(SourceRange range, ASTNode first, ASTNode next) {
		int pos= range.getOffset();
		return    first.getStartPosition() + first.getLength() <= pos
		        && pos <= next.getStartPosition();
	}
	private static boolean rangeEndsBetween(SourceRange range, ASTNode first, ASTNode next) {
		int pos= range.getEndExclusive();
		return    first.getStartPosition() + first.getLength() <= pos
		        && pos <= next.getStartPosition();
	}
	private static boolean rangeIncludesExtraNonWhitespace(SourceRange range, List operands, ICompilationUnit cu) throws JavaModelException {
		return Util.rangeIncludesNonWhitespaceOutsideRange(range, getRangeOfOperands(operands), cu.getBuffer());
	}
	private static SourceRange getRangeOfOperands(List operands) {
		Expression first= (Expression) operands.get(0);
		Expression last= (Expression) operands.get(operands.size() - 1);
		return new SourceRange(first.getStartPosition(), last.getStartPosition() + last.getLength() - first.getStartPosition());	
	}
	
	public IASTFragment[] getMatchingFragmentsWithNode(ASTNode node) {
		IASTFragment fragmentForNode= ASTFragmentFactory.createFragmentForFullSubtree(node);
		
		if(fragmentForNode instanceof AssociativeInfixExpressionFragment) {
			AssociativeInfixExpressionFragment kin= (AssociativeInfixExpressionFragment)fragmentForNode;
			return kin.getSubFragmentsWithMyNodeMatching(this);
		} else
			return new IASTFragment[0];
	}
	
	/**
	 * Returns List of Lists of <code>ASTNode</code>s
	 */
	private static List getMatchingContiguousNodeSubsequences(List source, List toMatch) {
		//naive implementation:
		
		List subsequences= new ArrayList();
		
		for(int i= 0; i < source.size();) {
			if(matchesAt(i, source, toMatch)) {
				subsequences.add(source.subList(i, i + toMatch.size()));
				i += toMatch.size();
			} else
				i++;
		}
		
		return subsequences;
	}
	private static boolean matchesAt(int index, List subject, List toMatch) {
		if(index + toMatch.size() > subject.size())
			return false;  
		for(int i= 0; i < toMatch.size(); i++, index++) {
			if(!JdtASTMatcher.doNodesMatch(
			        (ASTNode) subject.get(index), (ASTNode) toMatch.get(i)
			    )
			)
				return false;
		}
		return true;
	}
	
	private static boolean isAGroupRoot(ASTNode node) {
		Assert.isNotNull(node);
		
		return    isAssociativeInfix(node)
		        && !isParentInfixWithSameOperator((InfixExpression) node);
	}
	private static boolean isAssociativeInfix(ASTNode node) {
		return node instanceof InfixExpression && isOperatorAssociative(((InfixExpression)node).getOperator());
	}
	private static boolean isParentInfixWithSameOperator(InfixExpression node) {
			return    node.getParent() instanceof InfixExpression 
			        && ((InfixExpression) node.getParent()).getOperator() == node.getOperator();
	}
	private static boolean isOperatorAssociative(InfixExpression.Operator operator) {
		return    operator == InfixExpression.Operator.PLUS
		        || operator == InfixExpression.Operator.TIMES
		        || operator == InfixExpression.Operator.XOR
		        || operator == InfixExpression.Operator.OR
		        || operator == InfixExpression.Operator.AND
		        || operator == InfixExpression.Operator.CONDITIONAL_OR
		        || operator == InfixExpression.Operator.CONDITIONAL_AND;
	}
	
	private AssociativeInfixExpressionFragment(InfixExpression groupRoot, List operands) {
		Assert.isTrue(isAGroupRoot(groupRoot));
		Assert.isTrue(!operands.isEmpty());
		fGroupRoot= groupRoot;
		fOperands= operands;	
	}
	
	public boolean matches(IASTFragment other) {
		Assert.isNotNull(other);
		if(!other.getClass().equals(getClass()))
			return false;
		
		AssociativeInfixExpressionFragment otherOfKind= (AssociativeInfixExpressionFragment) other;
		return    getOperator() == otherOfKind.getOperator()
		        && doOperandsMatch(otherOfKind);
	}
	private boolean doOperandsMatch(AssociativeInfixExpressionFragment other) {
		Assert.isNotNull(other);
		
		if (getOperands().size() != other.getOperands().size())
			return false;
		Iterator myOperands= getOperands().iterator();
		Iterator othersOperands= other.getOperands().iterator();
		
		while(myOperands.hasNext() && othersOperands.hasNext()) {	
			ASTNode myOperand= (ASTNode) myOperands.next();
			ASTNode othersOperand= (ASTNode) othersOperands.next();
			
			if(!JdtASTMatcher.doNodesMatch(myOperand, othersOperand))
				return false;
		}
		
		return true;
	}

	public IASTFragment[] getSubFragmentsMatching(IASTFragment toMatch) {
		
		return union(
		               getSubFragmentsWithMyNodeMatching(toMatch),
		               getSubFragmentsWithAnotherNodeMatching(toMatch)
		             );
	}
	private IASTFragment[] getSubFragmentsWithMyNodeMatching(IASTFragment toMatch) {
		if(toMatch.getClass() != getClass())
			return new IASTFragment[0];
			
		AssociativeInfixExpressionFragment kinToMatch= (AssociativeInfixExpressionFragment) toMatch;
		if(kinToMatch.getOperator() != getOperator())
			return new IASTFragment[0];
		
		List matchingSubsequences=
			getMatchingContiguousNodeSubsequences(
				getOperands(),
				kinToMatch.getOperands()
			);
	
		IASTFragment[] matches= new IASTFragment[matchingSubsequences.size()];
		for(int i= 0; i < matchingSubsequences.size(); i++) {
			IASTFragment match= new AssociativeInfixExpressionFragment(
		                           getGroupRoot(), 
		                           (List) matchingSubsequences.get(i)
		                );
			Assert.isTrue(match.matches(toMatch) || toMatch.matches(match));		    
		    matches[i]= match;
		}
		return matches;
	}
	private IASTFragment[] getSubFragmentsWithAnotherNodeMatching(IASTFragment toMatch) {
		IASTFragment[] result= new IASTFragment[0];
		for (Iterator iter= getOperands().iterator(); iter.hasNext();) {
			ASTNode operand= (ASTNode) iter.next();
			result= union(result, ASTMatchingFragmentFinder.findMatchingFragments(operand, (ASTFragment)toMatch));
		}
		return result;
	}
	private static IASTFragment[] union(IASTFragment[] a1, IASTFragment[] a2) {
		IASTFragment[] union= new IASTFragment[a1.length + a2.length];
		System.arraycopy(a1, 0, union, 0, a1.length);
		System.arraycopy(a2, 0, union, a1.length, a2.length);
		return union;
	}


	/**
	 * Note that this fragment does not directly
	 * represent this expression node, but rather
	 * a part of it.
	 */
	public Expression getAssociatedExpression() {
		return getGroupRoot();
	}

	/**
	 * Note that this fragment does not directly
	 * represent this node, but rather a particular sort of
	 * part of its subtree.
	 */
	public ASTNode getAssociatedNode() {
		return getGroupRoot();
	}
	
	public InfixExpression getGroupRoot() {
		Assert.isNotNull(fGroupRoot);
		return fGroupRoot;	
	} 

	public int getLength() {
		return getEndPositionExclusive() - getStartPosition();
	}
	
	private int getEndPositionExclusive() {
		List operands= getOperands();
		ASTNode lastNode= (ASTNode)operands.get(operands.size() - 1);
		return lastNode.getStartPosition() + lastNode.getLength();
	}

	public int getStartPosition() {
		return ((ASTNode)getOperands().get(0)).getStartPosition();
	}
	
	public List getOperands() {
		return new ArrayList(fOperands);	
	}
	
	public InfixExpression.Operator getOperator() {
		return getGroupRoot().getOperator();
	}

	private static class GroupMemberFinder extends GenericVisitor {
		private List fMembersInOrder= new ArrayList();
		private InfixExpression fGroupRoot;
		
		public static List findGroupMembersInOrderFor(InfixExpression groupRoot) {
			return new GroupMemberFinder(groupRoot).getMembersInOrder();	
		}
		
		private GroupMemberFinder(InfixExpression groupRoot) {
			super(true);
			Assert.isTrue(isAssociativeInfix(groupRoot));
			fGroupRoot= groupRoot;
			fGroupRoot.accept(this);
		}
		private List getMembersInOrder() {
			return fMembersInOrder;	
		}
		protected boolean visitNode(ASTNode node) {
			if(node instanceof InfixExpression && ((InfixExpression)node).getOperator() == fGroupRoot.getOperator())
				return true;
			
			fMembersInOrder.add(node);
			return false;
		}
	}
}
