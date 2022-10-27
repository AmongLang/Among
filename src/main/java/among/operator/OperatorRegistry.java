package among.operator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static among.operator.OperatorType.BINARY;
import static among.operator.OperatorType.POSTFIX;

/**
 * Registry for {@link OperatorDefinition}s. Provides interface for registering/de-registering operator/keywords, as
 * well as ability to enumerate defined operator/keywords.<br>
 * Operators and keywords are grouped by name(used during tokenization) or by priority(used during parsing).
 */
public final class OperatorRegistry{
	private final Map<String, NameGroup> operators = new HashMap<>();
	private final Map<Double, PriorityGroup> priorityGroup = new HashMap<>();
	@Nullable private List<PriorityGroup> priorityGroupList;

	/**
	 * Starting codepoint to set of name groups, sorted by name length in descending order (for maximal munch rule)
	 */
	private final Map<Integer, Set<NameGroup>> operatorByStartingCodepoint = new HashMap<>();
	/**
	 * Starting codepoint to set of name groups, sorted by name length in descending order (for maximal munch rule)
	 */
	private final Map<Integer, Set<NameGroup>> keywordByStartingCodepoint = new HashMap<>();

	public OperatorRegistry(){}
	public OperatorRegistry(OperatorRegistry copyFrom){
		copyFrom.allOperators().forEach(this::add);
		this.priorityGroupList = null;
	}

	public Collection<NameGroup> getAllNameGroups(){
		return Collections.unmodifiableCollection(operators.values());
	}

	public RegistrationResult addOperator(String name, OperatorType type){
		return addOperator(name, type, Double.NaN);
	}
	public RegistrationResult addOperator(String name, OperatorType type, double priority){
		return add(new OperatorDefinition(name, false, type, priority));
	}
	public RegistrationResult addOperator(String name, OperatorType type, @Nullable String alias){
		return addOperator(name, type, alias, Double.NaN);
	}
	public RegistrationResult addOperator(String name, OperatorType type, @Nullable String alias, double priority){
		return add(new OperatorDefinition(name, false, type, alias, OperatorProperty.NONE, priority));
	}
	public RegistrationResult addOperator(String name, OperatorType type, byte properties){
		return addOperator(name, type, properties, Double.NaN);
	}
	public RegistrationResult addOperator(String name, OperatorType type, byte properties, double priority){
		return add(new OperatorDefinition(name, false, type, null, properties, priority));
	}
	public RegistrationResult addOperator(String name, OperatorType type, @Nullable String alias, byte properties){
		return addOperator(name, type, alias, properties, Double.NaN);
	}
	public RegistrationResult addOperator(String name, OperatorType type, @Nullable String alias, byte properties, double priority){
		return add(new OperatorDefinition(name, false, type, alias, properties, priority));
	}

	public RegistrationResult addKeyword(String name, OperatorType type){
		return addKeyword(name, type, Double.NaN);
	}
	public RegistrationResult addKeyword(String name, OperatorType type, double priority){
		return add(new OperatorDefinition(name, false, type, priority));
	}
	public RegistrationResult addKeyword(String name, OperatorType type, @Nullable String alias){
		return addKeyword(name, type, alias, Double.NaN);
	}
	public RegistrationResult addKeyword(String name, OperatorType type, @Nullable String alias, double priority){
		return add(new OperatorDefinition(name, false, type, alias, OperatorProperty.NONE, priority));
	}
	public RegistrationResult addKeyword(String name, OperatorType type, byte properties){
		return addKeyword(name, type, properties, Double.NaN);
	}
	public RegistrationResult addKeyword(String name, OperatorType type, byte properties, double priority){
		return add(new OperatorDefinition(name, false, type, null, properties, priority));
	}
	public RegistrationResult addKeyword(String name, OperatorType type, @Nullable String alias, byte properties){
		return addKeyword(name, type, alias, properties, Double.NaN);
	}
	public RegistrationResult addKeyword(String name, OperatorType type, @Nullable String alias, byte properties, double priority){
		return add(new OperatorDefinition(name, false, type, alias, properties, priority));
	}

	public RegistrationResult add(OperatorDefinition definition){
		if(isPriorityOccupiedByWrongType(definition))
			return RegistrationResult.PRIORITY_OCCUPIED_BY_DIFFERENT_TYPE;
		if(hasDifferentAssociativity(definition))
			return RegistrationResult.MIXED_ASSOCIATIVITY;
		NameGroup o = operators.get(definition.name());
		if(o==null){
			operators.put(definition.name(), o = new NameGroup(definition));
			if(!definition.name().isEmpty())
				(definition.isKeyword() ?
						keywordByStartingCodepoint :
						operatorByStartingCodepoint)
						.computeIfAbsent(o.codePointAt(0), i -> new TreeSet<>((o1, o2) -> {
							int c = Integer.compare(o2.name.length(), o1.name.length());
							if(c!=0) return c;
							c = o1.name.compareTo(o2.name);
							return c;
						}))
						.add(o);
		}else{
			RegistrationResult result = o.add(definition);
			if(!result.isSuccess()) return result;
		}
		addToPriorityGroup(definition);
		return RegistrationResult.OK;
	}

