package among.macro;

import among.NodePath;
import among.report.ReportHandler;
import among.exception.Sussy;
import among.obj.Among;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Object representing one replacement operation.
 *
 * @see MacroDefinition
 */
public final class MacroReplacement{
	public static MacroReplacement valueReplacement(NodePath path, int param){
		return new MacroReplacement(path, new MacroOp.ValueReplacement(param));
	}
	public static MacroReplacement nameReplacement(NodePath path, int param){
		return new MacroReplacement(path, new MacroOp.NameReplacement(param));
	}
	public static MacroReplacement macroCall(NodePath path, Macro macro){
		return new MacroReplacement(path, new MacroOp.MacroCall(macro));
	}

	private final NodePath path;
	private final MacroOp operation;

	public MacroReplacement(NodePath path, MacroOp operation){
		this.path = path;
		this.operation = operation;
	}

	public NodePath path(){
		return path;
	}
	public MacroOp operation(){
		return operation;
	}

	/**
	 * Applies the replacement.
	 *
	 * @param args          Argument for the replacement
	 * @param target        Target
	 * @param copyConstant  If {@code true}, macros executed inside this operation returns deep copy of their template.
	 *                      If {@code false}, they might return reference to object which might be shared between other
	 *                      places.
	 * @param reportHandler Optional report handler
	 * @return Object after replacement; it will just return {@code target} most of the time
	 */
	public Among apply(Among[] args, Among target, boolean copyConstant, @Nullable ReportHandler reportHandler){
		return operation.applyTo(path, args, target, copyConstant, reportHandler);
	}

	@Override public String toString(){
		return "MacroReplacement{"+
				"path="+path+
				", operation="+operation+
				'}';
	}

	public static abstract class MacroOp{
		/**
		 * Applies the replacement.
		 *
		 * @param path          Path of the operation target relative to {@code target}
		 * @param args          Argument for the replacement
		 * @param target        Target
		 * @param copyConstant  If {@code true}, macros executed inside this operation returns deep copy of their
		 *                      template.
		 *                      If {@code false}, they might return reference to object which might be shared between
		 *                      other places.
		 * @param reportHandler Optional report handler
		 * @return Object after replacement; it will just return {@code target} most of the time
		 */
		public abstract Among applyTo(NodePath path, Among[] args, Among target, boolean copyConstant, @Nullable ReportHandler reportHandler);

		public static final class ValueReplacement extends MacroOp{
			private final int param;

			public ValueReplacement(int param){
				this.param = param;
			}

			public int param(){
				return param;
			}

			@Override public Among applyTo(NodePath path, Among[] args, Among target, boolean copyConstant, @Nullable ReportHandler reportHandler){
				if(path.isEmpty()) return args[this.param];
				if(!path.resolveAndSet(target, args[this.param]))
					throw new Sussy("No replacement target");
				return target;
			}

			@Override public String toString(){
				return "ValueReplacement{"+
						"param="+param+
						'}';
			}
		}

		public static final class NameReplacement extends MacroOp{
			private final int param;

			public NameReplacement(int param){
				this.param = param;
			}

			public int param(){
				return param;
			}

			@Override public Among applyTo(NodePath path, Among[] args, Among target, boolean copyConstant, @Nullable ReportHandler reportHandler){
				Among resolved = path.resolveAndGet(target);
				if(resolved==null) throw new Sussy("No replacement target");
				resolved.asNameable().setName(args[this.param].asPrimitive().getValue());
				return target;
			}

			@Override public String toString(){
				return "NameReplacement{"+
						"param="+param+
						'}';
			}
		}

		public static final class MacroCall extends MacroOp{
			private final Macro macro;

			public MacroCall(Macro macro){
				this.macro = Objects.requireNonNull(macro);
			}

			public Macro macro(){
				return macro;
			}

			@Override public Among applyTo(NodePath path, Among[] args, Among target, boolean copyConstant, @Nullable ReportHandler reportHandler){
				if(path.isEmpty()) return macro.apply(target);
				Among among = path.resolveAndGet(target);
				if(among==null) throw new Sussy("No replacement target");
				if(!path.resolveAndSet(target, macro.apply(among, copyConstant, reportHandler)))
					throw new Sussy("No replacement target");
				return target;
			}

			@Override public String toString(){
				return "MacroCall{"+
						"macro="+macro+
						'}';
			}
		}
	}
}
