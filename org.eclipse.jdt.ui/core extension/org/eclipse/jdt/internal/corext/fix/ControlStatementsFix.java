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
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IfStatement;
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
	
	public interface IChangeBlockOperation {

		public void setSubOperation(IFixRewriteOperation op);

		public Statement getSingleStatement();

    }

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
				List resultingCollection) {
			
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
				Statement doBody= node.getBody();
				if (!(doBody instanceof Block)) {
					fResult.add(new AddBlockOperation(DoStatement.BODY_PROPERTY, doBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesCleanUpPrecondition(node, DoStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
					fResult.add(new RemoveBlockOperation(node, DoStatement.BODY_PROPERTY));
				}
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
					if (iterableConverter.isApplicable().isOK()) {
						fResult.add(iterableConverter);
						fUsedNames.put(node, identifierName);
					} else if (fFindControlStatementsWithoutBlock) {
						Statement forBody= node.getBody();
						if (!(forBody instanceof Block)) {
							fResult.add(new AddBlockOperation(ForStatement.BODY_PROPERTY, forBody, node));
						}
					} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
						if (RemoveBlockOperation.satisfiesCleanUpPrecondition(node, ForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
							fResult.add(new RemoveBlockOperation(node, ForStatement.BODY_PROPERTY));
						}
					}
				}
			} else if (fFindControlStatementsWithoutBlock) {
				Statement forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fResult.add(new AddBlockOperation(ForStatement.BODY_PROPERTY, forBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesCleanUpPrecondition(node, ForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
					fResult.add(new RemoveBlockOperation(node, ForStatement.BODY_PROPERTY));
				}
			}
			return super.visit(node);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(EnhancedForStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				Statement forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fResult.add(new AddBlockOperation(EnhancedForStatement.BODY_PROPERTY, forBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesCleanUpPrecondition(node, EnhancedForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
					fResult.add(new RemoveBlockOperation(node, EnhancedForStatement.BODY_PROPERTY));
				}
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
				Statement then= statement.getThenStatement();
				if (!(then instanceof Block)) {
					fResult.add(new AddBlockOperation(IfStatement.THEN_STATEMENT_PROPERTY, then, statement));
				}
				Statement elseStatement= statement.getElseStatement();
				if (elseStatement != null && !(elseStatement instanceof Block) && !(elseStatement instanceof IfStatement)) {
					fResult.add(new AddBlockOperation(IfStatement.ELSE_STATEMENT_PROPERTY, elseStatement, statement));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesCleanUpPrecondition(statement, IfStatement.THEN_STATEMENT_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
					fResult.add(new RemoveBlockOperation(statement, IfStatement.THEN_STATEMENT_PROPERTY));
				}
				if (!(statement.getElseStatement() instanceof IfStatement)) {
					if (RemoveBlockOperation.satisfiesCleanUpPrecondition(statement, IfStatement.ELSE_STATEMENT_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
						fResult.add(new RemoveBlockOperation(statement, IfStatement.ELSE_STATEMENT_PROPERTY));
					}	
				}
			}
			return super.visit(statement);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.WhileStatement)
		 */
		public boolean visit(WhileStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				Statement whileBody= node.getBody();
				if (!(whileBody instanceof Block)) {
					fResult.add(new AddBlockOperation(WhileStatement.BODY_PROPERTY, whileBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesCleanUpPrecondition(node, WhileStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow))
					fResult.add(new RemoveBlockOperation(node, WhileStatement.BODY_PROPERTY));
			}
			return super.visit(node);
		}

	}
	
	private static class IfElseIterator {
		
		private IfStatement fCursor;
		
		public IfElseIterator(IfStatement item) {
			fCursor= findStart(item);
		}
		
		public IfStatement next() {
			if (!hasNext())
				return null;
			
			IfStatement result= fCursor;
			
			if (fCursor.getElseStatement() instanceof IfStatement) {
				fCursor= (IfStatement)fCursor.getElseStatement();
			} else {
				fCursor= null;
			}
			
			return result;
		}
		
		public boolean hasNext() {
			return fCursor != null;
		}

		private IfStatement findStart(IfStatement item) {
            while (item.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
            	item= (IfStatement)item.getParent();
            }
            return item;
        }
	}
	
	private static final class AddBlockOperation extends AbstractFixRewriteOperation implements IChangeBlockOperation {

		private final ChildPropertyDescriptor fBodyProperty;
		private final Statement fBody;
		private final Statement fControlStatement;
		private IFixRewriteOperation fOperation;

		public AddBlockOperation(ChildPropertyDescriptor bodyProperty, Statement body, Statement controlStatement) {
			fBodyProperty= bodyProperty;
			fBody= body;
			fControlStatement= controlStatement;
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
			
			ASTNode moveTarget;
			if (fOperation == null) {
				moveTarget= rewrite.createMoveTarget(fBody);
			} else {
				if (fOperation instanceof ConvertForLoopOperation) {
					ConvertForLoopOperation convertForLoopOperation= ((ConvertForLoopOperation)fOperation);
					convertForLoopOperation.makePassive();
					fOperation.rewriteAST(cuRewrite, textEditGroups);
					moveTarget= convertForLoopOperation.getEnhancedForStatement();
				} else  {
					ConvertIterableLoopOperation convertIterableLoopOperation= ((ConvertIterableLoopOperation)fOperation);
					convertIterableLoopOperation.makePassive();
					fOperation.rewriteAST(cuRewrite, textEditGroups);
					moveTarget= convertIterableLoopOperation.getEnhancedForStatement();
				}
			}
			
			Block replacingBody= cuRewrite.getRoot().getAST().newBlock();
			replacingBody.statements().add(moveTarget);
			rewrite.set(fControlStatement, fBodyProperty, replacingBody, group);
		}

		/**
         * {@inheritDoc}
         */
        public Statement getSingleStatement() {
	        return fBody;
        }

		/**
         * {@inheritDoc}
         */
        public void setSubOperation(IFixRewriteOperation op) {
			fOperation= op;
        }
	}
	
	static class RemoveBlockOperation extends AbstractFixRewriteOperation implements IChangeBlockOperation {

		private final Statement fStatement;
		private final ChildPropertyDescriptor fChild;
		private IFixRewriteOperation fOperation;

		public RemoveBlockOperation(Statement controlStatement, ChildPropertyDescriptor child) {
			fStatement= controlStatement;
			fChild= child;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			Statement moveTarget;
			if (fOperation == null) {
				moveTarget= (Statement)rewrite.createMoveTarget(getSingleStatement());
			} else {
				if (fOperation instanceof ConvertForLoopOperation) {
					ConvertForLoopOperation convertForLoopOperation= ((ConvertForLoopOperation)fOperation);
					convertForLoopOperation.makePassive();
					fOperation.rewriteAST(cuRewrite, textEditGroups);
					moveTarget= convertForLoopOperation.getEnhancedForStatement();
				} else  {
					ConvertIterableLoopOperation convertIterableLoopOperation= ((ConvertIterableLoopOperation)fOperation);
					convertIterableLoopOperation.makePassive();
					fOperation.rewriteAST(cuRewrite, textEditGroups);
					moveTarget= convertIterableLoopOperation.getEnhancedForStatement();
				}
			}
			
			TextEditGroup group= createTextEditGroup(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription);
			textEditGroups.add(group);
			rewrite.set(fStatement, fChild, moveTarget, group);
		}
		
        public void setSubOperation(IFixRewriteOperation op) {
        	fOperation= op;
        }
		
        public Statement getSingleStatement() {
	        return (Statement)((Block)fStatement.getStructuralProperty(fChild)).statements().get(0);
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
		IStatus status= loopConverter.isApplicable();
		if (status.getSeverity() == IStatus.ERROR)
			return null;

		ControlStatementsFix result= new ControlStatementsFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
		result.setStatus(status);
		return result;
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
			List result= new ArrayList();
			
			List removeAllList= new ArrayList();
			
			IfElseIterator iter= new IfElseIterator((IfStatement)statement);
			IfStatement item= null;
			while (iter.hasNext()) {
				item= iter.next();
				if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(item, IfStatement.THEN_STATEMENT_PROPERTY)) {
            		RemoveBlockOperation op= new RemoveBlockOperation(item, IfStatement.THEN_STATEMENT_PROPERTY);
					removeAllList.add(op);
					if (item == statement)
						result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfBlock_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op}));
            	}
			}
			
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(item, IfStatement.ELSE_STATEMENT_PROPERTY)) {
            	RemoveBlockOperation op= new RemoveBlockOperation(item, IfStatement.ELSE_STATEMENT_PROPERTY);
				removeAllList.add(op);
				if (item == statement)
					result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeElseBlock_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op}));
            }
            
			if (removeAllList.size() > 1) {
				IFixRewriteOperation[] allConvert= (IFixRewriteOperation[])removeAllList.toArray(new IFixRewriteOperation[removeAllList.size()]);
				result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfElseBlock_proposalDescription, compilationUnit, allConvert));
            }
            
            return (IFix[])result.toArray(new IFix[result.size()]);
		} else if (statement instanceof WhileStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, WhileStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, WhileStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof ForStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, ForStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, ForStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof EnhancedForStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, EnhancedForStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, EnhancedForStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof DoStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, DoStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, DoStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		}
		
		return null;
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
		
		flattenOperations(operations);
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new ControlStatementsFix("", compilationUnit, ops); //$NON-NLS-1$
	}	

    private static void flattenOperations(List operations) {
    	Hashtable changeBlockOps= new Hashtable();
    	for (Iterator iterator= operations.iterator(); iterator.hasNext();) {
	        IFixRewriteOperation op= (IFixRewriteOperation)iterator.next();
	        if (op instanceof IChangeBlockOperation) {
	        	IChangeBlockOperation rOp= (IChangeBlockOperation)op;
	        	Statement singleStatement= rOp.getSingleStatement();
	        	if (singleStatement instanceof ForStatement)
					changeBlockOps.put(singleStatement, rOp);
	        }
        }
    	
    	for (Iterator iterator= operations.iterator(); iterator.hasNext();) {
	        IFixRewriteOperation op= (IFixRewriteOperation)iterator.next();
	        if (op instanceof ConvertForLoopOperation) {
	        	ConvertForLoopOperation convertOp= (ConvertForLoopOperation)op;
	        	ForStatement forStatement= convertOp.getForLoop();
	        	if (changeBlockOps.containsKey(forStatement)) {
	        		IChangeBlockOperation changeOp= (IChangeBlockOperation)changeBlockOps.get(forStatement);
	        		changeOp.setSubOperation(convertOp);
	        		iterator.remove();
	        	}
	        } else if (op instanceof ConvertIterableLoopOperation) {
	        	ConvertIterableLoopOperation convertOp= (ConvertIterableLoopOperation)op;
	        	ForStatement forStatement= convertOp.getForLoop();
	        	if (changeBlockOps.containsKey(forStatement)) {
	        		IChangeBlockOperation changeOp= (IChangeBlockOperation)changeBlockOps.get(forStatement);
	        		changeOp.setSubOperation(convertOp);
	        		iterator.remove();
	        	}
	        }
        }
    }

	protected ControlStatementsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
