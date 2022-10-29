package among;

import among.exception.SussyCompile;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Result of {@link AmongEngine#readFrom(String, Consumer)} and {@link AmongEngine#getOrReadFrom(String,
 * Consumer)}.<br>
 * This object can be divided into three states, each being:
 * <ul>
 *   <li>Script being resolved from instance provider. It will contain no {@link CompileResult}.</li>
 *   <li>Script being resolved from source provider. It will contain {@link CompileResult} as well as other things.</li>
 *   <li>Script failed to resolve, either by no source being provided or by unexpected exception.
 *   It will contain no {@link CompileResult}. {@link RootAndDefinition} provided by the instance will be an empty instance.</li>
 * </ul>
 * <p>
 * For compiled results, sources can occupy large chunk of memory if not cleaned after. Calling
 * {@link ReadResult#dropCompileResult()} after using the result can save some memory. Alternative option is deleting
 * the cache made by {@link AmongEngine}, by calling {@link AmongEngine#clearInstances()}.
 */
public abstract class ReadResult{
	private final String path;
	private final RootAndDefinition rootAndDefinition;

	private ReadResult(String path, RootAndDefinition rootAndDefinition){
		this.path = path;
		this.rootAndDefinition = rootAndDefinition;
	}

	public String path(){
		return path;
	}
	public RootAndDefinition rootAndDefinition(){
		return rootAndDefinition;
	}
	public AmongRoot root(){
		return rootAndDefinition.root();
	}
	public AmongDefinition definition(){
		return rootAndDefinition.definition();
	}

	public abstract boolean isSuccess();

	@Nullable public abstract CompileResult result();
	@Nullable public Source source(){
		CompileResult result = result();
		return result!=null ? result.source() : null;
	}

	/**
	 * Check for {@link CompileResult#isSuccess()}. If {@code isSuccess() == false}, an exception will be thrown.
	 *
	 * @throws SussyCompile If {@code isSuccess() == false}
	 */
	public void expectSuccess(){
		if(!isSuccess()) throw new SussyCompile("Failed to compile");
	}

	public void printReports(){
		CompileResult result = this.result();
		if(result!=null) result.printReports();
	}
	public void printReports(@Nullable String path){
		CompileResult result = this.result();
		if(result!=null) result.printReports(path);
	}
	public void printReports(@Nullable String path, Consumer<String> logger){
		CompileResult result = this.result();
		if(result!=null) result.printReports(path, logger);
	}

	/**
	 * Discards compile results. Subsequent calls to {@link ReadResult#result()} yields {@code null}. If this result
	 * has no compile result, this method does nothing.
	 */
	public void dropCompileResult(){}

	/**
	 * Result for scripts resolved with instance provider.
	 */
	public static final class Provided extends ReadResult{
		public Provided(String path, RootAndDefinition rootAndDefinition){
			super(path, rootAndDefinition);
		}

		@Override public boolean isSuccess(){
			return true;
		}
		@Nullable @Override public CompileResult result(){
			return null;
		}
	}

	/**
	 * Result for compiled scripts.
	 */
	public static final class Compiled extends ReadResult{
		private final boolean success;
		@Nullable private CompileResult result;

		public Compiled(String path, CompileResult result){
			super(path, result.rootAndDefinition());
			this.success = result.isSuccess();
			this.result = result;
		}

		@Override public boolean isSuccess(){
			return success;
		}
		@Override public CompileResult result(){
			return result;
		}
		@Override public void dropCompileResult(){
			this.result = null;
		}
	}

	/**
	 * Result for scripts failed to resolve.
	 */
	public static final class Failure extends ReadResult{
		public Failure(String path){
			super(path, new RootAndDefinition());
		}

		@Override public boolean isSuccess(){
			return false;
		}
		@Override public @Nullable CompileResult result(){
			return null;
		}
	}
}
