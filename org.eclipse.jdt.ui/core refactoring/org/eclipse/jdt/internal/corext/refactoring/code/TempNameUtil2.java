package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;

public class TempNameUtil2 {

	private TempNameUtil2(){
	}
	
	public static String[] getLocalNames(VariableDeclaration localDeclaration){
		Set set= getLocalNameMap(localDeclaration).keySet();
		return (String[]) set.toArray(new String[set.size()]);
	}
	
	//String -> ISourceRange
	public static Map getLocalNameMap(VariableDeclaration localDeclaration){
		MethodDeclaration methodNode= findMethodNode(localDeclaration);
		if (methodNode == null)
			return new HashMap(0);
		
		TempCollector tempCollector= new TempCollector();
		methodNode.accept(tempCollector);
		VariableDeclaration[] temps= tempCollector.getTempDeclarations();
		
		Map result= new HashMap();
		for (int i= 0; i < temps.length; i++) {
			result.put(getName(temps[i]), new SourceRange(temps[i].getStartPosition(), temps[i].getLength()));
		}
		return result;
	}

	private static String getName(VariableDeclaration localDeclaration){
		if (localDeclaration instanceof SingleVariableDeclaration)
			return ((SingleVariableDeclaration)localDeclaration).getName().getIdentifier();
		if (localDeclaration instanceof VariableDeclarationFragment)
			return ((VariableDeclarationFragment)localDeclaration).getName().getIdentifier();
		Assert.isTrue(false);	
		return "";
	}	
	private static MethodDeclaration findMethodNode(VariableDeclaration localDeclaration){
		ASTNode current= localDeclaration;
		while (current != null && ! (current instanceof MethodDeclaration))
			current= current.getParent();
		return (MethodDeclaration)current;
	}
	
	//--- visitor ---
	private static class TempCollector extends ASTVisitor{
		
		private Collection fTemps;
		
		TempCollector(){
			fTemps= new ArrayList();
		}
		
		VariableDeclaration[] getTempDeclarations(){
			return (VariableDeclaration[]) fTemps.toArray(new VariableDeclaration[fTemps.size()]);	
		}
		
		public boolean visit(SingleVariableDeclaration node) {
			fTemps.add(node);
			return super.visit(node);
		}

		public boolean visit(VariableDeclarationFragment node) {
			fTemps.add(node);
			return super.visit(node);
		}
		
		//-----  stop nodes --------
		public boolean visit(ClassInstanceCreation node) {
			return false;
		}
		
		public boolean visit(TypeDeclaration node) {
			return false;
		}
	}
}
