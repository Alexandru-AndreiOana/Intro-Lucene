// Example from: http://lucenetutorial.com/lucene-in-5-minutes.html

package org.example;

import java.io.*;
import java.util.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;


public class Main {
    public static void main(String[] args) throws Exception {
        CustomRomanianAnalyzer analyzer = new CustomRomanianAnalyzer();

        // 1. create the index
        Directory index = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        // Replace the dictionary with text extracted from files
        String folderPath = "data/"; // Specify your folder path here
        File folder = new File(folderPath);

        // Extract the text from the files and add to index
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                Arrays.sort(files);

                for (File file : files) {
                    if (file.isFile()) {
                        String key = file.getName(); // File name as the key
                        String value = extractTextFromFile(file); // Extracted text from the file

                        // Add the document to the index
                        addDoc(w, key, value);
                    }
                }
            } else {
                throw new IllegalArgumentException("No files found in " + folderPath);
            }
        } else {
            throw new IllegalArgumentException("Provided path is not a valid directory" + folderPath);
        }

        w.close();

        // 2. query
        String querystr = args.length > 0 ? args[0] : "metodă";

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

    // Method to extract text from all documents inside a folder
    private static List<String> extractTextFromFolder(File folder) throws Exception {
        List<String> extractedTexts = new ArrayList<>();

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                // Sort files alphabetically by name to maintain the order
                Arrays.sort(files);

                for (File file : files) {
                    // Only process files (skip subdirectories)
                    if (file.isFile()) {
                        String extractedText = extractTextFromFile(file);
                        extractedTexts.add(extractedText);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Provided path is not a valid directory.");
        }

        return extractedTexts;
    }

    // Method to extract text from a single file
    private static String extractTextFromFile(File file) throws Exception {
        // Use Tika's AutoDetectParser for automatic format detection
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1); // Use -1 for unlimited text size

        try (InputStream input = new FileInputStream(file)) {
            parser.parse(input, handler, metadata);
        }

        return handler.toString();
    }
}