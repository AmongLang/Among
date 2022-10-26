package ttmp.among;

import org.jetbrains.annotations.Nullable;

/**
 * Simple object to provide instance matching the path provided.
 */
@FunctionalInterface
public interface Provider<T>{
	/**
	 * Tries to resolve an instance from given path. The exception thrown will be logged and handled in appropriate
	 * places.
	 *
	 * @param path Path, nonnull expected
	 * @return Source resolved and read, or {@code null} if not found
	 * @throws Exception If any error occurs
	 */
	@Nullable T resolve(String path) throws Exception;
}
