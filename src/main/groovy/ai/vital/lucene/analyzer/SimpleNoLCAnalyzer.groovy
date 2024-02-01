package ai.vital.lucene.analyzer;
import java.io.Reader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.util.Version;

/** An {@link Analyzer} that uses {@link LetterTokenizer} 
 *  No lower-case fileter is involved */

public final class SimpleNoLCAnalyzer extends Analyzer {
//  @Override
//  public TokenStream tokenStream(String fieldName, Reader reader) {
//    return new LetterTokenizer(reader);
//  }
//
//  @Override
//  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
//    Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
//    if (tokenizer == null) {
//      tokenizer = new LetterTokenizer(reader);
//      setPreviousTokenStream(tokenizer);
//    } else
//      tokenizer.reset(reader);
//    return tokenizer;
//  }

	@Override
	protected TokenStreamComponents createComponents(String arg0, Reader arg1) {

		TokenStreamComponents components = new TokenStreamComponents(new LetterTokenizer(Version.LUCENE_47, arg1));
		return components;
	}
	
	
}