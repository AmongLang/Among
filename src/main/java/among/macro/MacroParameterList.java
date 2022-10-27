package among.macro;

import among.ToPrettyString;
import among.ToStringContext;
import among.ToStringOption;
import among.exception.Sussy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Immutable list of {@link MacroParameter}s. Provides both search-by-index and search-by-name functionality.
 *
 * @see MacroDefinition
 * @see MacroParameter
 */
public final class MacroParameterList extends ToPrettyString.Base{
	private static final MacroParameterList EMPTY = new MacroParameterList();

	/**
	 * @return Empty parameter list
	 */
	public static MacroParameterList of(){
		return EMPTY;
	}
	/**
	 * @param parameters Array of parameters
	 * @return Parameter list with given parameters
	 * @throws NullPointerException If the array or one of the parameters are {@code null}
	 * @throws Sussy                If two parameters have same name
	 */
	public static MacroParameterList of(MacroParameter... parameters){
		return parameters.length==0 ? EMPTY : new MacroParameterList(parameters);
	}
	/**
	 * @param parameters List of parameters
	 * @return Parameter list with given parameters
	 * @throws NullPointerException If the collection or one of the parameters are {@code null}
	 * @throws Sussy                If two parameters have same name
	 */
	public static MacroParameterList of(Collection<MacroParameter> parameters){
		return parameters.isEmpty() ? EMPTY : new MacroParameterList(parameters);
	}

	private final List<MacroParameter> params = new ArrayList<>();
	private final Map<String, Integer> nameToIndex = new HashMap<>();

	private MacroParameterList(MacroParameter... parameters){
		this(Arrays.asList(parameters));
	}
	private MacroParameterList(Collection<MacroParameter> parameters){
		for(MacroParameter p : parameters){
			if(nameToIndex.put(p.name(), params.size())!=null)
				throw new Sussy("Duplicated parameter '"+p.name()+"'");
			params.add(p);
		}
	}

	public int size(){
		return params.size();
	}
	public boolean isEmpty(){
		return params.isEmpty();
	}

	/**
	 * Returns unmodifiable view of all parameter names mapped to their respective index. Iteration order is not
	 * guaranteed to be same with declaration order.
	 *
	 * @return Unmodifiable view of all parameter names mapped to their respective index
	 */
	public Map<String, Integer> parameters(){
		return Collections.unmodifiableMap(nameToIndex);
	}

	/**
	 * Returns parameter declared at specific index.
	 *
	 * @param index Index of the parameter
	 * @return Parameter at specified index
	 * @throws IndexOutOfBoundsException If {@code index < 0 || index >= size()}
	 */
	public MacroParameter paramAt(int index){
		return params.get(index);
	}
	/**
	 * Searches for index of parameter with given name. If there is no parameter with given name, {@code index} is
	 * returned.
	 *
	 * @param paramName Name of the parameter
	 * @return Index of the parameter with given name, or {@code -1} if there isn't
	 * @throws NullPointerException If {@code paramName == null}
	 */
	public int indexOf(String paramName){
		Integer i = nameToIndex.get(paramName);
		return i!=null ? i : -1;
	}

	/**
	 * Returns whether this parameter list contains at least one optional parameter.
	 *
	 * @return Whether this parameter list contains at least one optional parameter
	 */
	public boolean hasOptionalParams(){
		for(MacroParameter p : params)
			if(p.defaultValue()!=null) return true;
		return false;
	}

	/**
	 * Returns whether this parameter list has consecutive optional parameters. Being 'consecutive' refers to all
	 * optional parameters being placed at the end of the parameter list; see the snippet below.
	 * <pre>
	 * macro macro1[param1, param2, param3]: stub  // consecutive
	 * macro macro2[param1, param2, param3 = defaultValue]: stub  // consecutive
	 * macro macro3[param1, param2 = defaultValue, param3 = defaultValue]: stub  // consecutive
	 * macro macro4[param1 = defaultValue, param2 = defaultValue, param3 = defaultValue]: stub  // consecutive
	 * macro macro5[param1 = defaultValue, param2, param3]: stub  // NOT consecutive, will produce error
	 * macro macro6[param1, param2 = defaultValue, param3]: stub  // NOT consecutive, will produce error
	 * macro macro7[]: stub  // consecutive
	 * </pre>
	 * As mentioned above, this check is only done for list/operation macros; it is because the parameters are
	 * identified by their index. Same rule does not apply for object macros; As their parameters are based on
	 * unordered
	 * properties.
	 * <pre>
	 * macro macro1{param1 = defaultValue, param2, param3}: stub  // NOT consecutive, but does not produce error
	 *
	 * macro1{
	 *     param1: "Parameter 1"
	 *     param2: "Parameter 2"
	 *     param3: "Parameter 3"
	 * }
	 * </pre>
	 *
	 * @return Whether this parameter list has consecutive optional parameters
	 */
	public boolean hasConsecutiveOptionalParams(){
		boolean defaultParamSeen = false;
		for(MacroParameter p : params){
			if(defaultParamSeen){
				if(p.defaultValue()==null)
					return false;
			}else if(p.defaultValue()!=null)
				defaultParamSeen = true;
		}
		return true;
	}

	/**
	 * Returns stream containing only required parameters, i.e. parameters without default value.
	 *
	 * @return Stream of required parameters
	 */
	public Stream<MacroParameter> requiredParameters(){
		return this.params.stream().filter(p -> p.defaultValue()==null);
	}

	/**
	 * Returns stream containing only optional parameters, i.e. parameters with default value.
	 *
	 * @return Stream of optional parameters
	 */
	public Stream<MacroParameter> optionalParameters(){
		return this.params.stream().filter(p -> p.defaultValue()!=null);
	}

	private int requiredParameterSize = -1, optionalParameterSize = -1;

	/**
	 * Returns number of required parameters. Optional parameters are not counted.
	 *
	 * @return Number of required parameters
	 */
	public int requiredParameterSize(){
		if(requiredParameterSize<0){
			requiredParameterSize = 0;
			for(MacroParameter p : this.params)
				if(p.defaultValue()==null) requiredParameterSize++;
		}
		return requiredParameterSize;
	}

	/**
	 * Returns number of optional parameters. Required parameters are not counted.
	 *
	 * @return Number of optional parameters
	 */
	public int optionalParameterSize(){
		if(optionalParameterSize<0){
			optionalParameterSize = 0;
			for(MacroParameter p : this.params)
				if(p.defaultValue()!=null) optionalParameterSize++;
		}
		return optionalParameterSize;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		MacroParameterList that = (MacroParameterList)o;
		return params.equals(that.params);
	}
	@Override public int hashCode(){
		return Objects.hash(params);
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		boolean first = true;
		for(MacroParameter p : params){
			if(first) first = false;
			else stb.append(',');
			p.toString(stb, option, ToStringContext.NONE);
		}
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		toPrettyString(stb, indents, option, false);
	}
	public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, boolean replaceDefaultValueWithStubs){
		boolean first = true;
		for(MacroParameter p : params){
			if(first) first = false;
			else stb.append(", ");
			p.toPrettyString(stb, indents+1, option, replaceDefaultValueWithStubs);
		}
	}
}
