package among.macro;

import among.ReportType;
import among.ToStringContext;
import among.ToStringOption;
import among.obj.Among;
import among.obj.AmongList;
import among.obj.AmongObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MacroRegistry{
	private final Map<MacroSignature, Group> groups = new HashMap<>();

	public MacroRegistry(){}
	public MacroRegistry(MacroRegistry macros){
		groups.putAll(macros.groups);
	}

	public boolean isEmpty(){
		return groups.isEmpty();
	}
	public void clear(){
		groups.clear();
	}

	/**
	 * Adds the macro to this registry. This method always succeeds; it always adds the macro by one way
	 * or another. If there is another macro with same signature and it is considered 'identical' parameter,
	 * the preexisting one is overwritten.
	 *
	 * @param macro Macro to be registered
	 * @throws NullPointerException If {@code macro == null}
	 * @see MacroRegistry#add(Macro, BiConsumer)
	 */
	public void add(Macro macro){
		add(macro, null);
	}
	public void add(Macro macro, @Nullable BiConsumer<ReportType, String> reportHandler){
		Group g = groups.computeIfAbsent(macro.signature(), s -> {
			switch(s.type()){
				case CONST: case ACCESS: return new ConstGroup();
				case OBJECT: return new ObjectGroup();
				case LIST: case OPERATION: return new ListGroup();
				case OBJECT_FN: return new ObjectFunctionGroup();
				case LIST_FN: case OPERATION_FN: return new ListFunctionGroup();
				default: throw new IllegalStateException("Unreachable");
			}
		});
		g.add(macro, reportHandler);
	}

	public void remove(String name, MacroType type){
		remove(new MacroSignature(name, type));
	}
	public void remove(MacroSignature signature){
		groups.remove(signature);
	}

	@Nullable public Group groupFor(Among argument){
		return groupFor(MacroSignature.of(argument));
	}
	@Nullable public Group groupFor(String name, MacroType type){
		return groupFor(new MacroSignature(name, type));
	}
	@Nullable public Group groupFor(MacroSignature signature){
		return groups.get(signature);
	}

	public Stream<Macro> allMacros(){
		return groups.values().stream().flatMap(Group::macros);
	}
	public Stream<MacroSignature> allMacroSignatures(){
		return groups.keySet().stream();
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		MacroRegistry that = (MacroRegistry)o;
		return groups.equals(that.groups);
	}
	@Override public int hashCode(){
		return Objects.hash(groups);
	}

	public static abstract class Group{
		@Nullable public abstract Macro search(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler);
		protected abstract void add(Macro macro, @Nullable BiConsumer<ReportType, String> reportHandler);
		protected abstract Stream<Macro> macros();
	}

	public static final class ConstGroup extends Group{
		@Nullable private Macro macro;

		@Override @Nullable public Macro search(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler){
			return macro;
		}
		@Override protected void add(Macro macro, @Nullable BiConsumer<ReportType, String> reportHandler){
			if(this.macro!=null) reportOverwrite(reportHandler, macro, Collections.singletonList(this.macro));
			this.macro = macro;
		}
		@Override protected Stream<Macro> macros(){
			return Stream.of(macro);
		}

		@Override public boolean equals(Object o){
			if(this==o) return true;
			if(o==null||getClass()!=o.getClass()) return false;
			ConstGroup that = (ConstGroup)o;
			return Objects.equals(macro, that.macro);
		}
		@Override public int hashCode(){
			return Objects.hash(macro);
		}
	}

	public static abstract class MatchBasedGroup<T extends Among> extends Group{
		private final List<Macro> macros = new ArrayList<>();

		@Nullable protected Macro searchInternal(T argument, @Nullable BiConsumer<ReportType, String> reportHandler){
			@Nullable Macro matched = null;
			int overmatched = 0;
			@Nullable List<Macro> ambiguousMacros = null;
			for(Macro m : macros){
				int match = match(m, argument);
				if(match<0) continue;
				if(matched==null){
					matched = m;
					overmatched = match;
				}else if(overmatched>match){
					matched = m;
					overmatched = match;
					if(ambiguousMacros!=null) ambiguousMacros.clear();
				}else if(overmatched==match){
					if(ambiguousMacros==null){
						ambiguousMacros = new ArrayList<>();
						ambiguousMacros.add(matched);
					}
					ambiguousMacros.add(m);
				}
			}
			if(ambiguousMacros!=null&&!ambiguousMacros.isEmpty()){
				reportAmbiguousUsage(reportHandler, ambiguousMacros.get(0).signature(), ambiguousMacros);
				return null;
			}
			if(matched==null) reportNoMatch(reportHandler, macros);
			return matched;
		}
		@Override protected void add(Macro macro, @Nullable BiConsumer<ReportType, String> reportHandler){
			List<Macro> overwritten = null, overlapping = null;
			for(int i = 0; i<macros.size(); ){
				Macro m = macros.get(i);
				if(overwrites(macro, m)){
					if(overwritten==null) overwritten = new ArrayList<>();
					overwritten.add(m);
					macros.remove(i);
					continue;
				}else if(isParameterOverlaps(macro, m)){
					if(overlapping==null) overlapping = new ArrayList<>();
					overlapping.add(m);
				}
				i++;
			}
			if(overwritten!=null) reportOverwrite(reportHandler, macro, overwritten);
			if(overlapping!=null) reportOverlap(reportHandler, macro, overlapping);
			macros.add(macro);
		}
		@Override protected Stream<Macro> macros(){
			return macros.stream();
		}

		/**
		 * @param macro Macro to be checked
		 * @param args  An argument value
		 * @return {@code -1} if doesn't match, {@code 0} if matches perfectly, positive number {@code n} if {@code n}
		 * arguments are oversupplied
		 */
		protected abstract int match(Macro macro, T args);
		/**
		 * @param newMacro      Newly defined macro
		 * @param existingMacro Preexisting macro
		 * @return Whether {@code newMacro} should overwrite {@code existingMacro}
		 */
		protected abstract boolean overwrites(Macro newMacro, Macro existingMacro);
		/**
		 * @param newMacro      Newly defined macro
		 * @param existingMacro Preexisting macro
		 * @return Whether either macro's parameter overlaps, either partially or completely
		 */
		protected abstract boolean isParameterOverlaps(Macro newMacro, Macro existingMacro);

		@Override public boolean equals(Object o){
			if(this==o) return true;
			if(o==null||getClass()!=o.getClass()) return false;
			MatchBasedGroup<?> that = (MatchBasedGroup<?>)o;
			return macros.equals(that.macros);
		}
		@Override public int hashCode(){
			return Objects.hash(macros);
		}
	}

	public static class ListGroup extends MatchBasedGroup<AmongList>{
		@Override @Nullable public Macro search(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler){
			return searchInternal(argument.asList(), reportHandler);
		}
		@Override protected int match(Macro macro, AmongList args){
			if(args.size()<macro.parameter().requiredParameterSize()) return -1;
			return Math.max(0, args.size()-macro.parameter().size());
		}
		@Override protected boolean overwrites(Macro newMacro, Macro existingMacro){
			return newMacro.parameter().size()==existingMacro.parameter().size()&&
					newMacro.parameter().requiredParameterSize()==existingMacro.parameter().requiredParameterSize();
		}
		@Override protected boolean isParameterOverlaps(Macro newMacro, Macro existingMacro){
			int aMin = newMacro.parameter().requiredParameterSize(), aMax = newMacro.parameter().size();
			int bMin = existingMacro.parameter().requiredParameterSize(), bMax = existingMacro.parameter().size();
			return aMax>=bMin&&bMax>=aMin;
		}
	}

	public static class ObjectGroup extends MatchBasedGroup<AmongObject>{
		@Override @Nullable public Macro search(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler){
			return searchInternal(argument.asObj(), reportHandler);
		}
		@Override protected int match(Macro macro, AmongObject args){
			if(args.size()<macro.parameter().requiredParameterSize()) return -1;
			int defaultArgsProvided = 0;
			for(int i = 0; i<macro.parameter().size(); i++){
				MacroParameter p = macro.parameter().paramAt(i);
				if(p.defaultValue()==null){
					if(!args.hasProperty(p.name())) return -1;
				}else if(args.hasProperty(p.name())) defaultArgsProvided++;
			}
			return Math.max(0, args.size()-macro.parameter().requiredParameterSize()-defaultArgsProvided);
		}
		@Override protected boolean overwrites(Macro newMacro, Macro existingMacro){
			return newMacro.parameter().parameters().equals(existingMacro.parameter().parameters());
		}
		@Override protected boolean isParameterOverlaps(Macro newMacro, Macro existingMacro){
			Set<String> s1 = newMacro.parameter().requiredParameters().map(MacroParameter::name).collect(Collectors.toSet());
			Set<String> s2 = existingMacro.parameter().requiredParameters().map(MacroParameter::name).collect(Collectors.toSet());
			return s1.equals(s2);
		}
	}

	public static class ListFunctionGroup extends ListGroup{
		@Override @Nullable public Macro search(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler){
			return searchInternal(argument.asList().get(1).asList(), reportHandler);
		}
	}

	public static class ObjectFunctionGroup extends ObjectGroup{
		@Override @Nullable public Macro search(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler){
			return searchInternal(argument.asList().get(1).asObj(), reportHandler);
		}
	}

	private static void reportAmbiguousUsage(@Nullable BiConsumer<ReportType, String> reportHandler,
	                                         MacroSignature signature, Iterable<Macro> ambiguousMacros){
		if(reportHandler==null) return;
		StringBuilder stb = new StringBuilder("Ambiguous usage of macro ").append(signature).append(':');
		for(Macro m : ambiguousMacros)
			m.signatureAndParameter(true).toPrettyString(stb.append("\n  "), 1, ToStringOption.DEFAULT, ToStringContext.NONE);
		reportHandler.accept(ReportType.ERROR, stb.toString());
	}

	private static void reportNoMatch(@Nullable BiConsumer<ReportType, String> reportHandler,
	                                  Collection<Macro> macros){
		if(reportHandler==null) return;
		if(macros.isEmpty()){
			reportHandler.accept(ReportType.ERROR, "No macro defined, this shouldn't happen");
		}else{
			StringBuilder stb = new StringBuilder("Wrong usage, expected:");
			for(Macro m : macros)
				m.signatureAndParameter(true).toPrettyString(stb.append("\n  "), 1, ToStringOption.DEFAULT, ToStringContext.NONE);
			reportHandler.accept(ReportType.ERROR, stb.toString());
		}
	}

	private static void reportOverwrite(@Nullable BiConsumer<ReportType, String> reportHandler,
	                                    Macro newMacro, Collection<Macro> overwrittenMacros){
		if(reportHandler==null) return;
		StringBuilder stb = new StringBuilder("New macro ").append(newMacro.signature()).append(" overwrites ")
				.append(overwrittenMacros.size()).append(" preexisting macro(s).")
				.append("\n  Preexisting macro(s):");
		for(Macro m : overwrittenMacros)
			m.signatureAndParameter(true).toPrettyString(stb.append("\n    "), 2, ToStringOption.DEFAULT, ToStringContext.NONE);
		newMacro.signatureAndParameter(true).toPrettyString(stb.append("\n  New macro:\n    "), 2, ToStringOption.DEFAULT, ToStringContext.NONE);
		reportHandler.accept(ReportType.WARN, stb.toString());
	}

	private static void reportOverlap(@Nullable BiConsumer<ReportType, String> reportHandler,
	                                  Macro newMacro, Collection<Macro> overlappingMacros){
		if(reportHandler==null) return;
		StringBuilder stb = new StringBuilder("New macro ").append(newMacro.signature()).append(" possibly overlaps with ")
				.append(overlappingMacros.size()).append(" preexisting macro(s).")
				.append("\n  Preexisting macro(s):");
		for(Macro m : overlappingMacros)
			m.signatureAndParameter(true).toPrettyString(stb.append("\n    "), 2, ToStringOption.DEFAULT, ToStringContext.NONE);
		newMacro.signatureAndParameter(true).toPrettyString(stb.append("\n  New macro:\n    "), 2, ToStringOption.DEFAULT, ToStringContext.NONE);
		reportHandler.accept(ReportType.INFO, stb.toString());
	}
}
