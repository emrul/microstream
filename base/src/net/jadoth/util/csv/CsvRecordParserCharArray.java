package net.jadoth.util.csv;

import net.jadoth.chars.EscapeHandler;
import net.jadoth.chars.XChars;
import net.jadoth.chars.VarString;
import net.jadoth.functional._charRangeProcedure;

public interface CsvRecordParserCharArray
{
	/* (01.10.2014 TM)TODO: CsvRecordParserCharArray architecture improvement
	 * Test if stack-allocated CsvRecordParserCharArray instances have comparable performance
	 * if all the control characters are implemented as fields instead of method parameters.
	 */

	public int parseRecord(
		char[]              input          ,
		int                 iStart         ,
		int                 iBound         ,
		char                valueSeparator ,
		char                delimiter      ,
		char                escaper        ,
		char                recordSeparator,
		char                terminator     ,
		CsvConfiguration    config         ,
		VarString           literalBuilder ,
		EscapeHandler       escapeHandler  ,
		_charRangeProcedure valueCollector
	);



	public final class Static
	{
		public static boolean isTrailingSeparator(
			final char[] input    ,
			final int    iLowBound,
			final int    index    ,
			final char   separator
		)
		{
			int i = index;
			while(i-- > iLowBound)
			{
				if(input[i] == separator)
				{
					return true;
				}
				if(XChars.isNonWhitespace(input[i]))
				{
					return false;
				}
			}
			return false;
		}

		public static final int skipSkippable(
			final char[]           input         ,
			final int              iStart        ,
			final int              iBound        ,
			final char             commentStarter,
			final CsvConfiguration config
		)
		{
			final int lastCharIndex = iBound - 1;

			int i = iStart;

			// skip any number and combination of whitespaces, simple and full comments
			while(i < iBound)
			{
				if(XChars.isWhitespace(input[i]))
				{
					i++;
				}
				else if(input[i] != commentStarter)
				{
					return i; // any non-comment starter non-whitespace must be the start of a non-skippable, so return.
				}
				else if(i >= lastCharIndex)
				{
					return i; // nasty special case: single trailing comment signal char (but no comment) at the end
				}
				else if(input[i + 1] == config.commentSimpleStarter())
				{
					i = skipCommentSimple(input, i + 2, iBound, config.lineSeparator());
				}
				else if(input[i + 1] == config.commentFullStarter())
				{
					// repeated array instantiation locally, but only if actually needed.
					final char[] commentFullTerminator = config.commentFullTerminatorArray();
					i = skipCommentFull(input, i + 2, iBound, commentFullTerminator[0], commentFullTerminator);
				}
				else
				{
					return i - 1; // no comment at all. No reason to freak out and throw an exception, just return.
				}
			}

			return iBound;
		}

		public static final int skipDataComments(
			final char[]           input          ,
			final int              iStart         ,
			final int              iBound         ,
			final char             terminator     ,
			final char             valueSeparator ,
			final char             recordSeparator,
			final char             commentSignal  ,
			final CsvConfiguration config
		)
		{
			int i = iStart;

			// skip white spaces and do a quick check for comment start symbol before setting up all the symbol stuff
			while(i < iBound)
			{
				// check control chars explicitly before skipping whitespaces as they might be whitespaces themselves
				if(input[i] == terminator || input[i] == valueSeparator || input[i] == recordSeparator)
				{
					return i;
				}

				// skip all non-control white spaces before, between and after comments
				if(XChars.isWhitespace(input[i]))
				{
					// case whitespace, skip and continue
					i++;
					continue;
				}
				else if(input[i] != config.commentSignal())
				{
					// not a comment, abort before performing more stuff
					return i;
				}

				// nasty special case: single trailing comment signal character at the end
				if(++i >= iBound || input[i] == terminator || input[i] == valueSeparator || input[i] == recordSeparator)
				{
					return i - 1;
				}
				else if(input[i] == commentSignal)
				{
					i = skipCommentSimple(input, i + 1, iBound, config.lineSeparator());
				}
				else if(input[i] == config.commentFullStarter())
				{
					// repeated array instantiation locally, but only if actually needed.
					final char[] commentFullTerminator = config.commentFullTerminatorArray();
					i = skipCommentFull(input, i + 1, iBound, commentFullTerminator[0], commentFullTerminator);
				}
			}

			// reached bound, loops terminated
			return i;
		}

		public static final int skipCommentSimple(
			final char[] input     ,
			final int    iStart    ,
			final int    iBound    ,
			final char   terminator
		)
		{
			int i = iStart;
			while(i < iBound)
			{
				if(input[i] == terminator)
				{
					return i;
				}

				/*
				 * A simple comment is defined as being terminated by a (ascii) line end (not a record separator),
				 * so no configuration.
				 * Simplified check for just \n is intentional. It covers both standard/java/unix
				 * and windows's clumsy \r\n.
				 * Older Mac OS, with their moronic idea of NOT using a new line symbol to indicate a new line
				 * just to be different from everyone else can go to overpriced iHell. At least they fixed it in OS X.
				 */
				if(input[i] == '\n')
				{
					return i + 1;
				}

				// skip comment content character
				i++;
			}

			// reached bound
			return i;
		}

		public static final int skipCommentFull(
			final char[] input                     ,
			final int    iStart                    ,
			final int    iBound                    ,
			final char   commentFullTerminatorFirst,
			final char[] commentFullTerminator
		)
		{
			int i = iStart;
			while(i < iBound)
			{
				// run until comment terminator is found
				if(input[i] == commentFullTerminatorFirst && containsString(input, i, iBound, commentFullTerminator))
				{
					return i + commentFullTerminator.length;
				}

				// skip comment content character
				i++;
			}

			// (22.11.2014)EXCP: proper exception
			throw new RuntimeException("Incomplete trailing full comment at index " + i);
		}

		private static boolean containsString(
			final char[] input ,
			final int    iStart,
			final int    iBound,
			final char[] string
		)
		{
			for(int i = 0; i < string.length; i++)
			{
				if(iStart + i == iBound || input[iStart + i] != string[i])
				{
					return false;
				}
			}
			return true;
		}

		private Static()
		{
			// static only
			throw new UnsupportedOperationException();
		}
	}

	public interface Provider
	{
		public CsvRecordParserCharArray provideRecordParser();
	}

}