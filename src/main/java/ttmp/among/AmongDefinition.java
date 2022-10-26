package ttmp.among;

import ttmp.among.macro.MacroRegistry;
import ttmp.among.operator.OperatorRegistry;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Represents a definition side of the source, i.e. macro and operator definitions.
 */
public final class AmongDefinition extends ToPrettyString.Base{
	private final MacroRegistry macros;
	private final OperatorRegistry operators;

	/**
	 * Create new empty definition - no macros, no operators, nothing.
	 */
	public AmongDefinition(){
		this.macros = new MacroRegistry();
		this.operators = new OperatorRegistry();
	}
	private AmongDefinition(AmongDefinition copyFrom){
		this.macros = new MacroRegistry(copyFrom.macros);
		this.operators = new OperatorRegistry(copyFrom.operators);
	}

	public boolean isEmpty(){
		return macros.isEmpty()&&operators.isEmpty();
	}
	public void clear(){
		macros.clear();
		operators.clear();
	}

	public MacroRegistry macros(){
		return macros;
	}
	public OperatorRegistry operators(){
		return operators;
	}

	/**
	 * Create a shallow copy of this object. Macro and operator instances are recycled.
	 *
	 * @return A shallow copy of this object
	 */
	public AmongDefinition copy(){
		return new AmongDefinition(this);
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		Iterator<ToPrettyString.Base> it = Stream.concat(macros.allMacros(), operators.allOperators()).iterator();
		boolean first = true;
		while(it.hasNext()){
			if(first) first = false;
			else stb.append(',');
			it.next().toString(stb, option, ToStringContext.NONE);
		}
	}
	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		Iterator<ToPrettyString.Base> it = Stream.concat(macros.allMacros(), operators.allOperators()).iterator();
		boolean first = true;
		while(it.hasNext()){
			if(first) first = false;
			else stb.append('\n');
			it.next().toPrettyString(stb, indents, option, ToStringContext.NONE);
		}
	}
}
