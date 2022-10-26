package ttmp.among;

/**
 * Context of current {@link ToPrettyString#toString(StringBuilder, ToStringOption, ToStringContext)} /
 * {@link ToPrettyString#toPrettyString(StringBuilder, int, ToStringOption, ToStringContext)}. Used in literal quoting
 * for example.
 */
public enum ToStringContext{
	NONE, OPERATION, ROOT
}
