package ttmp.among.util;

import ttmp.among.exception.Sussy;
import ttmp.among.obj.MacroDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable list of {@link MacroParameter}s. Provides both search-by-index and search-by-name functionality.
 *
 * @see MacroDefinition
 * @see MacroParameter
 */
public final class MacroParameterList implements ToPrettyString{
	/**
	 * Empty list.
	 */
	private static final MacroParameterList EMPTY = new MacroParameterList();

	/**
	 * @return Empty parameter list
	 */
	public static MacroParameterList of(){
		return EMPTY;
	}
	/**
	 * @return Parameter list with given parameters
	 * @throws NullPointerException If the array or one of the parameters are {@code null}
	 * @throws Sussy                If two parameters have same name
	 */
	public static MacroParameterList of(MacroParameter... parameters){
		return parameters.length==0 ? EMPTY : new MacroParameterList(parameters);
	}
	/**
	 * @return Parameter list with given parameters
	 * @throws NullPointerException If the collection or one of the parameters are {@code null}
	 * @throws Sussy                If two parameters have same name
	 */
	public static MacroParameterList of(Collection<MacroParameter> parameters){
		return parameters.isEmpty() ? EMPTY : new MacroParameterList(parameters);
	}

	private final List<MacroParameter> params = new ArrayList<>();
	private final Map<String, Integer> nameToIndex = new HashMap<>();

	private MacroParameterList(){}
	private MacroParameterList(MacroParameter... parameters){
		for(MacroParameter p : parameters){
			if(nameToIndex.put(p.name(), params.size())!=null)
				throw new Sussy("Duplicated parameter '"+p.name()+"'");
			params.add(p);
		}
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
	 * @return Unmodifiable view of all parameter names mapped to their respective index
	 */
	public Map<String, Integer> parameters(){
		return Collections.unmodifiableMap(nameToIndex);
	}

	/**
	 * @param index Index of the parameter
	 * @return Parameter at specified index
	 * @throws IndexOutOfBoundsException If {@code index < 0 || index >= size()}
	 */
	public MacroParameter paramAt(int index){
		return params.get(index);
	}
	/**
	 * @return Index of the parameter with given name, or {@code -1} if there isn't
	 * @throws NullPointerException If {@code paramName == null}
	 */
	public int indexOf(String paramName){
		Integer i = nameToIndex.get(paramName);
		return i!=null ? i : -1;
	}

	/**
	 * Checks for consecutive optional parameters. This check is only done for list/operation macros.<br>
	 * Being 'consecutive' refers to optional parameters being at the end of the parameter list; see the snippet below.
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
	 * Note that {@link MacroParameterList} does not hold information about underlying macro type; calling this method
	 * for parameter list inside {@link MacroType#OBJECT} macro may still throw exception.
	 *
	 * @throws Sussy If optional parameters are not defined at the end of the list
	 */
	public void checkConsecutiveOptionalParams(){
		boolean defaultParamSeen = false;
		for(MacroParameter p : params){
			if(defaultParamSeen){
				if(p.defaultValue()==null)
					throw new Sussy("Non-object macros should have consecutive optional parameters (i.e. Needs to have all optional parameters at the end of the parameter list, not in between)");
			}else if(p.defaultValue()!=null)
				defaultParamSeen = true;
		}
	}

	private int requiredParameterSize = -1;

	/**
	 * Returns number of required parameters.
	 *
	 * @return Number of required parameters
	 */
	public int requiredParameters(){
		if(requiredParameterSize<0){
			requiredParameterSize = 0;
			for(MacroParameter p : this.params)
				if(p.defaultValue()==null) requiredParameterSize++;
		}
		return requiredParameterSize;
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

	@Override public String toString(){
		return params.stream()
				.map(MacroParameter::toString)
				.collect(Collectors.joining(","));
	}

	@Override public String toPrettyString(int indents, String indent){
		return params.stream()
				.map(p -> p.toPrettyString(indents+1, indent))
				.collect(Collectors.joining(", "));
	}
}