	public void remove(String operatorName, boolean keyword){
		NameGroup op = operators.get(operatorName);
		if(op!=null&&op.isKeyword==keyword){
			operators.remove(operatorName);
			if(!operatorName.isEmpty()){
				Map<Integer, Set<NameGroup>> m = keyword ?
						keywordByStartingCodepoint :
						operatorByStartingCodepoint;
				Set<NameGroup> operators = m.get(op.codePointAt(0));
				operators.remove(op);
				if(operators.isEmpty()) m.remove(op.codePointAt(0));
			}
			for(OperatorDefinition def : op.defByType.values()){
				removeFromParsingOrder(def);
			}
		}
	}

	public boolean isEmpty(){
		return operators.isEmpty();
	}
	public void clear(){
		operators.clear();
		priorityGroup.clear();
		priorityGroupList = null;
	}

	private boolean isPriorityOccupiedByWrongType(OperatorDefinition definition){
		PriorityGroup g = this.priorityGroup.get(definition.priority());
		return g!=null&&g.type()!=definition.type();
	}
	private boolean hasDifferentAssociativity(OperatorDefinition definition){
		if(definition.type()!=BINARY) return false;
		PriorityGroup g = this.priorityGroup.get(definition.priority());
		return g!=null&&g.rightAssociative!=definition.hasProperty(OperatorProperty.RIGHT_ASSOCIATIVE);
	}
	private void addToPriorityGroup(OperatorDefinition definition){
		PriorityGroup g = this.priorityGroup.get(definition.priority());
		if(g==null) this.priorityGroup.put(definition.priority(), new PriorityGroup(definition));
		else g.add(definition);
		priorityGroupList = null;
	}

	private void removeFromParsingOrder(OperatorDefinition def){
		PriorityGroup g = this.priorityGroup.get(def.priority());
		if(g!=null){
			g.remove(def);
			if(g.isEmpty()){
				this.priorityGroup.remove(def.priority());
				this.priorityGroupList = null;
			}
		}
	}

	public Set<NameGroup> getOperators(int startingCodePoint){
		Set<NameGroup> operators = operatorByStartingCodepoint.get(startingCodePoint);
		return operators==null ? Collections.emptySet() : Collections.unmodifiableSet(operators);
	}

	public Set<NameGroup> getKeywords(int startingCodePoint){
		Set<NameGroup> keywords = keywordByStartingCodepoint.get(startingCodePoint);
		return keywords==null ? Collections.emptySet() : Collections.unmodifiableSet(keywords);
	}

	public List<PriorityGroup> priorityGroup(){
		if(priorityGroupList==null){
			List<PriorityGroup> groups = new ArrayList<>(this.priorityGroup.values());
			groups.sort(null);
			priorityGroupList = Collections.unmodifiableList(groups);
		}
		return priorityGroupList;
	}

	/**
	 * @return Stream of all operators and keywords in registry.
	 */
	public Stream<OperatorDefinition> allOperators(){
		return operators.values().stream().flatMap(g -> g.defByType.values().stream());
	}

	public Stream<NameGroup> allOperatorNames(){
		return operators.values().stream();
	}

	@Override public boolean equals(Object obj){
		if(this==obj) return true;
		if(!(obj instanceof OperatorRegistry)) return false;
		OperatorRegistry reg = (OperatorRegistry)obj;
		return this.operators.equals(reg.operators);
	}

	/**
	 * Result of {@link OperatorRegistry#add(OperatorDefinition)}. Values other than {@link RegistrationResult#OK}
	 * represents different reasons for failure.
	 */
	public enum RegistrationResult{
		/**
		 * Success.
		 */
		OK,
		/**
		 * Defined as both operator and keyword.
		 */
		BOTH_OPERATOR_AND_KEYWORD,
		/**
		 * Defined twice as same type.
		 */
		DUPLICATE,
		/**
		 * Defined twice as same type. Both are identical.
		 */
		IDENTICAL_DUPLICATE,
		/**
		 * Defined as both binary and postfix, which is incompatible.<br>
		 * Being them together should not produce weird crashes or anything, it just does not behave in the way average
		 * user would expect. This restriction is in place to prevent such scenario from get-go.
		 */
		BOTH_BINARY_AND_POSTFIX,
		/**
		 * Defined with priority value that is already being in use with different type.
		 */
		PRIORITY_OCCUPIED_BY_DIFFERENT_TYPE,
		/**
		 * Both left-associative and right-associative operators are present in the same priority group.
		 */
		MIXED_ASSOCIATIVITY;

		public boolean isSuccess(){
			return this==OK;
		}

