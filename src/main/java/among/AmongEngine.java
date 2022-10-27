package among;

import among.internals.Parser;
import among.internals.library.DefaultInstanceProvider;
import among.macro.MacroDefinition;
import among.obj.Among;
import among.operator.OperatorDefinition;
import among.operator.OperatorRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An object responsible for reading among sources. Additionally, this class provides number of compilation options and
 * interface to provide custom instances or source resolving rules.
 */
public class AmongEngine{
	/**
	 * If enabled, duplicate properties (Multiple properties with identical key) will not produce compilation error.
	 * Instead, any duplicate properties following the first will be ignored, and reported as warning.
	 */
	public boolean allowDuplicateObjectProperty = false;

	/**
	 * If enabled, invalid registration of operators will not produce compilation error.
	 * Instead, it will only produce warning.
	 *
	 * @see OperatorRegistry#add(OperatorDefinition)
	 */
	public boolean allowInvalidOperatorRegistration = false;

	/**
	 * If enabled, constant macro will return deep copied value, rather than template itself. Disabling this option
	 * makes the same instance to be shared between each macro usage along with the macro itself, avoiding potentially
	 * expensive deep copy operation and saving memory. But doing so poses possible risk of undefined behavior if the
	 * value is modified after.<br>
	 * It is advised to disable this option only if the result is not expected to be modified afterwards.
	 *
	 * @see MacroDefinition#apply(Among, boolean)
	 */
	public boolean copyMacroConstant = true;

	/**
	 * Specifies error handling behavior for invalid unicode escape. It specifically refers to invalid trailing value
	 * for {@code \u005Cu} and {@code \u005CU} notation, which should be a hexadecimal with 4 characters (for {@code
	 * \u005Cu}) and 6 characters (for {@code \u005CU}). This behavior is also used when the codepoint supplied by
	 * {@code \u005CU} notation is outside the unicode definition (larger than {@code 10FFFF}).<br>
	 * Regardless of the setting, the compiler will process the input as the {@code \u005Cu} notation never existed;
	 * for example, invalid input {@code '\u005Cuabcd'} will produce {@code 'uabcd'}.<br>
	 * If any other value is provided, {@link ErrorHandling#ERROR ERROR} will be used.
	 *
	 * @see ErrorHandling
	 */
	public int invalidUnicodeHandling = ErrorHandling.ERROR;

	private final List<Provider<Source>> sourceProviders = new ArrayList<>();
	private final List<Provider<RootAndDefinition>> instanceProviders = new ArrayList<>();
	private final Map<String, RootAndDefinition> pathByInstance = new HashMap<>();

	{
		instanceProviders.add(DefaultInstanceProvider.instance());
	}

	/**
	 * Add new source provider to this root. Source providers are searched consecutively with registration order, from
	 * oldest to newest.
	 *
	 * @param sourceProvider The source provider to be registered
	 * @throws NullPointerException If {@code sourceProvider == null}
	 */
	public final void addSourceProvider(Provider<Source> sourceProvider){
		sourceProviders.add(Objects.requireNonNull(sourceProvider));
	}

	/**
	 * Add new instance provider to this root. Instance providers are searched consecutively with registration order,
	 * from oldest to newest.
	 *
	 * @param instanceProvider The instance provider to be registered
	 * @throws NullPointerException If {@code sourceProvider == null}
	 */
	public final void addInstanceProvider(Provider<RootAndDefinition> instanceProvider){
		instanceProviders.add(Objects.requireNonNull(instanceProvider));
	}

	/**
	 * Reads and parses the source into newly created {@link AmongRoot}. The instance read will not be correlated to any
	 * path.
	 *
	 * @param source Source to be read from
	 * @return Result with new root containing objects parsed from {@code source}
	 * @see AmongEngine#read(Source, AmongRoot, AmongDefinition)
	 */
	public final CompileResult read(Source source){
		return read(source, null, null);
	}

	/**
	 * Reads and parses the source into given {@link AmongRoot}, or new one if {@code null} is supplied.
	 *
	 * @param source           Source to be read from
	 * @param root             Root to be used; will be modified returned as compilation result. If {@code null} is
	 *                         supplied, new root will be created.
	 * @param importDefinition Imported definitions to be used; will be modified. Does not get returned as compilation
	 *                         result.
	 * @return Result with {@code root} (or new root if it was {@code null}) containing objects parsed from {@code source}
	 */
	public final CompileResult read(Source source, @Nullable AmongRoot root, @Nullable AmongDefinition importDefinition){
		return new Parser(source, this,
				root==null ? new AmongRoot() : root,
				importDefinition==null ? new AmongDefinition() : importDefinition)
				.parse();
	}

