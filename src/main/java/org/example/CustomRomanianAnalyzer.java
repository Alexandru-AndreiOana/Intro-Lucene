package org.example;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.tartarus.snowball.ext.RomanianStemmer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;


public class CustomRomanianAnalyzer extends Analyzer {

    // Combine the default Romanian stopwords with custom stopwords
    private static final CharArraySet STOP_WORDS;

    static {
        // Get the default Romanian stopword set
        CharArraySet defaultStopwords = RomanianAnalyzer.getDefaultStopSet();
        CharArraySet stopwords = new CharArraySet(defaultStopwords, true);

        // Add additional stopwords
        ArrayList<String> stopWordsList = new ArrayList<>(List.of("si"));
        CharArraySet additionalStopWords = new CharArraySet(stopWordsList, true);

        // Create a new CharArraySet with both default and additional stopwords
        stopwords.addAll(additionalStopWords);

        // Create a new CharArraySet for STOP_WORDS with diacritics removed
        STOP_WORDS = new CharArraySet(stopwords.size(), true);
        for (Object obj : stopwords) {
            char[] charArray = (char[]) obj;  // Cast each element to char[]
            String word = new String(charArray);  // Convert char[] to String

            String wordWithoutDiacritics = removeDiacritics(word);
            STOP_WORDS.add(wordWithoutDiacritics);
        }

        System.out.println("After removing diacritics: " + STOP_WORDS);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // Initialize a tokenizer (using StandardTokenizer as an example)
        Tokenizer tokenizer = new StandardTokenizer();

        // Create a token stream and apply filters
        TokenStream tokenStream = new LowerCaseFilter(tokenizer);// Convert tokens to lowercase
        tokenStream = new ASCIIFoldingFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, STOP_WORDS);// Convert Romanian characters to ASCII
        tokenStream = new SnowballFilter(tokenStream, new RomanianStemmer());             // Apply Romanian stemming

        // Return the TokenStreamComponents with tokenizer and filtered TokenStream
        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        // Apply normalization filters for custom analyzer
        return new ASCIIFoldingFilter(new LowerCaseFilter(in));
    }

    // Function to remove diacritics
    private static String removeDiacritics(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}

