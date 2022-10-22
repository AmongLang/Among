package ttmp.among.definition;

import ttmp.among.obj.Among;
import ttmp.among.obj.AmongNamed;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags indicating type of the object.
 */
public interface TypeInference{
	byte PRIMITIVE = 1;
	byte UNNAMED_OBJECT = 2;
	byte UNNAMED_LIST = 4;
	byte UNNAMED_OPERATION = 8;
	byte NAMED_OBJECT = 16;
	byte NAMED_LIST = 32;
	byte NAMED_OPERATION = 64;

	byte OBJECT = UNNAMED_OBJECT|NAMED_OBJECT;
	byte LIST = UNNAMED_LIST|NAMED_LIST;
	byte OPERATION = UNNAMED_OPERATION|NAMED_OPERATION;

	byte UNNAMED = UNNAMED_OBJECT|UNNAMED_LIST|UNNAMED_OPERATION;
	byte NAMED = NAMED_OBJECT|NAMED_LIST|NAMED_OPERATION;

	byte COLLECTION = UNNAMED|NAMED;

	byte ANY = PRIMITIVE|COLLECTION;

	static boolean matches(byte typeInference, Among value){
		if(typeInference==ANY) return true;
		byte flag;
		if(value.isPrimitive()) flag = PRIMITIVE;
		else{
			AmongNamed named = value.asNamed();
			if(named.isObj()) flag = named.hasName() ? NAMED_OBJECT : UNNAMED_OBJECT;
			else if(named.asList().isOperation()) flag = named.hasName() ? NAMED_OPERATION : UNNAMED_OPERATION;
			else flag = named.hasName() ? NAMED_LIST : UNNAMED_LIST;
		}
		return (typeInference&flag)!=0;
	}

	static String toString(byte flag){
		switch(flag&ANY){
			case ANY: return "Anything";
			case PRIMITIVE: return "Primitive";
			case UNNAMED_OBJECT: return "Unnamed Object";
			case UNNAMED_LIST: return "Unnamed List";
			case UNNAMED_OPERATION: return "Unnamed Operation";
			case NAMED_OBJECT: return "Named Object";
			case NAMED_LIST: return "Named List";
			case NAMED_OPERATION: return "Named Operation";
			case OBJECT: return "Object";
			case LIST: return "List";
			case OPERATION: return "Operation";
			case UNNAMED: return "Unnamed Collection";
			case NAMED: return "Named Collection";
			case COLLECTION: return "Collection";
			default:{
				List<String> l = new ArrayList<>();
				if(has(flag, PRIMITIVE)) l.add("Primitive");

				if(has(flag, COLLECTION)) l.add("Collection");
				else{
					if(has(flag, NAMED)){
						l.add("Named Collection");
						flag ^= NAMED;
					}else if(has(flag, UNNAMED)){
						l.add("Unnamed Collection");
						flag ^= UNNAMED;
					}

					if(has(flag, OBJECT)) l.add("Object");
					else if(has(flag, UNNAMED_OBJECT)) l.add("Unnamed Object");
					else if(has(flag, NAMED_OBJECT)) l.add("Named Object");
					if(has(flag, LIST)) l.add("List");
					else if(has(flag, UNNAMED_LIST)) l.add("Unnamed List");
					else if(has(flag, NAMED_LIST)) l.add("Named List");
					if(has(flag, OPERATION)) l.add("Operation");
					else if(has(flag, UNNAMED_OPERATION)) l.add("Unnamed Operation");
					else if(has(flag, NAMED_OPERATION)) l.add("Named Operation");
				}
				switch(l.size()){
					case 0: return "Invalid";
					case 1: return l.get(0);
					default: return String.join(" or ", l);
				}
			}
		}
	}

	static boolean has(byte flag, byte value){
		return (flag&value)==value;
	}
}