	/**
	 * Get an instance of {@link AmongRoot} correlated to specific path. If the instance was not read yet, the engine
	 * will try to resolve the instance using instance providers, then the source - which will be read with {@link
	 * AmongEngine#read(Source)}. The resulting object will be automatically correlated to given path.<br>
	 * If neither instance nor source cannot be resolved, {@code null} will be returned. If the compilation result
	 * returned by {@link AmongEngine#read(Source)} contains error, {@code null} will be returned. Following calls in
	 * the future with identical path will yield {@code null} without an attempt to resolve the instance or source
	 * again.<br>
	 * Note that modifying the returned root might produce unwanted behavior.
	 *
	 * @param path Path of the instance
	 * @return Instance of {@link AmongRoot} correlated to the path, or {@code null} if the search failed
	 * @throws NullPointerException If {@code path == null}
	 */
	@Nullable public final RootAndDefinition getOrReadFrom(String path){
		RootAndDefinition r = pathByInstance.get(path);
		if(r==null){
			r = resolve(path);
			pathByInstance.put(path, r);
		}
		return r;
	}

	/**
	 * Try to resolve an instance of {@link AmongRoot} with given path, first using instance providers, then the source
	 * - which will be read with {@link AmongEngine#read(Source)}. If succeeded, the instance will be correlated to
	 * the path. If there was already an instance correlated, it will be overwritten.<br>
	 * If neither instance nor source cannot be resolved, {@code null} will be returned. If the compilation result
	 * returned by {@link AmongEngine#read(Source)} contains error, {@code null} will be returned. Following calls of
	 * {@link AmongEngine#getOrReadFrom(String)} in
	 * the future with identical path will yield {@code null} without an attempt to resolve the instance or source
	 * again.<br>
	 * Note that modifying the returned root might produce unwanted behavior.
	 *
	 * @param path Path of the instance
	 * @return Instance of {@link AmongRoot} correlated to the path, or {@code null} if the search failed
	 * @throws NullPointerException If {@code path == null}
	 */
	@Nullable public final RootAndDefinition readFrom(String path){
		RootAndDefinition r = resolve(path);
		pathByInstance.put(path, r);
		return r;
	}

	private final LinkedHashSet<String> resolvingPathCache = new LinkedHashSet<>();

	@Nullable private RootAndDefinition resolve(String path){
		if(!resolvingPathCache.add(path)){
			List<String> trace = new ArrayList<>();
			boolean found = false;
			for(String s : resolvingPathCache){
				if(found) trace.add(s);
				else if(s.equals(path)){
					found = true;
					trace.add(s);
				}
			}
			handleCircularReference(path, trace);
			return null;
		}
		RootAndDefinition r = resolveInternal(path);
		resolvingPathCache.remove(path);
		return r;
	}

	@Nullable private RootAndDefinition resolveInternal(String path){
		for(Provider<RootAndDefinition> ip : instanceProviders){
			try{
				RootAndDefinition resolve = ip.resolve(path);
				if(resolve!=null) return resolve;
			}catch(Exception ex){
				handleInstanceResolveException(path, ex);
			}
		}
		for(Provider<Source> sp : sourceProviders){
			try{
				Source source = sp.resolve(path);
				if(source!=null){
					RootAndDefinition rad = createDefaultDefinition(path);
					CompileResult res = read(source,
							rad==null ? null : rad.root(),
							rad==null ? null : rad.definition());
					if(res.isSuccess()){
						handleCompileSuccess(path, res);
						return res.rootAndDefinition();
					}else{
						handleCompileError(path, res);
						return null;
					}
				}
			}catch(Exception ex){
				handleSourceResolveException(path, ex);
			}
		}
		return null;
	}

	/**
	 * Clears all caches of instance read with {@link AmongEngine#getOrReadFrom(String)} and {@link
	 * AmongEngine#readFrom(String)}. Source providers and instance providers are not affected.
	 */
	public final void clearInstances(){
		pathByInstance.clear();
	}

	protected void handleSourceResolveException(String path, Exception ex){
		System.err.println("An error occurred while resolving '"+path+"'");
		ex.printStackTrace();
	}

	protected void handleInstanceResolveException(String path, Exception ex){
		System.err.println("An error occurred while resolving '"+path+"'");
		ex.printStackTrace();
	}

	protected void handleCompileSuccess(String path, CompileResult result){
		result.printReports(path);
	}

	protected void handleCompileError(String path, CompileResult result){
		result.printReports(path);
	}

	protected void handleCircularReference(String path, List<String> trace){
		switch(trace.size()){
			case 0: // what?
				System.err.println("Cannot import script resolved with '"+path+"' due to cosmic ray interference");
				break;
			case 1: // self-reference
				System.err.println("Self-reference in script '"+path+"'");
				break;
			default: // circular reference
				System.err.println("Cannot import script resolved with '"+path+"' due to circular reference:");
				for(int i = 0; i<trace.size()-1; i++)
					System.err.println("  '"+trace.get(i)+"' references '"+trace.get(i+1)+"'");
				System.err.println("  and '"+trace.get(trace.size()-1)+"' references '"+path+"'");
		}
	}

	/**
	 * Create default definition for source at given path. If {@code null} is returned, an empty root and definition
	 * will be used.<br>
	 * The root and definition may be modified during compilation. As such, each root and definition returned from this
	 * method should be newly created.
	 *
	 * @return Default definition for source at given path
	 */
	@Nullable protected RootAndDefinition createDefaultDefinition(String path){
		return null;
	}
}
