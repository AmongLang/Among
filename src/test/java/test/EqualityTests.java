package test;

import among.obj.Among;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static among.obj.Among.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class EqualityTests{
	@Test public void objTest(){
		simpleEqualityTest("objTest",
				object().prop("Hello", "World!"),
				object().prop("1", 1).prop("2", 2).prop("3", 3),
				object().prop("Hello", "World!"),
				object().prop("1", 1).prop("2", 2).prop("3", 3));
	}

	@Test public void listTest(){
		simpleEqualityTest("listTest",
				list("one", "two", "oatmeal"),
				list("one two oatmeal", "kirby is a pink guy", "one two oatmeal", "because kirby is very cute"));
	}

	@Test public void operationTest(){
		simpleEqualityTest("operationTest",
				list(1, 1, 2, 3, 5, 8, 13, 21),
				list(1, 1, 2, 3, 5, 8, 13, 21));
	}

	@Test public void primitiveTest(){
		simpleEqualityTest("primitiveTest",
				value("The Industrial Revolution and its consequences have been a disaster for the\nhuman race."),
				value("According to all known laws of aviation, there is no way a bee should be able to fly.\n"+
						"Its wings are too small to get its fat little body off the ground.\n"+
						"The bee, of course, flies anyway because bees don't care what humans think is impossible.\n"+
						"Yellow, black. Yellow, black.\n"+
						"Yellow, black. Yellow, black.\n"+
						"Ooh, black and yellow!\n"+
						"Let's shake it up a l\n"),
				value("Look ma, I'm on TV!"),
				value("\b\f\n\r\t\u0060\uD828\uDC90"));
	}

	@Test public void macroTest(){
		simpleEqualityTest("macroTest",
				value("This is macro"),
				namedList("macro1"),
				object().prop("Macro", "Hi!"),
				object().prop("Macro", namedList("*", "amo", "gus")));
	}

	@Test public void collapseUnaryOperation(){
		simpleEqualityTest("collapseUnaryOperation",
				namedList("/",
						namedList("-", "x", "y"),
						"2"),
				namedList("+",
						namedList("+",
								namedList("+",
										list(1, 2, 3),
										list("list")),
								list()),
						namedList("fib", 3)));
	}

	@Test public void undefTest(){
		simpleEqualityTest("undefTest",
				value("Yes, I am a macro, indeed......"),
				value("Yes, I am a macro, indeed......"),
				namedList("areYouMacro"),
				namedList("!!", "a", "b"),
				namedList("!!", namedList("!!", "a"), "b"),
				list("!!", "!!"));
	}

	@Test public void unicodeTest(){
		simpleEqualityTest("unicodeTest",
				value("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠛⠛⠛⠛⠛⠛⢛⣿⠿⠟⠛⠛⠛⠛⠛⠛⠿⠿⣿⣟⠛⠛⠛⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⣛\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣇⠀⠀⠀⠀⢠⡿⠁⣀⢀⢀⠀⣀⠀⣀⠀⠀⡀⣀⠙⢷⡀⠺⠿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠃⣰⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠀⠀⠀⠀⢸⡇⢰⢿⣿⢻⡿⣿⢿⣯⡼⣧⡇⣿⡆⢸⡇⠀⠀⠙⣿⣿⣿⣿⣿⣿⣿⠃⢀⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣄⣀⡀⠀⠘⢷⣀⠀⠀⠀⠀⠉⠀⠈⠀⠀⠀⠀⢀⣼⠃⠀⠀⠀⠉⠛⠿⢿⣿⣿⡏⠀⣼⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣿⡿⠟⠉⠀⡀⠀⠀⠉⠛⢷⣄⠈⢙⡷⠀⠀⣠⣤⣤⣤⣤⣤⡴⠾⠋⠁⣠⡶⠶⠶⠶⠶⣤⡀⠀⣿⡇⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⠏⠀⠀⠐⠉⠉⠁⠀⠀⠀⠀⠹⣶⠻⠟⠛⠛⠋⠀⠀⠀⡏⠀⠀⠀⠀⢠⡏⣠⣤⠤⠤⣄⡈⢻⡄⣿⡇⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⠀⠀⠀⢰⡖⠲⣶⣶⢤⡤⠤⣤⣿⡆⠀⠀⠀⠀⠀⠀⠀⡇⠀⠀⠀⠀⡾⠀⠻⣷⣶⡶⠾⠃⠈⣿⣿⡇⠀⣿⡿⠿⢿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣆⠀⠀⢀⣉⣛⠛⠉⢠⡙⠲⢿⣿⠃⠀⠀⠀⠀⠀⠀⢰⠇⠀⠀⠀⢰⡇⠀⠀⠀⠀⠀⠀⠀⠀⢹⣿⡇⠀⣿⣧⣤⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⣿⣷⣤⣌⠛⠿⠿⠖⣎⣤⣶⡛⠁⠀⠀⠀⠀⠀⠀⠀⢸⠀⠀⠀⠀⣾⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⡇⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⣿⣿⠟⠋⠉⠙⠛⠻⣿⣿⣿⣿⣿⣿⣧⠀⠀⠀⠀⠀⠀⠀⢸⠀⠀⠀⢠⡇⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⡇⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⡿⠁⠀⠠⢤⡀⠀⢀⡬⠟⣻⣿⣯⠍⠻⣆⠀⠀⠀⠀⠀⠀⢸⠀⠀⠀⢸⡇⠀⣠⠶⠶⠶⢶⡀⠀⠀⢸⣿⡇⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⣿⠃⠀⡀⠀⠀⠉⠓⠋⠀⠀⣳⣾⡴⠂⠀⢹⡆⠀⠀⠀⠀⢀⣸⣰⣛⣛⣺⣀⣀⣸⣆⣀⣀⣸⣇⣀⣀⣸⣿⡇⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⡏⠀⠀⠉⠓⢦⣄⣀⣠⣴⣿⣷⣼⣵⣻⡄⠀⡇⠀⠀⠀⠀⢸⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿\n"+
						"⣿⣿⣿⣿⠀⠀⠀⠀⠀⠀⠀⠉⢹⣿⣿⣿⣿⣿⣍⣀⣸⣧⣤⣤⣤⣤⣼⣄⣀⣀⣀⡀⠀⢀⣀⣠⣤⣤⣤⣤⣤⣤⣀⣀⣀⠀⠀⠀⠀⠀\n"+
						"⠛⠛⢻⡏⠀⠀⠀⠀⠀⠀⠀⠀⣾⠀⠀⠀⠀⠀⠈⠉⠁⠀⠀⠀⠀⠀⠉⠉⠉⠛⠛⠛⠛⠛⠉⠉⠀⠀⠀⠀⠀⠉⠉⠉⠛⠛⠛⠛⠛⠛\n"+
						"⣀⣀⣸⠁⠀⠀⢀⣶⣶⣦⠀⢀⣟⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣠⣄⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀⣀"),
				value("tfw u get stranded\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE1E\uD83D\uDE1E\uD83D\uDE1E\uD83D\uDE1E\uD83D\uDE1E\uD83D\uDE1E\uD83D\uDE2B\uD83D\uDE2B\uD83D\uDE2B\uD83D\uDE2B\uD83D\uDE2B\uD83D\uDE2B\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D"+
						"succs \uD83D\uDC4E\uD83D\uDC4E\uD83D\uDC4E\uD83D\uDC4E\uD83D\uDC4E\uD83D\uDC4E\uD83D\uDC4E\uD83D\uDE3E\uD83D\uDE3E\uD83D\uDE3E\uD83D\uDE3E\uD83D\uDE3E\uD83D\uDE21\uD83D\uDE21\uD83D\uDE21\uD83D\uDE21\uD83D\uDCA9\uD83D\uDCA9\uD83D\uDCA9\uD83D\uDCA9\uD83D\uDCA9 cause theres no pokestops"+
						"\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDC4C\uD83D\uDC4C\uD83D\uDC4C\uD83D\uDC4C\uD83D\uDC4C\uD83D\uDCAF\uD83D\uDCAF\uD83D\uDCAF\uD83D\uDCAF\uD83D\uDCAF\uD83D\uDE1C\uD83D\uDE1C\uD83D\uDE1C\uD83D\uDE1C so whoever sees this \uD83D\uDC40\uD83D\uDC40\uD83D\uDC40\uD83D\uDC40\uD83D\uDC40"+
						"\uD83D\uDC48\uD83D\uDC48\uD83D\uDC48\uD83D\uDC48\uD83D\uDC48\uD83D\uDC48 u know what to do \uD83D\uDE0B\uD83D\uDE0B\uD83D\uDE0B\uD83D\uDE0F\uD83D\uDE0F\uD83D\uDE0F\uD83D\uDE0F\uD83D\uDE0F\uD83D\uDE1B\uD83D\uDE1B\uD83D\uDE1B\uD83D\uDE1B\uD83D\uDE09\uD83D\uDE09\uD83D\uDE09\uD83D\uDE09\uD83D\uDC85\uD83D\uDC85\uD83D\uDC85\uD83D\uDC85\uD83D\uDC85\uD83D\uDC85"),
				value("\uD83F\uDDAA\uD83D\uDDE3\uD83F\uDDA8\uD83D\uDD65\uD83C\uDCA5\uD83D\uDC8A\uD83C\uDC8E\uD83F\uDEFE\uD83F\uDEDE\uD83C\uDDE2\uD83F\uDCE8\uD83E\uDCDE\uD83C\uDCCD\uD83C\uDE50\uD83D\uDC9D\uD83C\uDD1C\uD83C\uDD68\uD83E\uDC84\uD83D\uDC40\uD83E\uDFF5\uD83C\uDC27\uD83D\uDE18\uD83D\uDF87\uD83C\uDE95\uD83C\uDF8C\uD83E\uDD30\uD83D\uDC24\uD83C\uDE28\uD83E\uDCD7\uD83C\uDDDD\uD83E\uDE49\uD83E\uDFAA\uD83E\uDE68\uD83D\uDE69\uD83E\uDCCA\uD83D\uDF8A\uD83D\uDFC6\uD83C\uDE0F\uD83F\uDE32\uD83E\uDC10\uD83C\uDFAF\uD83C\uDC5E\uD83D\uDF70\uD83E\uDCA2\uD83C\uDC27\uD83E\uDEA5\uD83D\uDC34\uD83F\uDEE4\uD83D\uDF67\uD83F\uDCFC"+
						"\uD83E\uDE78\uD83C\uDC36\uD83E\uDCC0\uD83E\uDD9E\uD83D\uDD4A\uD83F\uDCA3\uD83F\uDE13\uD83E\uDD00\uD83E\uDF05\uD83E\uDF8D\uD83E\uDDB2\uD83F\uDDB4\uD83C\uDC4D\uD83E\uDF07\uD83E\uDFCC\uD83D\uDD08\uD83C\uDD9C\uD83E\uDD25\uD83C\uDDA9\uD83E\uDE16\uD83E\uDD2F\uD83F\uDD41\uD83E\uDC19\uD83F\uDE7A\uD83E\uDE2C\uD83E\uDD45\uD83C\uDD0E\uD83F\uDD90\uD83D\uDC52\uD83D\uDEB3\uD83C\uDDB8\uD83C\uDE30\uD83D\uDFEF\uD83C\uDC27\uD83E\uDDF7\uD83F\uDE57\uD83C\uDE00\uD83D\uDF3C\uD83D\uDEBF\uD83C\uDCB2\uD83D\uDD8C\uD83C\uDC09\uD83C\uDDC9\uD83C\uDDB6\uD83F\uDE6B\uD83C\uDCE9\uD83E\uDF2C\uD83F\uDEAC\uD83D\uDF74\uD83F\uDC98\uD83D\uDD68\uD83C\uDDD8\uD83E\uDEA0"+
						"\uD83D\uDEF4\uD83F\uDD32\uD83C\uDF91\uD83E\uDDFC\uD83C\uDD7C\uD83D\uDC9B\uD83F\uDE0F\uD83E\uDE58\uD83D\uDD5C\uD83C\uDF83\uD83E\uDC1E\uD83C\uDF83\uD83F\uDDFB\uD83D\uDC5C\uD83E\uDD27\uD83C\uDC15\uD83D\uDE94\uD83C\uDCFB\uD83F\uDC37\uD83F\uDE5C\uD83C\uDECD\uD83F\uDED5\uD83D\uDD3D\uD83C\uDEF8\uD83E\uDCA4\uD83F\uDC1C\uD83C\uDF92\uD83C\uDFF3\uD83D\uDE68\uD83E\uDCED\uD83C\uDC5C\uD83D\uDF22\uD83E\uDC1B\uD83F\uDD31\uD83E\uDEC7\uD83D\uDE37\uD83D\uDCF5\uD83C\uDF05\uD83C\uDF4B\uD83D\uDCEF\uD83C\uDECD\uD83C\uDE26\uD83D\uDE29\uD83D\uDF9F\uD83E\uDDDF\uD83F\uDD4D\uD83F\uDDB1\uD83E\uDF13\uD83F\uDDA0"+
						"\uD83D\uDC1A\uD83C\uDDCC\uD83D\uDC7B\uD83E\uDE89\uD83C\uDC90\uD83E\uDC20\uD83C\uDFC9\uD83F\uDE50\uD83E\uDEAC\uD83F\uDDAF\uD83E\uDC37\uD83E\uDCE9\uD83F\uDD97\uD83C\uDCB1\uD83F\uDEFC\uD83F\uDE5E\uD83C\uDEAB\uD83C\uDF3C\uD83C\uDC95\uD83D\uDD5B\uD83D\uDE4C\uD83E\uDF41\uD83D\uDDFB\uD83D\uDFE6\uD83C\uDE59\uD83D\uDCBE\uD83C\uDE57\uD83E\uDC10\uD83E\uDF3E\uD83E\uDC14\uD83F\uDD18\uD83E\uDFB2\uD83D\uDFA9\uD83C\uDD15\uD83D\uDE3A\uD83E\uDE70\uD83C\uDFD1\uD83C\uDEBD\uD83D\uDDFA\uD83F\uDCF1\uD83D\uDE7D\uD83E\uDD0C\uD83D\uDF0B\uD83C\uDCD1\uD83E\uDCC1\uD83F\uDC88\uD83F\uDD72\uD83E\uDC9E\uD83D\uDC1B\uD83D\uDFE9"+
						"\uD83E\uDFE7\uD83E\uDEB8\uD83D\uDD70\uD83D\uDC5D\uD83E\uDCFF\uD83E\uDFC8\uD83E\uDCEA\uD83D\uDC53\uD83D\uDEA0\uD83C\uDE5F\uD83C\uDF2E\uD83C\uDC8A\uD83C\uDFA4\uD83E\uDECE\uD83F\uDEDE\uD83C\uDFF6\uD83C\uDF99\uD83D\uDC54\uD83E\uDDD9\uD83C\uDC57\uD83D\uDD42\uD83D\uDCDE\uD83C\uDFEE\uD83F\uDCE0\uD83E\uDE11\uD83D\uDC96\uD83D\uDC55\uD83C\uDF58\uD83C\uDE1E\uD83F\uDC04\uD83D\uDE93\uD83C\uDEC2\uD83E\uDC83\uD83D\uDCC6\uD83D\uDD9A\uD83C\uDC1B\uD83F\uDCD5\uD83D\uDF28\uD83E\uDE6B\uD83F\uDC70\uD83C\uDF46\uD83D\uDE73\uD83C\uDE79\uD83F\uDDCC\uD83E\uDDE9\uD83D\uDE43\uD83D\uDF23"+
						"\uD83E\uDE64\uD83E\uDFEC\uD83F\uDE56\uD83C\uDDA4\uD83E\uDF55\uD83C\uDCD6\uD83E\uDF4D\uD83E\uDF96\uD83D\uDDA8\uD83E\uDC6E\uD83C\uDF46\uD83C\uDDEA\uD83D\uDF0C\uD83C\uDD64\uD83C\uDC24\uD83C\uDE1D\uD83E\uDD9E\uD83C\uDC52\uD83D\uDF1D\uD83F\uDCD0\uD83F\uDD5A\uD83E\uDC9F\uD83D\uDC17\uD83D\uDDD0\uD83C\uDED5\uD83C\uDEDD\uD83F\uDDA0\uD83C\uDF7C\uD83D\uDED3\uD83D\uDC29\uD83E\uDD18\uD83C\uDC9A\uD83E\uDD17\uD83D\uDDFE\uD83E\uDE43\uD83D\uDECE\uD83D\uDEEF\uD83F\uDD0F\uD83F\uDC02\uD83D\uDD93\uD83E\uDC41\uD83F\uDD8B\uD83D\uDDF2\uD83C\uDED9\uD83D\uDDB4\uD83D\uDE40\uD83D\uDCE8\uD83E\uDFC2\uD83E\uDC19\uD83C\uDDFB\uD83C\uDC27\uD83C\uDE96\uD83F\uDCFE"+
						"\uD83C\uDFDE\uD83C\uDEE8\uD83E\uDDDC\uD83C\uDF8E\uD83C\uDF41\uD83E\uDF93\uD83F\uDC9E\uD83D\uDC78\uD83D\uDDAB\uD83F\uDDB6\uD83D\uDD27\uD83E\uDD6D\uD83E\uDE1F\uD83E\uDCA4\uD83F\uDCC4\uD83F\uDD57\uD83F\uDDB8\uD83C\uDF77\uD83D\uDC8A\uD83C\uDCCF\uD83C\uDF04\uD83E\uDCB7\uD83D\uDDA3\uD83F\uDDF5\uD83D\uDCA7\uD83E\uDC5E\uD83D\uDDF4\uD83C\uDFE5\uD83D\uDE39\uD83D\uDE4F\uD83C\uDECF\uD83E\uDDA4\uD83E\uDC58\uD83C\uDD5B\uD83E\uDFCC\uD83C\uDD9A\uD83D\uDE79\uD83F\uDD5B\uD83F\uDE9F\uD83D\uDCFF\uD83E\uDE53\uD83D\uDF23\uD83F\uDEBB\uD83E\uDDB6\uD83D\uDEAD\uD83D\uDE0B\uD83E\uDF03\uD83E\uDE4D\uD83C\uDF0F"+
						"\uD83E\uDFF0\uD83F\uDDE2\uD83E\uDD3A\uD83E\uDF0B\uD83D\uDC75\uD83D\uDE06\uD83E\uDF3D\uD83D\uDC45\uD83C\uDC37\uD83D\uDF43\uD83F\uDD22\uD83D\uDC5C\uD83D\uDDF4\uD83F\uDD46\uD83E\uDCDC\uD83D\uDD39\uD83C\uDC7A\uD83F\uDE67\uD83D\uDFD9\uD83C\uDC95\uD83D\uDDE9\uD83E\uDF58\uD83D\uDFA0\uD83E\uDEB8\uD83E\uDD6F\uD83F\uDCD6\uD83D\uDE94\uD83F\uDDE4\uD83E\uDFD7\uD83D\uDF1A\uD83D\uDEB7\uD83C\uDC62\uD83C\uDD51\uD83D\uDC73\uD83E\uDE17\uD83D\uDEEE\uD83E\uDF84\uD83E\uDD76\uD83D\uDFDA\uD83E\uDE0A\uD83D\uDC38\uD83D\uDC01\uD83E\uDCE9\uD83D\uDD77\uD83D\uDDFC\uD83E\uDE13\uD83D\uDC89\uD83E\uDD02\uD83D\uDF32"+
						"\uD83F\uDE6A\uD83D\uDDE9\uD83E\uDF96\uD83D\uDEFF\uD83C\uDEB3\uD83E\uDDE5\uD83D\uDF7C\uD83C\uDF51\uD83C\uDDD5\uD83E\uDCA7\uD83C\uDE83\uD83D\uDE22\uD83F\uDEC4\uD83D\uDF6B\uD83C\uDEA0\uD83E\uDCBC\uD83C\uDD26\uD83E\uDC3D\uD83D\uDFD6\uD83E\uDFB3\uD83C\uDF47\uD83D\uDD47\uD83D\uDCCA\uD83C\uDE47\uD83D\uDE9B\uD83C\uDEE8\uD83F\uDED5\uD83C\uDFE8\uD83E\uDCBA\uD83E\uDF0C\uD83F\uDCCD\uD83C\uDDC6\uD83D\uDD54\uD83D\uDE4D\uD83C\uDE60\uD83C\uDFC5\uD83D\uDC71\uD83C\uDF92\uD83F\uDD08\uD83E\uDCCA\uD83F\uDD8E\uD83F\uDE9F\uD83E\uDF0D\uD83E\uDD2A\uD83D\uDDC0\uD83F\uDC5D\uD83C\uDE66\uD83D\uDFDB\uD83D\uDC9C\uD83E\uDD65"+
						"\uD83E\uDD56\uD83D\uDE89\uD83F\uDC71\uD83D\uDC5E\uD83F\uDC17\uD83C\uDE41\uD83E\uDEB9\uD83F\uDDE2\uD83E\uDDD7\uD83F\uDD95\uD83D\uDC34\uD83F\uDCF4\uD83E\uDF4C\uD83E\uDFB7\uD83D\uDF6A\uD83C\uDFA9\uD83D\uDF73\uD83F\uDC7D\uD83D\uDD49\uD83F\uDEBE\uD83D\uDF22\uD83C\uDE96\uD83E\uDE58\uD83E\uDF61\uD83D\uDD60\uD83F\uDE58\uD83F\uDCC9\uD83D\uDD5B\uD83C\uDD1D\uD83D\uDEAA\uD83D\uDFA7\uD83E\uDED5\uD83C\uDEA6\uD83F\uDD47\uD83C\uDDEF\uD83F\uDC27\uD83F\uDC90\uD83D\uDECF\uD83E\uDF6A\uD83E\uDC4F\uD83C\uDEAD\uD83E\uDD16\uD83F\uDE79\uD83F\uDCCC\uD83E\uDD51\uD83D\uDC49\uD83E\uDC12\uD83F\uDC9F\uD83E\uDCB3\uD83F\uDD4A\uD83E\uDCA0\uD83E\uDCFF\uD83C\uDD68"+
						"\uD83C\uDE80\uD83F\uDD82\uD83F\uDC21\uD83E\uDE82\uD83C\uDE59\uD83F\uDDA0\uD83E\uDC5C\uD83D\uDD35\uD83F\uDD1C\uD83E\uDF87\uD83F\uDE32\uD83D\uDCD2\uD83F\uDEA2\uD83D\uDC11\uD83E\uDE52\uD83C\uDE51\uD83E\uDC1B\uD83F\uDD55\uD83D\uDDEB\uD83C\uDDE6\uD83E\uDFEE\uD83E\uDE05\uD83C\uDF6A\uD83D\uDD18\uD83C\uDE2A\uD83C\uDE4D\uD83E\uDE4F\uD83C\uDE10\uD83E\uDD5C\uD83F\uDE83\uD83F\uDD51\uD83D\uDC14\uD83D\uDD8D\uD83D\uDE47\uD83F\uDC6D\uD83C\uDE4B\uD83F\uDDB4\uD83F\uDD42\uD83D\uDF7A\uD83E\uDCEE\uD83D\uDDF1\uD83E\uDCAF\uD83E\uDE24\uD83C\uDE5C\uD83F\uDD3F\uD83F\uDE7C\uD83E\uDCB5\uD83D\uDD7D\uD83D\uDDD5\uD83E\uDC9E\uD83C\uDCC2\uD83F\uDC07\uD83F\uDD3E\uD83D\uDEBD\uD83D\uDED6\uD83E\uDF74"+
						"\uD83E\uDDCA\uD83F\uDE86\uD83C\uDDF1\uD83E\uDE4F\uD83E\uDC31\uD83D\uDCB2\uD83D\uDCFE\uD83E\uDE6E\uD83C\uDD18\uD83F\uDCE0\uD83D\uDF47\uD83F\uDCFB\uD83F\uDED7\uD83D\uDC0C\uD83C\uDF08\uD83C\uDD12\uD83C\uDFA0\uD83F\uDC8A\uD83E\uDC3A\uD83D\uDF1C\uD83E\uDEC6\uD83D\uDEB7\uD83F\uDC47\uD83F\uDC63\uD83D\uDD6D\uD83E\uDEB7\uD83E\uDD20\uD83F\uDCFA\uD83D\uDF02\uD83F\uDCDF\uD83E\uDF1E\uD83F\uDC44\uD83D\uDE29\uD83D\uDFF3\uD83F\uDE47\uD83F\uDC7B\uD83C\uDC15\uD83C\uDC0F\uD83F\uDDF0\uD83E\uDCFF\uD83D\uDEF1\uD83F\uDC4C\uD83C\uDDFC\uD83F\uDCFD\uD83E\uDCB7\uD83D\uDF1D\uD83E\uDEDE\uD83F\uDC3A\uD83E\uDF4C\uD83C\uDEC9\uD83E\uDF48\uD83D\uDC48\uD83D\uDDAD"+
						"\uD83D\uDC16\uD83E\uDF46\uD83D\uDD30\uD83C\uDD30\uD83E\uDDAD\uD83D\uDF68\uD83E\uDF84\uD83C\uDCB4\uD83F\uDCDB\uD83D\uDF5E\uD83D\uDE31\uD83E\uDC05\uD83D\uDD7E\uD83E\uDF46\uD83F\uDE32\uD83D\uDF43\uD83F\uDDC5\uD83D\uDC38\uD83D\uDFC2\uD83D\uDFFF\uD83D\uDD90\uD83C\uDDD0\uD83E\uDF3C\uD83D\uDF19\uD83E\uDC9E\uD83D\uDEE9\uD83D\uDDA4\uD83E\uDDBB\uD83D\uDF8B\uD83F\uDCDF\uD83C\uDF8A\uD83D\uDFE3\uD83D\uDE56\uD83F\uDD36\uD83D\uDEC7\uD83C\uDE32\uD83D\uDC4B\uD83E\uDF36\uD83F\uDEB4\uD83D\uDC14\uD83C\uDDB9\uD83C\uDEC4\uD83E\uDE41\uD83C\uDD45\uD83C\uDC25\uD83F\uDDB1\uD83D\uDE25\uD83C\uDD0B\uD83D\uDE7F\uD83F\uDCE8\uD83E\uDFD9\uD83F\uDEC4"+
						"\uD83D\uDC6C\uD83E\uDDA1\uD83E\uDF23\uD83E\uDDB1\uD83F\uDD0C\uD83F\uDEB2\uD83C\uDE48\uD83E\uDC99\uD83F\uDCA6\uD83C\uDC87\uD83D\uDE68\uD83E\uDDB8\uD83C\uDF91\uD83F\uDC19\uD83F\uDD31\uD83C\uDC7C\uD83C\uDFBD\uD83C\uDFF7\uD83E\uDEA7\uD83F\uDC92\uD83F\uDCB9\uD83F\uDE01\uD83C\uDDEF\uD83E\uDFEA\uD83F\uDDCB\uD83D\uDDD3\uD83E\uDE3A\uD83E\uDD09\uD83F\uDDF9\uD83D\uDCFC\uD83E\uDD05\uD83D\uDC86\uD83C\uDC8A\uD83F\uDC8F\uD83D\uDCE6\uD83C\uDE13\uD83E\uDC48\uD83C\uDF8C\uD83F\uDC38\uD83C\uDD18\uD83D\uDF44\uD83C\uDF3C\uD83F\uDE60\uD83E\uDE1B\uD83F\uDD6B\uD83D\uDFC5\uD83F\uDDD7\uD83D\uDF7F\uD83F\uDE8E\uD83D\uDE12"+
						"\uD83D\uDDA0\uD83C\uDE4E\uD83C\uDE5D\uD83E\uDCDA\uD83E\uDFDA\uD83F\uDD26\uD83E\uDC56\uD83C\uDE51\uD83D\uDEC5\uD83F\uDDD7\uD83E\uDF5F\uD83F\uDD7F\uD83E\uDC59\uD83D\uDF93\uD83E\uDFBA\uD83C\uDCD3\uD83C\uDCDD\uD83D\uDD79\uD83F\uDE75\uD83D\uDFDB\uD83E\uDEB5\uD83E\uDC2D\uD83E\uDD2F\uD83E\uDFCE\uD83C\uDFAD\uD83F\uDC54\uD83F\uDC80\uD83D\uDD94\uD83E\uDDE9\uD83D\uDC7F\uD83F\uDC60\uD83F\uDEFB\uD83C\uDDF3\uD83E\uDD70\uD83F\uDCFE\uD83D\uDD1C\uD83E\uDE34\uD83D\uDD69\uD83C\uDD4F\uD83C\uDFCF\uD83F\uDC8A\uD83E\uDFC9\uD83E\uDFD6\uD83F\uDE95\uD83E\uDE78\uD83C\uDD39\uD83E\uDC21\uD83E\uDFB1\uD83C\uDD1A\uD83F\uDCB0\uD83C\uDC6A\uD83D\uDD47\uD83E\uDCEC\uD83F\uDE6B"+
						"\uD83E\uDD9F\uD83E\uDF80\uD83E\uDE39\uD83C\uDD54\uD83D\uDC5B\uD83C\uDC13\uD83C\uDCC6\uD83F\uDD99\uD83C\uDE32\uD83F\uDC92\uD83C\uDD18\uD83C\uDDEE\uD83D\uDF14\uD83D\uDC0E\uD83C\uDD95\uD83C\uDEDD\uD83D\uDEBF\uD83E\uDE31\uD83C\uDD16\uD83D\uDCBA\uD83E\uDDA4\uD83C\uDC5B\uD83F\uDD5F\uD83F\uDE27\uD83E\uDC02\uD83E\uDE05\uD83E\uDD90\uD83E\uDD65\uD83F\uDDFF\uD83F\uDE27\uD83E\uDD2C\uD83E\uDC1A\uD83F\uDD69\uD83F\uDC99\uD83D\uDC8C\uD83D\uDDF0\uD83C\uDC0E\uD83E\uDC39\uD83D\uDE5E\uD83E\uDD82\uD83D\uDD1A\uD83C\uDC94\uD83F\uDED7\uD83D\uDC56\uD83C\uDDFE\uD83F\uDC9D\uD83F\uDE27\uD83D\uDF2B\uD83D\uDDF1"+
						"\uD83F\uDCBB\uD83F\uDC7F\uD83E\uDFDC\uD83E\uDDD3\uD83E\uDF42\uD83D\uDFCB\uD83D\uDD61\uD83F\uDD7D\uD83E\uDEB8\uD83F\uDD03\uD83F\uDE13\uD83C\uDF56\uD83C\uDC5B\uD83E\uDD11\uD83E\uDF0C\uD83C\uDD3F\uD83E\uDFDA\uD83D\uDF56\uD83F\uDE55\uD83C\uDD52\uD83F\uDE41\uD83E\uDFE3\uD83F\uDED3\uD83E\uDCD5\uD83F\uDCD7\uD83C\uDD30\uD83D\uDC5B\uD83F\uDDBE\uD83F\uDC55\uD83F\uDE5C\uD83F\uDCF6\uD83E\uDEA2\uD83C\uDDAA\uD83E\uDD98\uD83D\uDFCB\uD83C\uDDC5\uD83D\uDCC8\uD83D\uDE0A\uD83E\uDE14\uD83F\uDCFF\uD83F\uDC68\uD83C\uDF56\uD83D\uDC72\uD83D\uDE8A\uD83E\uDD83\uD83D\uDC26\uD83E\uDDE5\uD83F\uDDC5\uD83E\uDE10\uD83F\uDE1E\uD83D\uDF8C\uD83E\uDED7\uD83E\uDE1B\uD83D\uDEC7"+
						"\uD83C\uDE09\uD83E\uDF4E\uD83D\uDFA2\uD83D\uDFBF\uD83D\uDFEB\uD83D\uDF9E\uD83C\uDF7A\uD83E\uDED2\uD83C\uDFB3\uD83F\uDD74\uD83E\uDFFC\uD83F\uDC3B\uD83E\uDEB8\uD83C\uDE8E\uD83C\uDD0B\uD83E\uDE83\uD83E\uDC66\uD83C\uDC79\uD83C\uDD67\uD83C\uDEDC\uD83D\uDF4A\uD83F\uDD2D\uD83C\uDE18\uD83C\uDD00\uD83C\uDC36\uD83E\uDCA0\uD83E\uDE0C\uD83C\uDE01\uD83F\uDD85\uD83C\uDD32\uD83D\uDF64\uD83E\uDF9B\uD83D\uDF20\uD83E\uDF0C\uD83E\uDC71\uD83F\uDD87\uD83D\uDD7D\uD83E\uDE2D\uD83C\uDD03\uD83D\uDD2D\uD83E\uDF33\uD83C\uDEBB\uD83E\uDD19\uD83C\uDECD\uD83D\uDCA3\uD83C\uDF47\uD83F\uDC63\uD83E\uDF16\uD83E\uDE84\uD83D\uDC54\uD83E\uDEB5\uD83E\uDCA7\uD83D\uDDDA"+
						"\uD83F\uDECA\uD83D\uDE83\uD83D\uDE2F\uD83D\uDD95\uD83C\uDC07\uD83D\uDE3D\uD83D\uDDBA\uD83D\uDCC1\uD83D\uDC28\uD83D\uDC96\uD83D\uDE01\uD83E\uDCBD\uD83D\uDF75\uD83C\uDD2B\uD83F\uDC2A\uD83F\uDCAD\uD83C\uDF44\uD83C\uDDDD\uD83E\uDC5B\uD83F\uDC2B\uD83D\uDD0E\uD83E\uDFDE\uD83C\uDD0D\uD83D\uDEE1\uD83F\uDC57\uD83C\uDF9B\uD83E\uDD5E\uD83D\uDF15\uD83F\uDC4D\uD83D\uDF0C\uD83D\uDFFF\uD83E\uDF48\uD83C\uDC45\uD83D\uDEC4\uD83E\uDC68\uD83E\uDE20\uD83C\uDDE9\uD83E\uDD27\uD83C\uDCFF\uD83E\uDEAD\uD83E\uDDB6\uD83E\uDC5A\uD83D\uDCBF\uD83C\uDEC1\uD83D\uDE31\uD83F\uDCF6\uD83C\uDCFD\uD83D\uDE99\uD83F\uDEB6\uD83F\uDEC7\uD83D\uDF5D"+
						"\uD83F\uDD60\uD83D\uDE71\uD83E\uDF85\uD83D\uDFB4\uD83F\uDE2B\uD83E\uDEF1\uD83E\uDD42\uD83C\uDDF8\uD83D\uDE35\uD83D\uDD9A\uD83E\uDC78\uD83E\uDFD0\uD83E\uDF80\uD83F\uDDD0\uD83D\uDD26\uD83D\uDD32\uD83C\uDF92\uD83C\uDE20\uD83F\uDD98\uD83E\uDFC4\uD83D\uDF26\uD83F\uDC57\uD83E\uDD67\uD83C\uDF54\uD83C\uDE42"),
				list("When the imposter is sus!", "\uD83D\uDE33"),
				list("When the imposter is sus!", "\uD83E\uDD7A"),
				list("When the imposter is sus!", "\uD83D\uDE24\uD83D\uDE24\uD83D\uDE24"),
				list("When the imposter is sus!", "\uD83E\uDD75\uD83E\uDD75\uD83D\uDE31\uD83D\uDE31\uD83D\uDE30\uD83D\uDE30\uD83D\uDE2D\uD83D\uDE37\uD83E\uDD25\uD83D\uDC1B"),
				namedList("\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02", "\uD83D\uDE33"));
	}

	@Test public void keywordTest(){
		simpleEqualityTest("keywordTest", namedList("pika", "pikachu", namedList("pikapika", "chu!!!!!!")));
	}

	@Test public void numberTest(){
		simpleEqualityTest("numberTest",
				value("01234"),
				value("01234.45678"),
				list("01234", "01234.45678"),
				list("-01234", "-01234.45678"),
				namedList("-1.toString"),
				namedList("-1.35234"),
				value("01234"),
				value("01234.45678"),
				namedList("+", "01234", "45678"),
				list(namedList("+", "01234"), namedList("+", "01234.45678")),
				list(namedList("-", "01234"), namedList("-", "01234.45678")),
				namedList("a5678", "01234"),
				namedList("45678", "0123a"),
				namedList("45678", "a1234"),
				namedList("!", "1234.45678"),
				namedList("-", "1234.45678"),
				namedList("45a78", "01234"),
				namedList("0", namedList("0.0", "0.0")),
				namedList("==",
						namedList("*", "3", namedList("-", "1")),
						namedList("-", "3")),
				namedList("==",
						namedList("*", "3", namedList("-", "1")),
						namedList("-", "3")),
				namedList("-", namedList("toString", "1", list())),
				namedList("-", namedList("1.35234")));
	}

	@Test public void crossRef(){
		simpleEqualityTest("crossRef",
				list("a", "b", "p5", "p6"),
				list(list(list("a"), list("b"), "p5", "p6")));
	}

	@Test public void json(){
		simpleEqualityTest("json1", object()
				.prop("glossary", object()
						.prop("title", "example glossary")
						.prop("GlossDiv", object()
								.prop("title", "S")
								.prop("GlossList",
										object().prop("GlossEntry", object()
												.prop("ID", "SGML")
												.prop("SortAs", "SGML")
												.prop("GlossTerm", "Standard Generalized Markup Language")
												.prop("Acronym", "SGML")
												.prop("Abbrev", "ISO 8879:1986")
												.prop("GlossDef", object()
														.prop("para", "A meta-markup language, used to create markup languages such as DocBook.")
														.prop("GlossSeeAlso", list("GML", "XML")))
												.prop("GlossSee", "markup"))))));
		simpleEqualityTest("json2", object()
				.prop("menu", object()
						.prop("id", "file")
						.prop("value", "File")
						.prop("popup", object()
								.prop("menuitem", list(
										object().prop("value", "New").prop("onclick", "CreateNewDoc()"),
										object().prop("value", "Open").prop("onclick", "OpenDoc()"),
										object().prop("value", "Close").prop("onclick", "CloseDoc()")
								)))));
		simpleEqualityTest("json3", object()
				.prop("menu", object()
						.prop("header", "SVG Viewer")
						.prop("items", list(
								object().prop("id", "Open"),
								object().prop("id", "OpenNew").prop("label", "Open New"),
								value("null"),
								object().prop("id", "ZoomIn").prop("label", "Zoom In"),
								object().prop("id", "ZoomOut").prop("label", "Zoom Out"),
								object().prop("id", "OriginalView").prop("label", "Original View"),
								value("null"),
								object().prop("id", "Quality"),
								object().prop("id", "Pause"),
								object().prop("id", "Mute"),
								value("null"),
								object().prop("id", "Find").prop("label", "Find..."),
								object().prop("id", "FindAgain").prop("label", "Find Again"),
								object().prop("id", "Copy"),
								object().prop("id", "CopyAgain").prop("label", "Copy Again"),
								object().prop("id", "CopySVG").prop("label", "Copy SVG"),
								object().prop("id", "ViewSVG").prop("label", "View SVG"),
								object().prop("id", "ViewSource").prop("label", "View Source"),
								object().prop("id", "SaveAs").prop("label", "Save As"),
								value("null"),
								object().prop("id", "Help"),
								object().prop("id", "About").prop("label", "About Adobe CVG Viewer...")
						))));
	}

	@Test public void drafts(){
		simpleEqualityTest("1",
				object().prop("Property", "Value")
						.prop("P2", "123")
						.prop("P3", "1 2 3")
						.prop("P4", "Except for this expression\nThe line break inbetween is a part of this text msg")
						.prop("More complex key", ":thanosdaddy:"));
		simpleEqualityTest("2",
				object().prop("Object", object())
						.prop("NamedObj", namedObject("Named"))
						.prop("List", list(1, 2, 3))
						.prop("NamedList", namedList("Named", 1, 2, 3))
						.prop("ObjectWithField", object().prop("1", 1).prop("2", 2).prop("3", 3)));
		simpleEqualityTest("3",
				object().prop("Operation",
								namedList("=",
										namedList("+", 1, 2),
										3))
						.prop("Op2",
								namedList(">", "a", "b"))
						.prop("Op3",
								namedList("=",
										namedList("^", "c", 2),
										namedList("+",
												namedList("^", "a", 2),
												namedList("^", "b", 2))))
						.prop("Op4", namedList("+",
								1,
								namedList("*",
										2,
										namedList("+", 3, 4))))
						.prop("InsanelyNestedOperation", "This is fine."));
		simpleEqualityTest("4",
				object()
						.prop("NamedOperation", namedList("Among", "Us"))
						.prop("2", namedList("=",
								namedList("fib", "a"),
								namedList("+",
										namedList("fib", namedList("-", "a", 2)),
										namedList("fib", namedList("-", "a", 1))))));
		simpleEqualityTest("5",
				object().prop("Team", list(
						namedObject("Pokemon")
								.prop("Name", "Pikachu")
								.prop("Type", list("Electric")))),
				object().prop("BatMan", list("bat", "man"))
						.prop("2", namedList("*",
								namedList("sqrt", 2),
								namedList("sqrt", 2)))
						.prop("16", namedList("*",
								namedList("*", 2, 2),
								namedList("*", 2, 2))),
				object().prop("That's a lot of X's", list("Indeed!", "Indeed!", "x")),
				object().prop("Fib1",
						namedList("+",
								namedList("fib", namedList("-", 1, 2)),
								namedList("fib", namedList("-", 1, 1)))),
				object().prop("Fib1", namedList("fib", 1)),
				object().prop("Vector1", object()
								.prop("X", 1).prop("Y", 2).prop("Z", 3))
						.prop("The Truth", 69420));
		simpleEqualityTest("6",
				object().prop("A", namedList("...", "a", "b"))
						.prop("B", namedList("eat", "shit")),
				object().prop("Operator", namedList("...", "amo", "gus"))
						.prop("Keyword", "great"));
		simpleEqualityTest("7",
				namedList("awsdsf", 1, 2, 3),
				namedList("=", namedList("+", 1, 2), 3),
				object().prop("key", "us"));
	}

	@Test public void associativity(){
		simpleEqualityTest("associativity",
				namedList("=", "a", namedList("=", "b", "c")),
				namedList("+", namedList("+", "a", "b"), "c"));
	}

	@Test public void fn(){
		simpleEqualityTest("fn",
				object().prop("Field", "abc").prop("Self", 1),
				object().prop("Field", "abc").prop("Self", 2),

				object().prop("Method", "abc").prop("A", 1).prop("B", 2).prop("Type", "Obj").prop("Self", "a"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 2).prop("Type", "Obj").prop("Self", "b"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 3).prop("Type", "Obj").prop("Self", "c"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 2).prop("Type", "List").prop("Self", "d"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 2).prop("Type", "List").prop("Self", "e"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 3).prop("Type", "List").prop("Self", "f"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 2).prop("Type", "Oper").prop("Self", "g"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 2).prop("Type", "Oper").prop("Self", "h"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 3).prop("Type", "Oper").prop("Self", "i"),
				object().prop("Method", "abc").prop("A", 1).prop("B", 4).prop("C", 16).prop("Type", "Oper").prop("Self", "j"),

				namedList("abc", 1),
				namedList("abc", 2)
		);
	}

	@Test public void eval(){
		simpleEqualityTest("eval",
				value("6.0"),
				value("true"),
				value("4.0"),
				value("true"),
				value("true"));
	}

	@Test public void collections(){
		simpleEqualityTest("collections",
				namedList("amog"),
				namedObject("amog"),
				namedList("us", "a", "m", "o", "g"),
				value("among"),
				list("a", "m", "o", "g", "u", "s"),
				object().prop("among", "us").prop("red", "sus"));
	}

	@Test public void fmt(){
		simpleEqualityTest("fmt",
				value("hi"),
				value("x:1, y:2"),
				value("x:1, y:2, xy:12"),
				value("x:1, y:2"),
				value("x:1, y:2"),
				value("x:1, y:2, xy:12"),
				value("x:1, y:2"),
				value("{}{}"),
				value("a {} {}"),
				value("a a a {}"),
				value("1"));
	}

	@Test public void operationNewline(){
		simpleEqualityTest("operation_newline",
				namedList("+", namedList("+", "a", "b"), "c"),
				namedList("+", namedList("+", "a", "b"), "c"),
				list("a", namedList("+", "b"), namedList("+", "c")));
	}

	private static void simpleEqualityTest(String name, Among... expected){
		try{
			assertArrayEquals(expected,
					TestUtil.make(TestUtil.expectSourceFrom("equality_tests", name))
							.root()
							.values()
							.toArray(new Among[0]));
		}catch(IOException ex){
			throw new UncheckedIOException(ex);
		}
	}
}
