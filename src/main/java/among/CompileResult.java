package among;

import among.exception.SussyCompile;
import among.report.ReportList;
import org.jetbrains.annotations.Nullable;

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
	private final ReportList reports;

	public CompileResult(Source source, AmongRoot root, AmongDefinition definition, ReportList reports){
		this.source = source;
		this.root = root;
		this.definition = definition;
		this.reports = new ReportList(reports);
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
	public ReportList reports(){
		return reports;
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
		return reports.hasError();
	}

	/**
	 * Whether there are any warnings reported or not.
	 *
	 * @return Whether there are any warnings reported or not
	 */
	public boolean hasWarning(){
		return reports.hasWarning();
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
		reports.printReports(this.source);
	}
	public void printReports(@Nullable String path){
		reports.printReports(path, this.source);
	}

	public void printReports(@Nullable String path, Consumer<String> logger){
		reports.printReports(path, this.source, logger);
	}
}
