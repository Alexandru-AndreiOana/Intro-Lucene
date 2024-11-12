// Example from: http://lucenetutorial.com/lucene-in-5-minutes.html

package org.example;

import java.io.IOException;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;


public class Main {
    public static void main(String[] args) throws IOException, ParseException {
        CustomRomanianAnalyzer analyzer = new CustomRomanianAnalyzer();

        // 1. create the index
        Directory index = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        Map<String, String> dictionary = new HashMap<>();
        // Adding key-value pairs to the dictionary
        dictionary.put("Primul text", "Acesta este un exemplu de test in română");
        dictionary.put("Al doilea text", "Vorbim despre al doilea text și diacritice.");
        dictionary.put("Al treilea text", "Ceva simplu în al 3-lea text cu diacritice și cratimă.");

        // [DEBUG] For testing the analyzer
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            addDoc(w, key, value);

            List<String> tokens = getTokenizedSentences(analyzer, value);
            System.out.println("Tokens for \"" + key + "\": " + tokens);
        }

        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            addDoc(w, key, value);
        }

        w.close();
        // 2. query
        String querystr = args.length > 0 ? args[0] : "diacritică";

        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.
        Query q = new QueryParser("body", analyzer).parse(querystr);

        // 3. search
        int hitsPerPage = 5;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0; i<hits.length; i++) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + ". " + d.get("title"));
        }

        // reader can only be closed when there
        // is no need to access the documents anymore.
        reader.close();
    }

    private static void addDoc(IndexWriter w, String title, String body) throws IOException {
        Document doc = new Document();
        // Add doc title (no need for tokenization, we want to store its value)
        doc.add(new StringField("title", title, Field.Store.YES));

        // Add doc body (we want it tokenized, but no need to store its value)
        doc.add(new TextField("body", body, Field.Store.NO));
        w.addDocument(doc);
    }

    public static String removeDiacritics(String text) {
        // Normalize text to decompose diacritics
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        // Remove diacritics using a regex pattern
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public static List<String> getTokenizedSentences(Analyzer analyzer, String sentence) throws IOException {
        List<String> tokens = new ArrayList<>();
        TokenStream stream = analyzer.tokenStream(null, new StringReader(sentence));
        CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);

        stream.reset();
        while (stream.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        stream.end();
        stream.close();

        return tokens;
    }
}
