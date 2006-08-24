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
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class ControlStatementsFix extends AbstractFix {
	
	private static final String FOR_LOOP_ELEMENT_IDENTIFIER= "element"; //$NON-NLS-1$
	
	private final static class ControlStatementFinder extends GenericVisitor {
		
		private final List/*<IFixRewriteOperation>*/ fResult;
		private final Hashtable fUsedNames;
		private final CompilationUnit fCompilationUnit;
		private final boolean fFindControlStatementsWithoutBlock;
		private final boolean fFindForLoopsToConvert;
		private final boolean fRemoveUnnecessaryBlocks;
		private final boolean fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow;
		
		public ControlStatementFinder(CompilationUnit compilationUnit, 
				boolean findControlStatementsWithoutBlock,
				boolean removeUnnecessaryBlocks,
				boolean removeUnnecessaryBlocksOnlyWhenReturnOrThrow,
				boolean findForLoopsToConvert,
				List resultingCollection) throws CoreException {
			
			fFindControlStatementsWithoutBlock= findControlStatementsWithoutBlock;
			fRemoveUnnecessaryBlocks= removeUnnecessaryBlocks;
			fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow= removeUnnecessaryBlocksOnlyWhenReturnOrThrow;
			fFindForLoopsToConvert= findForLoopsToConvert;
			fResult= resultingCollection;
			fUsedNames= new Hashtable();
			fCompilationUnit= compilationUnit;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.DoStatement)
		 */
		public boolean visit(DoStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode doBody= node.getBody();
				if (!(doBody instanceof Block)) {
					fResult.add(new AddBlockOperation(DoStatement.BODY_PROPERTY, doBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				RemoveBlockOperation op= createRemoveBlockOperation(node, DoStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
				if (op != null)
					fResult.add(op);
			}
			return super.visit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ForStatement)
		 */
		public boolean visit(ForStatement node) {
			if (fFindForLoopsToConvert) {
				List usedVaribles= getUsedVariableNames(node);
				usedVaribles.addAll(fUsedNames.values());
				String[] used= (String[])usedVaribles.toArray(new String[usedVaribles.size()]);

				String identifierName= FOR_LOOP_ELEMENT_IDENTIFIER;
				int count= 0;
				for (int i= 0; i < used.length; i++) {
					if (used[i].equals(identifierName)) {
						identifierName= FOR_LOOP_ELEMENT_IDENTIFIER + count;
						count++;
						i= 0;
					}
				}
				
				ConvertForLoopOperation forConverter= new ConvertForLoopOperation(fCompilationUnit, node, identifierName, fFindControlStatementsWithoutBlock, fRemoveUnnecessaryBlocks);
				if (forConverter.satisfiesPreconditions()) {
					fResult.add(forConverter);
					fUsedNames.put(node, identifierName);
				} else {
					ConvertIterableLoopOperation iterableConverter= new ConvertIterableLoopOperation(fCompilationUnit, node, identifierName);
					if (expressionSatisfiesPrecondition(node) && iterableConverter.isApplicable()) {
						fResult.add(iterableConverter);
						fUsedNames.put(node, identifierName);
					} else if (fFindControlStatementsWithoutBlock) {
						ASTNode forBody= node.getBody();
						if (!(forBody instanceof Block)) {
							fResult.add(new AddBlockOperation(ForStatement.BODY_PROPERTY, forBody, node));
						}
					} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
						RemoveBlockOperation op= createRemoveBlockOperation(node, ForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
						if (op != null)
							fResult.add(op);
					}
				}
			} else if (fFindControlStatementsWithoutBlock) {
				ASTNode forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fResult.add(new AddBlockOperation(ForStatement.BODY_PROPERTY, forBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				RemoveBlockOperation op= createRemoveBlockOperation(node, ForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
				if (op != null)
					fResult.add(op);
			}
			return super.visit(node);
		}
		
		private boolean expressionSatisfiesPrecondition(ForStatement forStatement) {
	    	Expression expression= forStatement.getExpression();
			if (!(expression instanceof MethodInvocation))
				return false;
			
			MethodInvocation invoc= (MethodInvocation)expression;
			IMethodBinding methodBinding= invoc.resolveMethodBinding();
			if (methodBinding == null)
				return false;
			
			ITypeBinding declaringClass= methodBinding.getDeclaringClass();
			if (declaringClass == null)
				return false;
			
			String qualifiedName= declaringClass.getQualifiedName();
			String methodName= invoc.getName().getIdentifier();
			if (qualifiedName.startsWith("java.util.Enumeration")) { //$NON-NLS-1$
				if (!methodName.equals("hasMoreElements")) //$NON-NLS-1$
					return false;
			} else if (qualifiedName.startsWith("java.util.Iterator")) { //$NON-NLS-1$
				if (!methodName.equals("hasNext")) //$NON-NLS-1$
					return false;
			} else {
				return false;
			}
			
		    return true;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(EnhancedForStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fResult.add(new AddBlockOperation(EnhancedForStatement.BODY_PROPERTY, forBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				RemoveBlockOperation op= createRemoveBlockOperation(node, EnhancedForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
				if (op != null)
					fResult.add(op);
			}
			return super.visit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#endVisit(org.eclipse.jdt.core.dom.ForStatement)
		 */
		public void endVisit(ForStatement node) {
			if (fFindForLoopsToConvert) {
				fUsedNames.remove(node);
			}
			super.endVisit(node);
		}
		
		private List getUsedVariableNames(ASTNode node) {
			CompilationUnit root= (CompilationUnit)node.getRoot();
			IBinding[] varsBefore= (new ScopeAnalyzer(root)).getDeclarationsInScope(node.getStartPosition(),
				ScopeAnalyzer.VARIABLES);
			IBinding[] varsAfter= (new ScopeAnalyzer(root)).getDeclarationsAfter(node.getStartPosition()
				+ node.getLength(), ScopeAnalyzer.VARIABLES);

			List names= new ArrayList();
			for (int i= 0; i < varsBefore.length; i++) {
				names.add(varsBefore[i].getName());
			}
			for (int i= 0; i < varsAfter.length; i++) {
				names.add(varsAfter[i].getName());
			}
			return names;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.IfStatement)
		 */
		public boolean visit(IfStatement statement) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode then= statement.getThenStatement();
				if (!(then instanceof Block)) {
					fResult.add(new AddBlockOperation(IfStatement.THEN_STATEMENT_PROPERTY, then, statement));
				}
				ASTNode elseStatement= statement.getElseStatement();
				if (elseStatement != null && !(elseStatement instanceof Block) && !(elseStatement instanceof IfStatement)) {
					fResult.add(new AddBlockOperation(IfStatement.ELSE_STATEMENT_PROPERTY, elseStatement, statement));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				RemoveBlockOperation op= createRemoveBlockOperation(statement, IfStatement.THEN_STATEMENT_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
				if (op != null)
					fResult.add(op);
				if (!(statement.getElseStatement() instanceof IfStatement)) {
					op= createRemoveBlockOperation(statement, IfStatement.ELSE_STATEMENT_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
					if (op != null)
						fResult.add(op);
				}
			}
			return super.visit(statement);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.WhileStatement)
		 */
		public boolean visit(WhileStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode whileBody= node.getBody();
				if (!(whileBody instanceof Block)) {
					fResult.add(new AddBlockOperation(WhileStatement.BODY_PROPERTY, whileBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				RemoveBlockOperation op= createRemoveBlockOperation(node, WhileStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow);
				if (op != null)
					fResult.add(op);
			}
			return super.visit(node);
		}
		
		private static RemoveBlockOperation createRemoveBlockOperation(Statement statement, ChildPropertyDescriptor child, boolean onlyReturnAndThrows) {
			if (!RemoveBlockOperation.satisfiesCleanUpPrecondition(statement, child, onlyReturnAndThrows))
				return null;
		
			return new RemoveBlockOperation((Block) statement.getStructuralProperty(child), statement, child);
		}
	
	}
	
	private static final class AddBlockOperation extends AbstractFixRewriteOperation {

		private final ChildPropertyDescriptor fBodyProperty;
		private final ASTNode fBody;
		private final Statement fStatement;

		public AddBlockOperation(ChildPropertyDescriptor bodyProperty, ASTNode body, Statement statement) {
			fBodyProperty= bodyProperty;
			fBody= body;
			fStatement= statement;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			String label;
			if (fBodyProperty == IfStatement.THEN_STATEMENT_PROPERTY) {
				label = FixMessages.CodeStyleFix_ChangeIfToBlock_desription;
			} else if (fBodyProperty == IfStatement.ELSE_STATEMENT_PROPERTY) {
				label = FixMessages.CodeStyleFix_ChangeElseToBlock_description;
			} else {
				label = FixMessages.CodeStyleFix_ChangeControlToBlock_description;
			}
			
			TextEditGroup group= createTextEditGroup(label);
			textEditGroups.add(group);
			
			ASTNode childPlaceholder= rewrite.createMoveTarget(fBody);
			Block replacingBody= cuRewrite.getRoot().getAST().newBlock();
			replacingBody.statements().add(childPlaceholder);
			rewrite.set(fStatement, fBodyProperty, replacingBody, group);
		}
	}
	
	private static class RemoveBlockOperation extends AbstractFixRewriteOperation {

		private final Block fBlock;
		private final Statement fStatement;
		private final ChildPropertyDescriptor fChild;

		public RemoveBlockOperation(Block block, Statement statement, ChildPropertyDescriptor child) {
			fBlock= block;
			fStatement= statement;
			fChild= child;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Statement moveTarget= (Statement)rewrite.createMoveTarget((ASTNode)fBlock.statements().get(0));
			
			TextEditGroup group= createTextEditGroup(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription);
			textEditGroups.add(group);
			rewrite.set(fStatement, fChild, moveTarget, group);
		}
		
		
		public static boolean satisfiesCleanUpPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor, boolean onlyReturnAndThrows) {
			return satisfiesPrecondition(controlStatement, childDescriptor, onlyReturnAndThrows, true);
		}
		
		public static boolean satisfiesQuickAssistPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor) {
			return satisfiesPrecondition(controlStatement, childDescriptor, false, false);
		}

		//Can the block around child with childDescriptor of controlStatement be removed?
        private static boolean satisfiesPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor, boolean onlyReturnAndThrows, boolean cleanUpCheck) {
        	Object child= controlStatement.getStructuralProperty(childDescriptor);
        	
        	if (!(child instanceof Block))
        		return false;
        	
        	Block block= (Block)child;
        	List list= block.statements();
        	if (list.size() != 1)
        		return false;
        	
        	ASTNode singleStatement= (ASTNode)list.get(0);
        	
        	if (onlyReturnAndThrows)
        		if (!(singleStatement instanceof ReturnStatement) && !(singleStatement instanceof ThrowStatement))
        			return false;
        	
        	if (controlStatement instanceof IfStatement) {
        		// if (true) {
        		//  while (true) 
        		// 	 if (false)
        		//    ;
        		// } else
        		//   ;
        		
        		if (((IfStatement)controlStatement).getThenStatement() != child)
        			return true;//can always remove blocks in else part
        		
        		IfStatement ifStatement= (IfStatement)controlStatement;
        		if (ifStatement.getElseStatement() == null)
        			return true;//can always remove if no else part
        		
        		return !hasUnblockedIf((Statement)singleStatement, onlyReturnAndThrows, cleanUpCheck);
        	} else {
        		//if (true)
        		// while (true) {
        		//  if (false)
        		//   ;
        		// }
        		//else
        		// ;
        		if (!hasUnblockedIf((Statement)singleStatement, onlyReturnAndThrows, cleanUpCheck))
        			return true;
        		
        		ASTNode currentChild= controlStatement;
        		ASTNode parent= currentChild.getParent();
        		while (true) {
        			Statement body= null;
        			if (parent instanceof IfStatement) {
        				body= ((IfStatement)parent).getThenStatement();
        				if (body == currentChild && ((IfStatement)parent).getElseStatement() != null)//->currentChild is an unblocked then part
        					return false;
        			} else if (parent instanceof WhileStatement) {
        				body= ((WhileStatement)parent).getBody();
        			} else if (parent instanceof DoStatement) {
        				body= ((DoStatement)parent).getBody();
        			} else if (parent instanceof ForStatement) {
        				body= ((ForStatement)parent).getBody();
        			} else if (parent instanceof EnhancedForStatement) {
        				body= ((EnhancedForStatement)parent).getBody();
        			} else {
        				return true;
        			}
        			if (body != currentChild)//->parents child is a block
        				return true;
        			
        			currentChild= parent;
        			parent= currentChild.getParent();
        		}
        	}
        }

		private static boolean hasUnblockedIf(Statement p, boolean onlyReturnAndThrows, boolean cleanUpCheck) {
	        while (true) {
	        	if (p instanceof IfStatement) {
	        		return true;
	        	} else {

	        		ChildPropertyDescriptor childD= null;
	        		if (p instanceof WhileStatement) {
	        			childD= WhileStatement.BODY_PROPERTY;
	        		} else if (p instanceof ForStatement) {
	        			childD= ForStatement.BODY_PROPERTY;
	        		} else if (p instanceof EnhancedForStatement) {
	        			childD= EnhancedForStatement.BODY_PROPERTY;
	        		} else if (p instanceof DoStatement) {
	        			childD= DoStatement.BODY_PROPERTY;
	        		} else {
	        			return false;
	        		}
	        		Statement body= (Statement)p.getStructuralProperty(childD);
	        		if (body instanceof Block) {
	        			if (!cleanUpCheck) {
	        				return false;
	        			} else {
	        				if (!satisfiesPrecondition(p, childD, onlyReturnAndThrows, cleanUpCheck))
	        					return false;
	        				
	        				p= (Statement)((Block)body).statements().get(0);
	        			}
	        		} else {
	        			p= body;
	        		}
	        	}
	        }
        }

	}

	public static IFix createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertForLoopOperation loopConverter= new ConvertForLoopOperation(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER, false, false);
		if (!loopConverter.satisfiesPreconditions())
			return null;
		
		return new ControlStatementsFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
	}
	
	public static IFix createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER);
		if (!loopConverter.isApplicable())
			return null;

		return new ControlStatementsFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
	}
	
	public static IFix[] createRemoveBlockFix(CompilationUnit compilationUnit, ASTNode node) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (statement == null) {
			return null;
		}
		
		if (statement instanceof Block) {
			Block block= (Block)statement;
			if (block.statements().size() != 1)
				return null;
			
			ASTNode parent= block.getParent();
			if (!(parent instanceof Statement))
				return null;
			
			statement= (Statement)parent;
		}
		
		if (statement instanceof IfStatement) {
			IFixRewriteOperation[] allConvert= getAllConverts(((IfStatement)statement));
			
			String label1= FixMessages.ControlStatementsFix_removeIfBlock_proposalDescription;
			RemoveBlockOperation op1= createRemoveBlockOperation (statement, IfStatement.THEN_STATEMENT_PROPERTY, false);
			String label2= FixMessages.ControlStatementsFix_removeElseBlock_proposalDescription;
			RemoveBlockOperation op2= createRemoveBlockOperation(statement, IfStatement.ELSE_STATEMENT_PROPERTY, false);
			if (op1 != null && op2 != null) {
				return new IFix[] {
						new ControlStatementsFix(label1, compilationUnit, new IFixRewriteOperation[] {op1}),
						new ControlStatementsFix(label2, compilationUnit, new IFixRewriteOperation[] {op2}),
						new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfElseBlock_proposalDescription, compilationUnit, allConvert)
				};
			} else if (op1 != null) {
				if (allConvert.length == 1) {
					return new IFix[] {new ControlStatementsFix(label1, compilationUnit, new IFixRewriteOperation[] {op1})};
				} else {
					return new IFix[] {
							new ControlStatementsFix(label1, compilationUnit, new IFixRewriteOperation[] {op1}),
							new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfElseBlock_proposalDescription, compilationUnit, allConvert)
							};
				}
			} else if (op2 != null) {
				if (allConvert.length == 1) {
					return new IFix[] {new ControlStatementsFix(label2, compilationUnit, new IFixRewriteOperation[] {op2})};
				} else {
					return new IFix[] {
							new ControlStatementsFix(label2, compilationUnit, new IFixRewriteOperation[] {op2}),
							new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfElseBlock_proposalDescription, compilationUnit, allConvert)
							};
					
				}
			}
		} else if (statement instanceof WhileStatement) {
			RemoveBlockOperation op= createRemoveBlockOperation(statement, WhileStatement.BODY_PROPERTY, false);
			if (op != null) {
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof ForStatement) {
			RemoveBlockOperation op= createRemoveBlockOperation(statement, ForStatement.BODY_PROPERTY, false);
			if (op != null) {
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof EnhancedForStatement) {
			RemoveBlockOperation op= createRemoveBlockOperation(statement, EnhancedForStatement.BODY_PROPERTY, false);
			if (op != null) {
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof DoStatement) {
			RemoveBlockOperation op= createRemoveBlockOperation(statement, DoStatement.BODY_PROPERTY, false);
			if (op != null) {
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		}
		
		return null;
	}
	
	private static IFixRewriteOperation[] getAllConverts(IfStatement statement) {
		IfStatement start= statement;
		while (start.getParent() instanceof IfStatement) {
			start= (IfStatement)start.getParent();
		}
		List result= new ArrayList();
		RemoveBlockOperation op= createRemoveBlockOperation(start, IfStatement.THEN_STATEMENT_PROPERTY, false);
		if (op != null) {
			result.add(op);
		}
		while (start.getElseStatement() instanceof IfStatement) {
			start= (IfStatement)start.getElseStatement();
			op= createRemoveBlockOperation(start, IfStatement.THEN_STATEMENT_PROPERTY, false);
			if (op != null) {
				result.add(op);
			}
		}
		op= createRemoveBlockOperation(start, IfStatement.ELSE_STATEMENT_PROPERTY, false);
		if (op != null) {
			result.add(op);
		}
		
		return (IFixRewriteOperation[])result.toArray(new IFixRewriteOperation[result.size()]);
	}

	private static RemoveBlockOperation createRemoveBlockOperation(Statement statement, ChildPropertyDescriptor child, boolean onlyReturnAndThrows) {
		if (!RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, child))
			return null;
		
		return new RemoveBlockOperation((Block) statement.getStructuralProperty(child), statement, child);
	}

	public static IFix createCleanUp(CompilationUnit compilationUnit, 
			boolean convertSingleStatementToBlock, 
			boolean removeUnnecessaryBlock,
			boolean removeUnnecessaryBlockContainingReturnOrThrow,
			boolean convertForLoopToEnhanced) throws CoreException {
		
		if (!convertSingleStatementToBlock && !convertForLoopToEnhanced && !removeUnnecessaryBlock && !removeUnnecessaryBlockContainingReturnOrThrow)
			return null;
		
		List operations= new ArrayList();
		ControlStatementFinder finder= new ControlStatementFinder(compilationUnit, convertSingleStatementToBlock, removeUnnecessaryBlock, removeUnnecessaryBlockContainingReturnOrThrow, convertForLoopToEnhanced, operations);
		compilationUnit.accept(finder);
		
		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new ControlStatementsFix("", compilationUnit, ops); //$NON-NLS-1$
	}
	

	protected ControlStatementsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
