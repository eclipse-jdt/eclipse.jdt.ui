package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

class ParameterOffsetFinder {
	
	private ParameterOffsetFinder(){
	}
	
	/**
	 * @param method
	 * @param parameterName
	 * @param includeReferences if it is <code>true</code>, then not only the parameter declaration but also references will be included
	 * @return indices of offsets of the references to the parameter specified in constructor
	 */
	static int[] findOffsets(IMethod method, String parameterName, boolean includeReferences) throws JavaModelException{
		ParameterOffsetFinderVisitor visitor= new ParameterOffsetFinderVisitor(includeReferences, method, parameterName);
		AST.parseCompilationUnit(method.getCompilationUnit(), true).accept(visitor);
		return visitor.getOffsets();
	}
		
	private static class ParameterOffsetFinderVisitor extends ASTVisitor{
		
		private boolean fIncludeReferences;
		private Set fOffsetsFound;
		private int fMethodSourceStart;
		private int fMethodSourceEnd;
		private Set fParamBindings;
		private String fParameterName;
		
		ParameterOffsetFinderVisitor(boolean includeReferences, IMethod method, String parameterName) throws JavaModelException{
			fIncludeReferences= includeReferences;
			fOffsetsFound= new HashSet();
			fParamBindings= new HashSet();
			fMethodSourceStart= computeMethodSourceStart(method);
			fMethodSourceEnd= method.getSourceRange().getOffset() + method.getSourceRange().getLength();
			fParameterName= parameterName;
		}
		
		private static int computeMethodSourceStart(IMethod method) throws JavaModelException{
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(method.getSource().toCharArray());
			scanner.resetTo(0, method.getSourceRange().getLength());
			try{
				scanner.getNextToken();
				return method.getSourceRange().getOffset() + scanner.getCurrentTokenStartPosition();
			}	catch (InvalidInputException e){
				return method.getSourceRange().getOffset();
			}
		}
		
		private void addOffset(int offset){
			fOffsetsFound.add(new Integer(offset));	
		}
		
		private boolean withinMethod(ASTNode node){
			return (node.getStartPosition() >= fMethodSourceStart) 
				&& (node.getStartPosition() <= fMethodSourceEnd);
		}
		
		int[] getOffsets(){
			int[] result= new int[fOffsetsFound.size()];
			Integer[] integerResult= (Integer[])fOffsetsFound.toArray(new Integer[fOffsetsFound.size()]);
			for (int i= 0; i < integerResult.length; i++){
				result[i]= integerResult[i].intValue();
			}
			return result;
		}
		
		private boolean isParameterMatch(SimpleName simpleName){
			if (! withinMethod(simpleName))
				return false;
			if (! simpleName.getIdentifier().equals(fParameterName))
				return false;
			IBinding binding= simpleName.resolveBinding();	
			if (! fParamBindings.contains(binding))
				return false;
			return true;	
		}
		
		///--- visit methods ----
		
		public boolean visit(SimpleName simpleName){
			if (! fIncludeReferences)
				return true;
			
			if  (isParameterMatch(simpleName))
				addOffset(simpleName.getStartPosition());
			return true;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration) {
			if (methodDeclaration.getStartPosition() != fMethodSourceStart)
				return true;
				
			List params= methodDeclaration.parameters();
			for (Iterator iter= params.iterator(); iter.hasNext();) {
				SingleVariableDeclaration param= (SingleVariableDeclaration)iter.next();
				IBinding binding= param.getName().resolveBinding();
				if (binding != null)
					fParamBindings.add(binding);
			}
			return true;
		}
		
	}
}
