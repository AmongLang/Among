package ttmp.among;

import ttmp.among.exception.Sussy;
import ttmp.among.obj.Among;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents all objects defined in single source.
 */
public final class AmongRoot extends ToPrettyString.Base{
	private final List<Among> objects;

	/**
	 * Create an empty root.
	 */
	public AmongRoot(){
		this.objects = new ArrayList<>();
	}
	private AmongRoot(AmongRoot copyFrom){
		this.objects = new ArrayList<>(copyFrom.objects);
	}

	public List<Among> objects(){
		return Collections.unmodifiableList(objects);
	}
	public int objectSize(){
		return objects.size();
	}
	public Among getObject(int index){
		return objects.get(index);
	}
	public void addObject(Among among){
		this.objects.add(Objects.requireNonNull(among));
	}
	public Among removeObject(int index){
		return objects.remove(index);
	}
	public boolean isEmpty(){
		return objects.isEmpty();
	}
	public void clear(){
		objects.clear();
	}

	/**
	 * Returns the object defined. Only one object is expected; none or multiple objects will produce exception.
	 *
	 * @return The only object defined
	 * @throws Sussy If there's none or multiple object defined
	 */
	public Among singleObject(){
		switch(objects.size()){
			case 0: throw new Sussy("No objects defined");
			case 1: return objects.get(0);
			default: throw new Sussy("Expected one object");
		}
	}

	/**
	 * Creates a shallow copy of this object. Objects are re-added to the new root without copying.
	 *
	 * @return A shallow copy of this object
	 */
	public AmongRoot copy(){
		return new AmongRoot(this);
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		if(objects.isEmpty()) return;
		for(Among object : objects){
			object.toString(stb, option, ToStringContext.ROOT);
		}
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		boolean first = true;
		for(Among object : objects){
			if(first) first = false;
			else stb.append('\n');
			object.toPrettyString(stb, indents, option, ToStringContext.ROOT);
		}
	}
}
