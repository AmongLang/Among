package among;

import among.exception.SussyCompile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Result of parsing the source.<br>
 * The 'success' and 'failure' of the operation is determined by presence of error reports. As {@link
 * CompileResult#root} is always present regardless of the result, checking presence of any errors reports before using
 * root is recommended.
 */
public final class CompileResult{
	private final Source source;
	private final AmongRoot root;
	private final AmongDefinition definition;
	private final List<Report> reports;

	public CompileResult(Source source, AmongRoot root, AmongDefinition definition, List<Report> reports){
		this.source = source;
		this.root = root;
		this.definition = definition;
		this.reports = new ArrayList<>(reports);
	}

	/**
	 * Returns the source used in operation.
	 *
	 * @return The source used in operation
	 */
	public Source source(){
		return source;
	}
	/**
	 * The root modified during operation. This object is always present, regardless of whether error was
	 * reported or not.<br>
	 * Success or failure of the operation does not indicate whether content of this root was modified; in fact the
	 * root may have picked up erroneous interpretation of faulty script, after recovering from error. Checking presence
	 * of any errors reports before using this object is recommended.
	 *
	 * @return The root modified during operation
	 */
	public AmongRoot root(){
		return root;
	}
	/**
	 * Macros and operators defined during operation. Definitions imported are not included in this object. This object
	 * is always present, regardless of whether error was reported or not.<br>
	 * Success or failure of the operation does not indicate whether content of this definition was modified; in fact
	 * the definition may have picked up erroneous interpretation of faulty script, after recovering from error.
	 * Checking presence of any errors reports before using this object is recommended.
	 *
	 * @return The definition modified during operation
	 */
	public AmongDefinition definition(){
		return definition;
	}
	/**
	 * Objects and definitions modified during operation. Both the objects and definitions are always present,
	 * regardless of whether error was reported or not.<br>
	 * Success or failure of the operation does not indicate whether content of the objects were modified; in fact the
	 * objects may have picked up erroneous interpretation of faulty script, after recovering from error. Checking
	 * presence of any errors reports before using these objects are recommended.
	 *
	 * @return Objects and definitions modified during operation
	 */
	public RootAndDefinition rootAndDefinition(){
		return new RootAndDefinition(root, definition);
	}
	/**
	 * Unmodifiable list of all reports.
	 *
	 * @return Unmodifiable list of all reports
	 */
	public List<Report> reports(){
		return Collections.unmodifiableList(reports);
	}

	/**
	 * Whether the operation was successful or not. It is determined by simply checking for presence of error reports;
	 * any error report found indicates failure of the operation.
	 *
	 * @return Whether the operation was successful or not
	 */
	public boolean isSuccess(){
		return !hasError();
	}

	/**
	 * Whether there are any errors reported or not.
	 *
	 * @return Whether there are any errors reported or not
	 */
	public boolean hasError(){
		for(Report r : reports)
			if(r.type()==ReportType.ERROR) return true;
		return false;
	}

	/**
	 * Whether there are any warnings reported or not.
	 *
	 * @return Whether there are any warnings reported or not
	 */
	public boolean hasWarning(){
		for(Report r : reports)
			if(r.type()==ReportType.WARN) return true;
		return false;
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
		printReports(null);
	}
	public void printReports(@Nullable String path){
		if(reports.isEmpty()) return;
		printReports(path, isSuccess() ? System.out::println : System.err::println);
	}

	public void printReports(@Nullable String path, Consumer<String> logger){
		if(reports.isEmpty()) return;
		int infoCount = 0;
		int warningCount = 0;
		int errorCount = 0;
		for(Report r : reports){
			switch(r.type()){
				case INFO:
					infoCount++;
					break;
				case WARN:
					warningCount++;
					break;
				case ERROR:
					errorCount++;
					break;
			}
		}
		List<String> types = new ArrayList<>();
		if(errorCount>0) types.add(errorCount==1 ? "1 error" : errorCount+" errors");
		if(warningCount>0) types.add(warningCount==1 ? "1 warning" : warningCount+" warnings");
		if(infoCount>0) types.add(infoCount+" info");

		StringBuilder stb = new StringBuilder();
		if(path==null) stb.append("Compilation finished with ");
		else stb.append("Compilation of script at '").append(path).append("' finished with ");
		for(int i = 0; i<types.size(); i++){
			if(i>0) stb.append(i==types.size()-1 ? " and " : ", ");
			stb.append(types.get(i));
		}

		logger.accept(stb.toString());
		for(Report report : this.reports)
			report.print(this.source, logger);
	}
}
