package ttmp.among.obj;

import org.jetbrains.annotations.Nullable;
import ttmp.among.AmongWalker;
import ttmp.among.NodePath;
import ttmp.among.ToStringContext;
import ttmp.among.ToStringOption;
import ttmp.among.internals.LiteralFormats;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Nameable {@link Among} node with properties. Property is a pair of a name(key) to a child node(values).
 * Snippet below shows an object written in Among.
 * <pre>
 * {
 *   Hello: "Hello!"
 *   ThisIsKey: "This is value"
 *   EachElementAre: "Divided by either line breaks or ','."
 *   NestedObject: {
 *     Look: "A nested list!"
 *   }
 * }
 * </pre>
 */
public class AmongObject extends AmongNameable{
	private final Map<String, Among> properties = new LinkedHashMap<>(); // Use linkedhashmap to preserve insertion order

	AmongObject(){}
	AmongObject(@Nullable String name){
		super(name);
	}
	AmongObject(@Nullable String name, Map<String, Among> map){
		super(name);
		this.properties.putAll(map);
		for(Among among : this.properties.values())
			Objects.requireNonNull(among);
	}

	/**
	 * Set the property with name {@code key} to {@code value}. This is utility method for method chaining.<br>
	 * As this method is supposed to be used primarily for object initialization in code, duplicated properties are not
	 * allowed.
	 *
	 * @param key   Key of the property
	 * @param value Value of the property. If the value is an instance of {@link Among}, it will be directly set as
	 *              property; otherwise it will be converted to its string representation via {@link Object#toString()}
	 *              before being wrapped around {@link AmongPrimitive}.
	 * @return This
	 * @throws IllegalStateException If property for given key was already defined
	 * @throws NullPointerException  If either {@code key == null} or {@code value == null}
	 */
	public AmongObject prop(String key, Object value){
		return prop(key, value instanceof Among ? (Among)value : value(value));
	}
	/**
	 * Set the property with name {@code key} to {@code value}. This is utility method for method chaining.<br>
	 * As this method is supposed to be used primarily for object initialization in code, duplicated properties are not
	 * allowed.
	 *
	 * @param key   Key of the property
	 * @param value Value of the property
	 * @return This
	 * @throws IllegalStateException If property for given key was already defined
	 * @throws NullPointerException  If either {@code key == null} or {@code value == null}
	 */
	public AmongObject prop(String key, Among value){
		if(this.properties.putIfAbsent(key, Objects.requireNonNull(value))!=null)
			throw new IllegalStateException("Property '"+key+"' is already defined");
		return this;
	}

	/**
	 * @return Unmodifiable view of the properties
	 */
	public Map<String, Among> properties(){
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Returns whether this object has property named {@code key}.
	 *
	 * @param key Key of the property
	 * @return Whether this object has property named {@code key}.
	 * @throws NullPointerException If {@code key == null}
	 */
	public boolean hasProperty(String key){
		return this.properties.containsKey(key);
	}

	/**
	 * Return the property for name of {@code key}. Returns {@code null} if there's no property with given name.
	 *
	 * @param key Key of the property
	 * @return Whether this object has property named {@code key}.
	 * @throws NullPointerException If {@code key == null}
	 */
	@Nullable public Among getProperty(String key){
		return this.properties.get(key);
	}

	/**
	 * Set the property with name {@code key} to {@code value}; if there is already a property associated with {@code
	 * key}, it will be overwritten. Providing {@code null} for value will remove the property from this object.
	 *
	 * @param key   Key of the property
	 * @param value Value of the property
	 * @return Previous property value, or {@code null} if there was no property associated to the key
	 * @throws NullPointerException If {@code key == null}
	 */
	@Nullable public Among setProperty(String key, @Nullable Among value){
		if(value==null) return this.properties.remove(key);
		else return this.properties.put(key, value);
	}

	/**
	 * Remove the property with name {@code key}. If there is no property associated with {@code key}, this method does
	 * nothing.
	 *
	 * @param key Key of the property
	 * @return Previous property value, or {@code null} if there was no property associated to the key
	 * @throws NullPointerException If {@code key == null}
	 */
	@Nullable public Among removeProperty(String key){
		return this.properties.remove(key);
	}

	public int size(){
		return properties.size();
	}
	/**
	 * Returns if this object has no property.
	 *
	 * @return Whether this object has no property
	 */
	public boolean isEmpty(){
		return properties.isEmpty();
	}
	/**
	 * Removes all property from this object.
	 */
	public void clear(){
		properties.clear();
	}

	@Override public AmongObject asObj(){
		return this;
	}
	@Override public boolean isObj(){
		return true;
	}

	@Override public void walk(AmongWalker visitor, NodePath path){
		if(visitor.walkBefore(this, path)){
			for(Map.Entry<String, Among> e : this.properties.entrySet())
				e.getValue().walk(visitor, path.subPath(e.getKey()));
			visitor.walkAfter(this, path);
		}
	}

	@Override public AmongObject copy(){
		AmongObject o = new AmongObject(this.getName());
		for(Map.Entry<String, Among> e : this.properties.entrySet())
			o.setProperty(e.getKey(), e.getValue().copy());
		return o;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		AmongObject o2 = (AmongObject)o;
		return getName().equals(o2.getName())&&
				properties.equals(o2.properties);
	}
	@Override public int hashCode(){
		return Objects.hash(getName(), properties);
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		nameToString(stb, option, context);
		if(isEmpty()) stb.append("{}");
		else{
			stb.append('{');
			boolean first = true;
			for(Map.Entry<String, Among> e : properties.entrySet()){
				if(first) first = false;
				else stb.append(',');
				if(!option.jsonCompatibility&&LiteralFormats.isSimpleKey(e.getKey()))
					LiteralFormats.simpleKeyToString(stb, e.getKey(), false);
				else LiteralFormats.primitiveToString(stb, e.getKey());
				stb.append(':');
				e.getValue().toString(stb, option, ToStringContext.NONE);
			}
			stb.append('}');
		}
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		nameToPrettyString(stb, indents, option, context);
		if(hasName()) stb.append(' ');
		if(isEmpty()) stb.append("{}");
		else{
			stb.append('{');
			boolean isCompact = properties.size()<=option.compactObjectSize;
			boolean first = true;
			for(Map.Entry<String, Among> e : properties.entrySet()){
				if(!isCompact){
					if(option.jsonCompatibility){
						if(first) first = false;
						else stb.append(',');
					}
					LiteralFormats.newlineAndIndent(stb, indents+1, option);
				}else if(first){
					first = false;
					stb.append(' ');
				}else stb.append(", ");
				if(!option.jsonCompatibility&&LiteralFormats.isSimpleKey(e.getKey()))
					LiteralFormats.simpleKeyToString(stb, e.getKey(), false);
				else LiteralFormats.primitiveToPrettyString(stb, e.getKey(), isCompact ? indents : indents+1, option);
				e.getValue().toPrettyString(stb.append(": "), isCompact ? indents : indents+1, option, ToStringContext.NONE);
			}
			if(!isCompact) LiteralFormats.newlineAndIndent(stb, indents, option);
			else stb.append(' ');
			stb.append('}');
		}
	}
}
