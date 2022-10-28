package among.obj;

import among.AmongWalker;
import among.NodePath;
import among.ToStringContext;
import among.ToStringOption;
import among.internals.LiteralFormats;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Nameable {@link Among} node with ordered elements. Snippet below shows a list written in Among.
 * <pre>
 * [
 *   "Hello!"
 *   "This is list"
 *   "Each element are divided by either line breaks or ','."
 *   [
 *     "Look, a nested list!"
 *   ]
 * ]
 * </pre>
 * <p>
 * Note that operations get compiled into list; operations do not have separate type representation. Instead, {@link
 * AmongList#operation} flag is used to distinguish operations from lists. This flag is mainly for parsing, however,
 * since the majority of usages do not check the field for type checking.
 */
public class AmongList extends AmongNameable implements Iterable<Among>{
	private final List<Among> values = new ArrayList<>();
	private boolean operation;

	AmongList(){}
	AmongList(@Nullable String name){
		super(name);
	}
	AmongList(@Nullable String name, List<Among> values){
		super(name);
		this.values.addAll(values);
		for(Among a : this.values)
			Objects.requireNonNull(a);
	}

	/**
	 * @return Unmodifiable view of the values
	 */
	public List<Among> values(){
		return Collections.unmodifiableList(values);
	}

	public int size(){
		return values.size();
	}
	public boolean isEmpty(){
		return values.isEmpty();
	}
	public void clear(){
		values.clear();
	}

	public Among get(int index){
		return values.get(index);
	}

	public void set(int index, String value){
		set(index, new AmongPrimitive(value));
	}
	public void set(int index, Among among){
		this.values.set(index, Objects.requireNonNull(among));
	}

	public void add(String value){
		add(new AmongPrimitive(value));
	}
	public void add(Among among){
		this.values.add(Objects.requireNonNull(among));
	}

	public void add(int index, String value){
		add(index, new AmongPrimitive(value));
	}
	public void add(int index, Among among){
		this.values.add(index, Objects.requireNonNull(among));
	}

	public void removeAt(int index){
		this.values.remove(index);
	}

	/**
	 * @return Whether this list is operation or not. This flag has no effect on equality check.
	 */
	public boolean isOperation(){
		return operation;
	}
	/**
	 * Mark this list as an operation or not.
	 *
	 * @param operation Whether this list is operation or not. This flag has no effect on equality check.
	 */
	public void setOperation(boolean operation){
		this.operation = operation;
	}

	/**
	 * @return Iterator for each element on this list. {@link Iterator#remove()} is unsupported.
	 */
	@Override public Iterator<Among> iterator(){
		return Collections.unmodifiableList(values).iterator();
	}

	public Stream<Among> stream(){
		return this.values.stream();
	}

	@Override public AmongList asList(){
		return this;
	}
	@Override public boolean isList(){
		return true;
	}

	@Override public void walk(AmongWalker visitor, NodePath path){
		if(visitor.walkBefore(this, path)){
			for(int i = 0; i<this.values.size(); i++)
				this.values.get(i).walk(visitor, path.subPath(i));
			visitor.walkAfter(this, path);
		}
	}

	@Override public AmongList copy(){
		AmongList l = new AmongList(this.getName());
		l.setSourcePosition(this.sourcePosition());
		l.setOperation(this.isOperation());
		for(Among among : this.values)
			l.add(among.copy());
		return l;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		AmongList l = (AmongList)o;
		return getName().equals(l.getName())&&
				values.equals(l.values);
	}
	@Override public int hashCode(){
		return Objects.hash(getName(), values);
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		nameToString(stb, option, context);
		boolean operation = this.operation&&!option.jsonCompatibility;
		if(isEmpty()) stb.append(operation ? "()" : "[]");
		else{
			stb.append(operation ? '(' : '[');
			for(int i = 0; i<values.size(); i++){
				if(i>0) stb.append(",");
				values.get(i).toString(stb, option, operation ? ToStringContext.OPERATION : ToStringContext.NONE);
			}
			stb.append(operation ? ')' : ']');
		}
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		nameToPrettyString(stb, indents, option, context);
		if(hasName()) stb.append(' ');
		boolean operation = this.operation&&!option.jsonCompatibility;
		if(isEmpty()) stb.append(operation ? "()" : "[]");
		else{
			stb.append(operation ? '(' : '[');
			boolean isCompact = values.size()<=option.compactListSize;
			for(int i = 0; i<values.size(); i++){
				if(!isCompact){
					if(option.jsonCompatibility&&i>0) stb.append(',');
					LiteralFormats.newlineAndIndent(stb, indents+1, option);
				}else if(i>0) stb.append(", ");
				else stb.append(' ');
				values.get(i).toPrettyString(stb, isCompact ? indents : indents+1, option, operation ? ToStringContext.OPERATION : ToStringContext.NONE);
			}
			if(!isCompact) LiteralFormats.newlineAndIndent(stb, indents, option);
			else stb.append(' ');
			stb.append(operation ? ')' : ']');
		}
	}
}
