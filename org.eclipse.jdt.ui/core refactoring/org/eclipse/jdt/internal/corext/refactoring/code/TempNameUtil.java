package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.HashMap;
import java.util.Map;

import java.util.Set;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;

public class TempNameUtil {

	private TempNameUtil(){
	}
	
	public static String[] getLocalNames(BlockScope scope){
		Set set= getLocalNameMap(scope).keySet();
		return (String[]) set.toArray(new String[set.size()]);
	}
	
	//String -> ISourceRange
	public static Map getLocalNameMap(BlockScope scope){
		if (scope.locals == null)
			return new HashMap(0);
		Map result= new HashMap();
		LocalVariableBinding[] locals= scope.locals;
		for (int i= 0; i< locals.length; i++){
			if (locals[i] == null)
				continue;
			if (locals[i].declaration == null)	
				continue;
			int offset= locals[i].declaration.sourceStart;
			int length= locals[i].declaration.sourceEnd - locals[i].declaration.sourceStart + 1; 
			result.put(new String(locals[i].name), new SourceRange(offset, length));
		}	
		
		if (scope.subscopes == null)
			return result;	
		
		for (int i= 0; i < scope.subscopes.length; i++){
			Scope subScope= scope.subscopes[i];
			if (subScope instanceof BlockScope)
				result.putAll(getLocalNameMap((BlockScope)subScope));
		}	
		return result;	
	}
}
