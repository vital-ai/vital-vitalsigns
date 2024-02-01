package ai.vital.lucene.model;

import java.io.IOException;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.util.Version;

/**
 * An Analyzer that uses {@link WhitespaceTokenizer}.
 * <p>
 * <a name="version">You must specify the required {@link Version} compatibility
 * when creating {@link CharTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link WhitespaceTokenizer} uses an int based API to normalize
 * and detect token codepoints. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * <p>
 **/
public final class VitalWhitespaceAnalyzer extends Analyzer {

    public final static String STOPWORD_PLACEHOLDER = "_STOP_";
    public final static String STOPWORD_PLACEHOLDER_LC = "_stop_";

    private final static Set<String> stopwords = new HashSet<String>(
            Arrays.asList(STOPWORD_PLACEHOLDER, STOPWORD_PLACEHOLDER_LC));

    private final Version matchVersion;
    
    public static boolean isStopword(String w) {
        return stopwords.contains(w);
    }
    
    public static PhraseQuery createPhraseQuery(String field, String text) throws IOException {
        VitalWhitespaceAnalyzer analyzer = new VitalWhitespaceAnalyzer(Version.LUCENE_47);
        PhraseQuery pq = new PhraseQuery();
        TokenStream ts = analyzer.tokenStream(field, text);
        ts.reset();
        int index = -1;
        while(ts.incrementToken()) {
            String token = ts.getAttribute(CharTermAttribute.class).toString();
            int increment = ts.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
            index += increment;
            pq.add(new Term(field, token), index);
        }
        ts.close();
        return pq;
    }

    /**
     * Creates a new {@link WhitespaceAnalyzer}
     * 
     * @param matchVersion
     *            Lucene version to match See
     *            {@link <a href="#version">above</a>}
     */
    public VitalWhitespaceAnalyzer(Version matchVersion) {
        this.matchVersion = matchVersion;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName,
            final Reader reader) {
        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(matchVersion,
                reader);
        StopFilter filtered = new StopFilter(Version.LUCENE_47, tokenizer,
                new CharArraySet(Version.LUCENE_47, stopwords, false));
        return new TokenStreamComponents(tokenizer, filtered);
    }

    @Override
    public int getPositionIncrementGap(String fieldName) {
        return 100;
    }
}
