package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

public class SuperReferenceFinder {

	//no instances
	private SuperReferenceFinder(){
	}
	
	public static ISourceRange[] findSuperReferenceRanges(IMethod method) throws JavaModelException{
		Assert.isNotNull(method);
		SuperReferenceFinderVisitor visitor= new SuperReferenceFinderVisitor(method);
		AST.parseCompilationUnit(method.getCompilationUnit(), false).accept(visitor);
		return visitor.getSuperReferenceRanges();
	}
	
	private static class SuperReferenceFinderVisitor extends ASTVisitor{
		
		private Collection fFoundRanges;
		private int fMethodSourceStart;
		private int fMethodSourceEnd;
		private String fMethodSource;
		
		SuperReferenceFinderVisitor(IMethod method) throws JavaModelException{
			fFoundRanges= new ArrayList(0);
			fMethodSourceStart= method.getSourceRange().getOffset();
			fMethodSourceEnd= method.getSourceRange().getOffset() + method.getSourceRange().getLength();	
			fMethodSource= method.getSource();
		}
		
		ISourceRange[] getSuperReferenceRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private boolean withinMethod(ASTNode node){
			return (node.getStartPosition() >= fMethodSourceStart) && (node.getStartPosition() <= fMethodSourceEnd);
		}
		
		private ISourceRange getSuperRange(String scanSource){
			Scanner scanner= new Scanner(false, false);
			scanner.setSourceBuffer(scanSource.toCharArray());
			try {
				int token = scanner.getNextToken();
				while (token != TerminalSymbols.TokenNameEOF) {
					switch (token) {
						case TerminalSymbols.TokenNamesuper :
							int start= scanner.currentPosition - scanner.getCurrentTokenSource().length;
							int end= scanner.currentPosition;
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
