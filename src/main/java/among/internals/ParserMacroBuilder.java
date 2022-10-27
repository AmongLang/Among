package among.internals;

import among.AmongWalker;
import among.NodePath;
import among.TypeFlags;
import among.macro.Macro;
import among.macro.MacroDefinition;
import among.macro.MacroParameter;
import among.macro.MacroParameterList;
import among.macro.MacroReplacement;
import among.macro.MacroSignature;
import among.macro.MacroType;
import among.obj.Among;
import among.obj.AmongList;
import among.obj.AmongObject;
import among.obj.AmongPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ParserMacroBuilder{
	private final Parser parser;
	private final int start;
	private final String name;
	private final MacroType type;
	private final List<MacroParameter> params = new ArrayList<>();
	private final List<Map.Entry<MacroReplacement.MacroOp, Among>> operationToTarget = new ArrayList<>();

	@Nullable private List<ParserTypeInference> typeInferences = null;

	private boolean optionalParamSeen;
	private boolean invalid;

	ParserMacroBuilder(Parser parser, int start, String name, MacroType type){
		this.parser = parser;
		this.start = start;
		this.name = name;
		this.type = type;
		if(type.isFunctionMacro())
			params.add(new MacroParameter("self", null));
	}

	public void markInvalid(){
		this.invalid = true;
	}

	public void newParam(String name, @Nullable Among defaultValue, int pos){
		if(type==MacroType.CONST){
			parser.reportError("Constant macros cannot have parameters");
			markInvalid();
		}else if(type==MacroType.ACCESS){
			parser.reportError("Access macros cannot have parameters");
			markInvalid();
		}else if(paramIndex(name)>=0){
			parser.reportError(type.isFunctionMacro()&&"self".equals(name) ?
					"Cannot define parameter named 'self' in function macros" :
					"Duplicated parameter '"+name+"'.", pos);
			markInvalid();
		}else{
			if(type==MacroType.LIST||type==MacroType.OPERATION||
					type==MacroType.LIST_FN||type==MacroType.OPERATION_FN){
				if(defaultValue==null){
					if(optionalParamSeen){
						parser.reportError("Optional parameters of "+type.friendlyName()+
								" macro should be consecutive, placed at end of the parameter list", pos);
						markInvalid();
					}
				}else optionalParamSeen = true;
			}
			params.add(new MacroParameter(name, defaultValue));
		}
	}

	public int paramIndex(String name){
		for(int i = 0; i<params.size(); i++){
			MacroParameter p = params.get(i);
			if(p.name().equals(name)) return i;
		}
		return -1;
	}

	private ParserTypeInference getTypeInference(int paramIndex){
		if(typeInferences==null)
			typeInferences = new ArrayList<>();
		else for(ParserTypeInference i : typeInferences){
			if(i.index==paramIndex) return i;
		}
		ParserTypeInference i = new ParserTypeInference(paramIndex);
		typeInferences.add(i);
		return i;
	}

	public void inferTypeAs(int paramIndex, byte typeInference){
		ParserTypeInference i = getTypeInference(paramIndex);
		if(i.type==0) return; // already reported
		MacroParameter p = params.get(paramIndex);

		byte newInference = (byte)(typeInference&i.type);
		if(i.type!=typeInference){
			if(newInference==0){
				parser.reportWarning("Parameter '"+p.name()+"' has no valid input: needs to satisfy both "+
						TypeFlags.toString(i.type)+" AND "+TypeFlags.toString(typeInference));
			}else if(p.defaultValue()!=null&&!TypeFlags.matches(newInference, p.defaultValue())){
				parser.reportWarning("Default value of the parameter '"+p.name()+"' is invalid");
			}
			i.type = newInference;
		}
	}

	public boolean resolveParamRef(Among target){
		String name;
		if(target.isPrimitive()) name = target.asPrimitive().getValue();
		else if(target.isNameable()) name = target.asNameable().getName();
		else return false;
		int i = paramIndex(name);
		if(i<0) return false;
		if(target.isNameable()) inferTypeAs(i, TypeFlags.PRIMITIVE);
		if(!invalid)
			operationToTarget.add(new AbstractMap.SimpleEntry<>(
					target.isPrimitive() ?
							new MacroReplacement.MacroOp.ValueReplacement(i) :
							new MacroReplacement.MacroOp.NameReplacement(i),
					target));
		return true;
	}

	public void resolveMacroCall(Macro macro, Among target){
		operationToTarget.add(new AbstractMap.SimpleEntry<>(new MacroReplacement.MacroOp.MacroCall(macro), target));
	}

	public void register(Among expr){
		if(invalid) return;
		markInvalid();
		List<MacroReplacement> replacements = new ArrayList<>(this.operationToTarget.size());
		expr.walk(new AmongWalker(){
			@Override public void walk(AmongPrimitive primitive, NodePath path){
				resolve(primitive, path);
			}
			@Override public void walkAfter(AmongObject object, NodePath path){
				resolve(object, path);
			}
			@Override public void walkAfter(AmongList list, NodePath path){
				resolve(list, path);
			}
			private void resolve(Among target, NodePath path){
				for(Iterator<Map.Entry<MacroReplacement.MacroOp, Among>> it = operationToTarget.iterator(); it.hasNext(); ){
					Map.Entry<MacroReplacement.MacroOp, Among> e = it.next();
					if(e.getValue()==target){
						replacements.add(new MacroReplacement(path, e.getKey()));
						it.remove();
						return;
					}
				}
			}
		});
		if(!operationToTarget.isEmpty()){
			parser.reportError("Unresolved macro operation: "+
					operationToTarget.stream().map(e -> e.getKey().toString()).collect(Collectors.joining(", ")), start);
			return;
		}
		byte[] typeInferences;
		if(this.typeInferences!=null){
			typeInferences = new byte[params.size()];
			Arrays.fill(typeInferences, TypeFlags.ANY);
			for(ParserTypeInference i : this.typeInferences){
				typeInferences[i.index] = i.type;
			}
		}else typeInferences = null;
		MacroDefinition macro = new MacroDefinition(new MacroSignature(name, type),
				type.isFunctionMacro() ?
						MacroParameterList.of(params.subList(1, params.size())) :
						MacroParameterList.of(params),
				expr, replacements, typeInferences);
		parser.importDefinition().macros().add(macro, parser.reportAt(start));
		parser.definition().macros().add(macro);
	}
}
