package experiments;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.core.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;

class ReorderParameterASTAnalyzer extends AbstractRefactoringASTAnalyzer {
	
	private List fSearchResults;

	ReorderParameterASTAnalyzer(List searchResults){
		fSearchResults= searchResults;
	}
	
	private boolean isStartPositionOnList(int start){
		for (Iterator iter=fSearchResults.iterator(); iter.hasNext();){
			SearchResult sr= (SearchResult)iter.next();
			if (sr.getStart() == start)
				return true;
		};
		return false;
	}

	private boolean isOurMethod(MessageSend messageSend){
		return isStartPositionOnList((int) (messageSend.nameSourcePosition >> 32));
	}
	
	private static boolean canAnyHaveSideEffects(Expression[] ex){
		if (ex == null)
			return false;
		for (int i= 0; i < ex.length; i++){
			if (ex[i] != null && canHaveSideEffects(ex[i]))
				return true;
		}
		return false;
	}
	
	/**
	 * returns true if the expression has for sure safe no side effects
	 */
	private static boolean canHaveSideEffects(Expression ex){
		if (ex == null)
			return false;

		if (ex instanceof AllocationExpression)
			return true;

		if (ex instanceof ArrayAllocationExpression)
			return canAnyHaveSideEffects(((ArrayAllocationExpression)ex).dimensions);

		if (ex instanceof ArrayInitializer)
			return canAnyHaveSideEffects(((ArrayInitializer)ex).expressions);

		if (ex instanceof Assignment)
			return canHaveSideEffects(((Assignment)ex).expression);

		if (ex instanceof CastExpression)
			return canHaveSideEffects(((CastExpression)ex).expression);

		if (ex instanceof ClassLiteralAccess)
			return false;
		
		if (ex instanceof Literal)
			return false;
		
		if (ex instanceof MessageSend)
			return true;
		
		if (ex instanceof BinaryExpression){
			return   canHaveSideEffects(((BinaryExpression)ex).left) 
				  || canHaveSideEffects(((BinaryExpression)ex).right);	
		}	
		
		if (ex instanceof ConditionalExpression){
			return   canHaveSideEffects(((ConditionalExpression)ex).condition) 
				  || canHaveSideEffects(((ConditionalExpression)ex).valueIfTrue)
				  || canHaveSideEffects(((ConditionalExpression)ex).valueIfFalse);		
		}	
		
		if (ex instanceof InstanceOfExpression)
			return canHaveSideEffects(((InstanceOfExpression)ex).expression);
		
		if (ex instanceof UnaryExpression)
			return canHaveSideEffects(((UnaryExpression)ex).expression);
		
		if (ex instanceof ArrayReference){
			return   canHaveSideEffects(((ArrayReference)ex).receiver) 
				  || canHaveSideEffects(((ArrayReference)ex).position);	
		}
		
		if (ex instanceof FieldReference)
			return canHaveSideEffects(((FieldReference)ex).receiver);
			
		if (ex instanceof NameReference)	
			return false;
	
		if (ex instanceof ThisReference)
			return false;
		
		if (ex instanceof TypeReference)
			return false;
		
		return true;	
	}
		
	private void analyzeArgument(Expression ex){
		if (canHaveSideEffects(ex))
			addError("Expression used in method invocation in line:" + getLineNumber(ex) + " may have side effects. If it does, reordering the parameters will modify your program's behavior");
	}
	
	//------- visit methods 
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (! isOurMethod(messageSend))
			return true;
		Assert.isTrue(messageSend.arguments != null, "analyzed method must have arguments");	
		for (int i= 0; i < messageSend.arguments.length; i++)
			analyzeArgument(messageSend.arguments[i]);	
		return true;
	}
	
	
}

