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
				ASTNode doBody= node.getBody();
				if (!(doBody instanceof Block)) {
					fResult.add(new AddBlockOperation(DoStatement.BODY_PROPERTY, doBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesPrecondition(node, DoStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
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
					if (iterableConverter.isApplicable()) {
						fResult.add(iterableConverter);
						fUsedNames.put(node, identifierName);
					} else if (fFindControlStatementsWithoutBlock) {
						ASTNode forBody= node.getBody();
						if (!(forBody instanceof Block)) {
							fResult.add(new AddBlockOperation(ForStatement.BODY_PROPERTY, forBody, node));
						}
					} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
						if (RemoveBlockOperation.satisfiesPrecondition(node, ForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
							fResult.add(new RemoveBlockOperation(node, ForStatement.BODY_PROPERTY));
						}
					}
				}
			} else if (fFindControlStatementsWithoutBlock) {
				ASTNode forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fResult.add(new AddBlockOperation(ForStatement.BODY_PROPERTY, forBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesPrecondition(node, ForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
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
				ASTNode forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fResult.add(new AddBlockOperation(EnhancedForStatement.BODY_PROPERTY, forBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesPrecondition(node, EnhancedForStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
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
				ASTNode then= statement.getThenStatement();
				if (!(then instanceof Block)) {
					fResult.add(new AddBlockOperation(IfStatement.THEN_STATEMENT_PROPERTY, then, statement));
				}
				ASTNode elseStatement= statement.getElseStatement();
				if (elseStatement != null && !(elseStatement instanceof Block) && !(elseStatement instanceof IfStatement)) {
					fResult.add(new AddBlockOperation(IfStatement.ELSE_STATEMENT_PROPERTY, elseStatement, statement));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesPrecondition(statement, IfStatement.THEN_STATEMENT_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
					fResult.add(new RemoveBlockOperation(statement, IfStatement.THEN_STATEMENT_PROPERTY));
				}
				if (!(statement.getElseStatement() instanceof IfStatement)) {
					if (RemoveBlockOperation.satisfiesPrecondition(statement, IfStatement.ELSE_STATEMENT_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow)) {
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
				ASTNode whileBody= node.getBody();
				if (!(whileBody instanceof Block)) {
					fResult.add(new AddBlockOperation(WhileStatement.BODY_PROPERTY, whileBody, node));
				}
			} else if (fRemoveUnnecessaryBlocks || fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (RemoveBlockOperation.satisfiesPrecondition(node, WhileStatement.BODY_PROPERTY, fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow))
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
            while (item.getParent() instanceof IfStatement) {
            	item= (IfStatement)item.getParent();
            }
            return item;
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

		private final Statement fStatement;
		private final ChildPropertyDescriptor fChild;

		public RemoveBlockOperation(Statement controlStatement, ChildPropertyDescriptor child) {
			fStatement= controlStatement;
			fChild= child;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			Block block= (Block)fStatement.getStructuralProperty(fChild);
			Statement moveTarget= (Statement)rewrite.createMoveTarget((ASTNode)block.statements().get(0));
			
			TextEditGroup group= createTextEditGroup(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription);
			textEditGroups.add(group);
			rewrite.set(fStatement, fChild, moveTarget, group);
		}

		//Can the block around child with childDescriptor of controlStatement be removed?
        private static boolean satisfiesPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor, boolean onlyReturnAndThrows) {
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
        		// 	if (false)
        		//   ;
        		// } else
        		//   ;
        		
        		if (((IfStatement)controlStatement).getThenStatement() != child)
        			return true;//can always remove blocks in else part
        		
        		if (!(singleStatement instanceof IfStatement))
        			return true;//can always remove if single statement is not an if statement
        		
        		IfStatement ifStatement= (IfStatement)controlStatement;
        		if (ifStatement.getElseStatement() == null)
        			return true;//can always remove if no else part
        		
        		return false;
        	} else {
        		return true;
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
			List result= new ArrayList();
			
			List removeAllList= new ArrayList();
			
			IfElseIterator iter= new IfElseIterator((IfStatement)statement);
			IfStatement item= null;
			while (iter.hasNext()) {
				item= iter.next();
				if (RemoveBlockOperation.satisfiesPrecondition(item, IfStatement.THEN_STATEMENT_PROPERTY, false)) {
            		RemoveBlockOperation op= new RemoveBlockOperation(item, IfStatement.THEN_STATEMENT_PROPERTY);
					removeAllList.add(op);
					if (item == statement)
						result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfBlock_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op}));
            	}
			}
			
			if (RemoveBlockOperation.satisfiesPrecondition(item, IfStatement.ELSE_STATEMENT_PROPERTY, false)) {
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
			if (RemoveBlockOperation.satisfiesPrecondition(statement, WhileStatement.BODY_PROPERTY, false)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, WhileStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof ForStatement) {
			if (RemoveBlockOperation.satisfiesPrecondition(statement, ForStatement.BODY_PROPERTY, false)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, ForStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof EnhancedForStatement) {
			if (RemoveBlockOperation.satisfiesPrecondition(statement, EnhancedForStatement.BODY_PROPERTY, false)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, EnhancedForStatement.BODY_PROPERTY);
				return new IFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new IFixRewriteOperation[] {op})};
			}
		} else if (statement instanceof DoStatement) {
			if (RemoveBlockOperation.satisfiesPrecondition(statement, DoStatement.BODY_PROPERTY, false)) {
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
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new ControlStatementsFix("", compilationUnit, ops); //$NON-NLS-1$
	}	

	protected ControlStatementsFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
