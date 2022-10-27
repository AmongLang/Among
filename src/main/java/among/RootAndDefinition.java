package among;

import among.internals.LiteralFormats;

/**
 * A pair of {@link AmongRoot} and {@link AmongDefinition}.
 */
public final class RootAndDefinition extends ToPrettyString.Base{
	private final AmongRoot root;
	private final AmongDefinition definition;

	public RootAndDefinition(){
		this(new AmongRoot(), new AmongDefinition());
	}
	public RootAndDefinition(AmongRoot root){
		this(root, new AmongDefinition());
	}
	public RootAndDefinition(AmongDefinition definition){
		this(new AmongRoot(), definition);
	}
	public RootAndDefinition(AmongRoot root, AmongDefinition definition){
		this.root = root;
		this.definition = definition;
	}

	public AmongRoot root(){
		return root;
	}
	public AmongDefinition definition(){
		return definition;
	}

	/**
	 * Create a copy of this object. Both root and definitions will be shallow copied.
	 *
	 * @return A copy of this object
	 */
	public RootAndDefinition copy(){
		return new RootAndDefinition(root.copy(), definition.copy());
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		if(definition.isEmpty()){
			if(!root.isEmpty()) root.toString(stb, option, ToStringContext.NONE);
		}else{
			definition.toString(stb, option, ToStringContext.NONE);
			if(!root.isEmpty()) root.toString(stb.append(','), option, ToStringContext.NONE);
		}
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		if(definition.isEmpty()){
			if(!root.isEmpty()) root.toPrettyString(stb, indents, option, ToStringContext.NONE);
		}else{
			definition.toPrettyString(stb, indents, option, ToStringContext.NONE);
			if(!root.isEmpty()){
				LiteralFormats.newlineAndIndent(stb, indents, option);
				root.toPrettyString(stb, indents, option, ToStringContext.NONE);
			}
		}
	}
}
