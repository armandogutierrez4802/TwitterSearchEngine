package org.CS172;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
//import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
//import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.StringTokenizer;
import java.util.Scanner;

public class ServletClass extends HttpServlet {
	
	static class Page {
        String user;        // Username
        String tweet;     // Tweet Content
        String title;       // Title of URL webpage
        String date;        // Date posted
        String location;    //User location
        String hashtags;    // Hashtags


        Page(String user, String tweet, String title, String date, String location, String hashtags) {
            this.user = user;
            this.tweet = tweet;
            this.title = title;
            this.date = date;
            this.location = location;
            this.hashtags = hashtags;
        }

        public void outputVariables(){
            System.out.println("OutputVariables");
            System.out.println("username: " + user);
            System.out.println("tweet: " + tweet);
            System.out.println("pageTitle: " + title);
            System.out.println("date: " + date);
            System.out.println("location: " + location);
            System.out.println("hashtags: " + hashtags);
        }
    }
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String input = req.getParameter("userQuery");
		
		//Lucene Code
		Analyzer analyzer = new StandardAnalyzer();

        // Store the index in memory:
        Directory directory = new RAMDirectory();
        // To store an index on disk, use this instead:
        //Directory directory = FSDirectory.open("/tmp/test");
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        //Read in the parsed JSON objects from part 1
        Scanner scan = new Scanner(new File("/Users/armandogutierrez/Desktop/output.txt"));

        String next = "";
        String username = "";
        String tweet = "";
        String pageTitle = "";
        String date = "";
        String location = "";
        String hashtags = "";
        List<Page> pages = new ArrayList<Page>();
        Page p;

        while(scan.hasNext()){
            if(!next.equals("UsernameHandle")) {
                next = scan.next();
            }
            //System.out.print(next + " ");
            if(next.equals("UsernameHandle")){
                //System.out.println("I am in usernamehandle");
                next = scan.next();
                username = next;
                //System.out.println("username = " + username);
            }
            if(next.equals("TweetContents")){
                next = scan.next();
                //System.out.print(next + " ");
                while(!next.equals("PageTitle")){
                    tweet += next + " ";
                    next = scan.next();
                    //System.out.print(next + " ");
                }
            }
            if(next.equals("PageTitle")){
                next = scan.next();
                if(next.equals("None")){
                    //Do nothing or set pageTitle = next = "None".. or just leave it empty string
                    next = scan.next();
                }
                else{
                    while(!next.equals("UserPostDate")){
                        pageTitle += next + " ";
                        next = scan.next();
                        //System.out.print(next + " ");
                    }
                }
            }
            if(next.equals("UserPostDate")){
                date += scan.next() + " ";
                date += scan.next() + " ";
                date += scan.next() + " ";
                date += scan.next() + " ";
                date += scan.next() + " ";
                date += scan.next() + " ";
                //System.out.print("Date: " + date + " ");
                next = scan.next();
                //System.out.print(next + " ");
            }
            if(next.equals("UserLocation")){//May need to do some string manipulation here for the ranking
                next = scan.next();
                //System.out.print(next + " ");
                if(next.equals("None")){
                    //Do nothing or set location = next = "None".... or just leave it empty string
                    next = scan.next();
                }
                else{
                    while(!next.equals("UserHashtags")){
                        location += next + " ";
                        next = scan.next();
                        //System.out.print(next + " ");
                    }
                }
            }
            if(next.equals("UserHashtags")){
                next = scan.next();
                //System.out.println(next + " ");
                if(next.equals("None")){
                    //Do nothing or set hashtags = next = "None"
                    //next = scan.next();
                }
                else{
                    while(!next.equals("UsernameHandle") && scan.hasNext()){
                        hashtags += next + " ";
                        next = scan.next();
                        //System.out.println(next + " ");
                    }
                    if(!scan.hasNext()){
                        hashtags += next + " ";
                    }
                }
                //Create page, output its contents, and add it to the pages list
                p = new Page(username, tweet, pageTitle, date, location, hashtags);
                //p.outputVariables();
                pages.add(p);
                //Reset the variables for the next page
                username = "";
                tweet = "";
                pageTitle = "";
                date = "";
                location = "";
                hashtags = "";
            }
        }//End while scan.hasNext()
        System.out.println();
        //System.out.println("SIZE OF PAGES IS " + pages.size());


        for (Page page: pages) {
            Document doc = new Document();
            doc.add(new TextField("username", page.user, Field.Store.YES));
            doc.add(new TextField("tweet", page.tweet, Field.Store.YES));
            doc.add(new TextField("pageTitle", page.title, Field.Store.YES));
            doc.add(new TextField("date", page.date, Field.Store.YES));
            doc.add(new TextField("location", page.location, Field.Store.YES));
            doc.add(new TextField("hashtags", page.hashtags, Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.close();


        // Now search the index:
        DirectoryReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);


        String[] fields = {"username", "tweet", "pageTitle", "date", "location", "hashtags"};
        Map<String, Float> boosts = new HashMap<String, Float>();
        //Establish the ranking algorithm by setting the boosts on each field here?????????
        boosts.put(fields[0], 0.25f);//Username
        boosts.put(fields[1], 0.35f);//Tweet
        boosts.put(fields[2], 0.20f);//Page Title
        boosts.put(fields[3], 0.15f);//Date
        boosts.put(fields[4], 0.05f);//Location
        boosts.put(fields[5], 0.05f);//Hashtags
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);
		        
        
        resp.getWriter().println("<html>");
		resp.getWriter().println("<head>");
		resp.getWriter().println("<title>Search Results</title>");
		resp.getWriter().println("</head>");
		resp.getWriter().println("<body>");
        
        
		Query query;

        try {
        	query = parser.parse(input);
        	int topHitCount = 20;
            ScoreDoc[] hits = indexSearcher.search(query, topHitCount).scoreDocs;
            resp.getWriter().println("Showing search results for: " + input);
    		resp.getWriter().println("<br>");
    		resp.getWriter().println("<br>");
            //Iterate through the results:
            for (int rank = 0; rank < hits.length; ++rank) {
            	Document hitDoc = indexSearcher.doc(hits[rank].doc);
            	resp.getWriter().println((rank + 1) + " (score:" + hits[rank].score + ") --> " +
                        hitDoc.get("username") + " - " + hitDoc.get("tweet") + " - " +
                         hitDoc.get("pageTitle") + " - " + hitDoc.get("date") + " - " + 
                         hitDoc.get("location") + " - " + hitDoc.get("hashtags"));
            	System.out.println((rank + 1) + " (score:" + hits[rank].score + ") --> " +
                        hitDoc.get("username") + " - " + hitDoc.get("tweet") + " - " +
                        hitDoc.get("pageTitle") + " - " + hitDoc.get("date") + " - " + 
                        hitDoc.get("location") + " - " + hitDoc.get("hashtags"));
            	resp.getWriter().println("<br>");
            	resp.getWriter().println("<br>"); 
            }
        }
        catch(ParseException e) {
        	System.out.println("ParseException Error");
        }
          
        indexReader.close();
        directory.close();
		
		
		resp.getWriter().println("</body>");
		resp.getWriter().println("</html>");
		
	}
	
}

