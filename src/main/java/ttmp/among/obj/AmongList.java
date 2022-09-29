package ttmp.among.obj;

import org.jetbrains.annotations.Nullable;
import ttmp.among.util.AmongUs;
import ttmp.among.util.AmongWalker;
import ttmp.among.util.NodePath;
import ttmp.among.util.PrettyFormatOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
 * Note that <a href="https://youtu.be/doEqUhFiQS4">operations</a> get compiled into list; operations do not
 * have type representation.
 */
public class AmongList extends AmongNamed implements Iterable<Among>{
	private final List<Among> values = new ArrayList<>();

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
	 * @return Iterator for each element on this list. {@link Iterator#remove()} is unsupported.
	 */
	@Override public Iterator<Among> iterator(){
		return Collections.unmodifiableList(values).iterator();
	}

	@Override public AmongList asList(){
		return this;
	}
	@Override public boolean isList(){
		return true;
	}

	@Override public void walk(AmongWalker visitor, NodePath path){
		if(visitor.walk(this, path))
			for(int i = 0; i<this.values.size(); i++)
				this.values.get(i).walk(visitor, path.subPath(i));
	}

	@Override public AmongList copy(){
		AmongList l = new AmongList(this.getName());
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

	@Override public String toString(){
		StringBuilder stb = new StringBuilder();
		if(hasName()) AmongUs.nameToString(stb, getName(), isParamRef());
		if(isEmpty()) stb.append("[]");
		else{
			stb.append('[');
			boolean first = true;
			for(Among among : values){
				if(first) first = false;
				else stb.append(',');
				AmongUs.valueToString(stb, among);
			}
			stb.append(']');
		}
		return stb.toString();
	}

	@Override public String toPrettyString(int indents, PrettyFormatOption option){
		StringBuilder stb = new StringBuilder();
		if(hasName()){
			AmongUs.nameToPrettyString(stb, getName(), isParamRef(), indents+1, option);
			stb.append(' ');
		}
		if(isEmpty()) stb.append("[]");
		else{
			stb.append('[');
			boolean isCompact = values.size()<=option.compactListSize;
			for(int j = 0; j<values.size(); j++){
				if(!isCompact) AmongUs.newlineAndIndent(stb, indents+1, option);
				else if(j>0) stb.append(", ");
				else stb.append(' ');
				AmongUs.valueToPrettyString(stb, values.get(j), isCompact ? indents : indents+1, option);
			}
			if(!isCompact) AmongUs.newlineAndIndent(stb, indents, option);
			else stb.append(' ');
			stb.append(']');
		}
		return stb.toString();
	}
}