		public String message(OperatorDefinition def){
			switch(this){
				case BOTH_OPERATOR_AND_KEYWORD: return "Word '"+def.name()+"' is defined as both operator and keyword";
				case DUPLICATE:
				case IDENTICAL_DUPLICATE: return (def.isKeyword() ? "Keyword '" : "Operator '")+def.name()+"' is defined twice";
				case BOTH_BINARY_AND_POSTFIX: return (def.isKeyword() ? "Keyword '" : "Operator '")+def.name()+"' is both defined as binary and postfix";
				case PRIORITY_OCCUPIED_BY_DIFFERENT_TYPE: return "Priority "+def.priority()+" is already in use with different type";
				case MIXED_ASSOCIATIVITY: return "Both left-associative and right-associative operators are present in the same priority group "+def.priority()+".";
				default: return "";
			}
		}
	}

	/**
	 * Operators grouped by their name. Used in tokenization.
	 */
	public static final class NameGroup{
		private final String name;
		private final int[] codePoints;
		private final boolean isKeyword;
		private final EnumMap<OperatorType, OperatorDefinition> defByType = new EnumMap<>(OperatorType.class);

		public NameGroup(OperatorDefinition initial){
			this.name = initial.name();
			this.isKeyword = initial.isKeyword();
			this.codePoints = name.codePoints().toArray();
			add(initial);
		}

		public String name(){
			return name;
		}
		public boolean isKeyword(){
			return isKeyword;
		}

		public int codePointLength(){
			return codePoints.length;
		}
		public int codePointAt(int index){
			return codePoints[index];
		}

		public Collection<OperatorDefinition> definitions(){
			return Collections.unmodifiableCollection(defByType.values());
		}

		private RegistrationResult add(OperatorDefinition def){
			if(this.isKeyword!=def.isKeyword())
				return RegistrationResult.BOTH_OPERATOR_AND_KEYWORD;
			OperatorDefinition another = defByType.get(def.type());
			if(another!=null)
				return def.equals(another) ? RegistrationResult.IDENTICAL_DUPLICATE : RegistrationResult.DUPLICATE;
			if(def.type()==BINARY||def.type()==POSTFIX){
				if(defByType.containsKey(def.type()==BINARY ? POSTFIX : BINARY))
					return RegistrationResult.BOTH_BINARY_AND_POSTFIX;
			}
			defByType.put(def.type(), def);
			return RegistrationResult.OK;
		}

		@Override public boolean equals(Object o){
			if(this==o) return true;
			if(o==null||getClass()!=o.getClass()) return false;
			NameGroup nameGroup = (NameGroup)o;
			return isKeyword==nameGroup.isKeyword&&name.equals(nameGroup.name)&&defByType.equals(nameGroup.defByType);
		}
		@Override public int hashCode(){
			return Objects.hash(name, isKeyword, defByType);
		}

		@Override public String toString(){
			return name;
		}
	}

	/**
	 * Operators grouped by their priority. Used in parsing.
	 */
	public static final class PriorityGroup implements Comparable<PriorityGroup>{
		private final double priority;
		private final OperatorType type;
		private final boolean rightAssociative;
		private final Map<String, OperatorDefinition> operators = new HashMap<>();

		public PriorityGroup(OperatorDefinition initial){
			this.priority = initial.priority();
			this.type = initial.type();
			this.rightAssociative = type==BINARY&&initial.hasProperty(OperatorProperty.RIGHT_ASSOCIATIVE);
			add(initial);
		}

		private void add(OperatorDefinition def){
			if(Double.compare(this.priority, def.priority())!=0)
				throw new IllegalStateException("Trying to register operator with wrong priority to group");
			if(this.type!=def.type())
				throw new IllegalStateException("Trying to register operator with wrong type to group");
			if(this.type==BINARY&&this.rightAssociative!=def.hasProperty(OperatorProperty.RIGHT_ASSOCIATIVE))
				throw new IllegalStateException("Trying to register binary operator with different associativity to group");
			if(operators.putIfAbsent(def.name(), def)!=null)
				throw new IllegalStateException("Duplicated registration of operator '"+def.name()+"'");
		}

		private void remove(OperatorDefinition def){
			if(Double.compare(this.priority, def.priority())==0&&this.type==def.type())
				operators.remove(def.name());
		}

		public double priority(){
			return priority;
		}
		public OperatorType type(){
			return type;
		}
		public boolean isRightAssociative(){
			return rightAssociative;
		}

		public boolean isEmpty(){
			return operators.isEmpty();
		}

		@Nullable public OperatorDefinition get(String name){
			return operators.get(name);
		}

		@Override public int compareTo(@NotNull OperatorRegistry.PriorityGroup o){
			return Double.compare(priority, o.priority);
		}

		@Override public boolean equals(Object o){
			if(this==o) return true;
			if(o==null||getClass()!=o.getClass()) return false;
			PriorityGroup that = (PriorityGroup)o;
			return Double.compare(that.priority, priority)==0;
		}
		@Override public int hashCode(){
			return Objects.hash(priority);
		}

		@Override public String toString(){
			return priority+":"+type;
		}
	}
}
