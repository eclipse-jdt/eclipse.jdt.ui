package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class SuperReferenceFinder {

	//no instances
	private SuperReferenceFinder(){
	}
	
	public static ISourceRange[] findSuperReferenceRanges(IMethod method, IType superType) throws JavaModelException{
		Assert.isNotNull(method);
		SuperReferenceFinderVisitor visitor= new SuperReferenceFinderVisitor(method, superType);
		AST.parseCompilationUnit(method.getCompilationUnit(), true).accept(visitor);
		return visitor.getSuperReferenceRanges();
	}
	
	private static class SuperReferenceFinderVisitor extends ASTVisitor{
		
		private Collection fFoundRanges;
		private int fMethodSourceStart;
		private int fMethodSourceEnd;
		private String fMethodSource;
		private String fSuperTypeName;
		
		SuperReferenceFinderVisitor(IMethod method, IType superType) throws JavaModelException{
			fFoundRanges= new ArrayList(0);
			fMethodSourceStart= method.getSourceRange().getOffset();
			fMethodSourceEnd= method.getSourceRange().getOffset() + method.getSourceRange().getLength();	
			fMethodSource= method.getSource();
			fSuperTypeName= JavaModelUtil.getFullyQualifiedName(superType);
		}
		
		ISourceRange[] getSuperReferenceRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private boolean withinMethod(ASTNode node){
			return (node.getStartPosition() >= fMethodSourceStart) && (node.getStartPosition() <= fMethodSourceEnd);
		}
		
		private ISourceRange getSuperRange(String scanSource){
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(scanSource.toCharArray());
			try {
				int token = scanner.getNextToken();
				while (token != ITerminalSymbols.TokenNameEOF) {
					switch (token) {
						case ITerminalSymbols.TokenNamesuper :
							int start= scanner.getCurrentTokenEndPosition() + 1 - scanner.getCurrentTokenSource().length;
							int end= scanner.getCurrentTokenEndPosition() + 1;
							return new SourceRange(start, end - start);
					}
					token = scanner.getNextToken();
				}
			} catch(InvalidInputException e) {
				return new SourceRange(0, 0); //FIX ME
			}
			return new SourceRange(0, 0);//FIX ME
		}

		private String getSource(int start, int end){
			return fMethodSource.substring(start - fMethodSourceStart, end - fMethodSourceStart);
		}
		
		private String getScanSource(SuperMethodInvocation node){
			return getSource(getScanSourceOffset(node), node.getName().getStartPosition());
		}
		
		private String getScanSource(SuperFieldAccess node){
			return getSource(getScanSourceOffset(node), node.getName().getStartPosition());
		}
		
		private static int getScanSourceOffset(SuperMethodInvocation node){
			if (node.getQualifier() == null)
				return node.getStartPosition();
			else
				return node.getQualifier().getStartPosition() + node.getQualifier().getLength();			
		}
		
		private static int getScanSourceOffset(SuperFieldAccess node){
			if (node.getQualifier() == null)
				return node.getStartPosition();
			else
				return node.getQualifier().getStartPosition() + node.getQualifier().getLength();			
		}
		
		//---- visit methods ------------------
		
		public boolean visit(SuperFieldAccess node) {
			if (! withinMethod(node))
				return true;
			
			ISourceRange superRange= getSuperRange(getScanSource(node));
			fFoundRanges.add(new SourceRange(superRange.getOffset() + getScanSourceOffset(node), superRange.getLength()));
			return true;
		}

		public boolean visit(SuperMethodInvocation node) {
			if (! withinMethod(node))
				return true;
			
			IBinding nameBinding= node.getName().resolveBinding();
			if (nameBinding != null && nameBinding.getKind() == IBinding.METHOD){
				ITypeBinding declaringType= ((IMethodBinding)nameBinding).getDeclaringClass();
				if (declaringType != null && ! fSuperTypeName.equals(Bindings.getFullyQualifiedName(declaringType)))
					return true;
			}
			ISourceRange superRange= getSuperRange(getScanSource(node));
			fFoundRanges.add(new SourceRange(superRange.getOffset() + getScanSourceOffset(node), superRange.getLength()));
				
			return true;
		}
		
		//- stop nodes ---
		
		public boolean visit(TypeDeclarationStatement node) {
			if (withinMethod(node))
				return false;
			return true;
		}

		public boolean visit(AnonymousClassDeclaration node) {
			if (withinMethod(node))
				return false;
			return true;
		}

	}
}
