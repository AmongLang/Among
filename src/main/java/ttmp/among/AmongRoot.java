package ttmp.among;

import ttmp.among.exception.Sussy;
import ttmp.among.obj.Among;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents all values defined in single source.
 */
public final class AmongRoot extends ToPrettyString.Base{
	private final List<Among> values;

	/**
	 * Create an empty root.
	 */
	public AmongRoot(){
		this.values = new ArrayList<>();
	}
	private AmongRoot(AmongRoot copyFrom){
		this.values = new ArrayList<>(copyFrom.values);
	}

	public List<Among> values(){
		return Collections.unmodifiableList(values);
	}
	public int size(){
		return values.size();
	}
	public Among get(int index){
		return values.get(index);
	}
	public void add(Among among){
		this.values.add(Objects.requireNonNull(among));
	}
	public Among remove(int index){
		return values.remove(index);
	}
	public boolean isEmpty(){
		return values.isEmpty();
	}
	public void clear(){
		values.clear();
	}

	/**
	 * Return the value defined. Only one value is expected to be present; none or multiple values will produce
	 * an exception.
	 *
	 * @return The only value defined
	 * @throws Sussy If there's none or multiple value defined
	 */
	public Among single(){
		switch(values.size()){
			case 0: throw new Sussy("No values");
			case 1: return values.get(0);
			default: throw new Sussy(values.size()+" values, expected only one");
		}
	}

	/**
	 * Create a shallow copy of this root. Values are re-added to the new root without copying.
	 *
	 * @return A shallow copy of this root
	 */
	public AmongRoot copy(){
		return new AmongRoot(this);
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		if(values.isEmpty()) return;
		for(Among v : values) v.toString(stb, option, ToStringContext.ROOT);
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		boolean first = true;
		for(Among v : values){
			if(first) first = false;
			else stb.append('\n');
			v.toPrettyString(stb, indents, option, ToStringContext.ROOT);
		}
	}
}
