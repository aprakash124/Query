
import java.io.*;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import org.jsoup.Jsoup;

public class query 
{
	static ArrayList<String> query_terms = new ArrayList<String>();
	static Hashtable<Integer, Integer> acc = new Hashtable<>();
	static int dict_records = 480462;  
	static int acc_size = 0;
	
	// Creates tokens from a query
	public static void tokenize(String token)
	{
		//Removes most non-alphanumeric characters from query
		token = Jsoup.parse(token).text().replaceAll("[^a-zA-Z0-9.'-]", " ");
		StringTokenizer tokenizer = new StringTokenizer(token);
		while (tokenizer.hasMoreTokens())
		{	
			//Lowercases all tokens
			token = tokenizer.nextToken().toLowerCase();
			
			//Only allows for starting character to be alphanumeric
			while (token.length() > 0 && !Character.isDigit(token.charAt(0)) && !Character.isLetter(token.charAt(0)))
			{
				if (token.length() > 1)
					token = token.substring(1);
				else
					token = "";
			}
			
			//Only allows for ending character to be alphanumeric
			while (token.length() > 0 && !Character.isDigit(token.charAt(token.length()-1)) && !Character.isLetter(token.charAt(token.length()-1)))
			{
				token = token.substring(0, token.length()-1);
			}
			
			//Only adds token to list of query terms if token is not empty
			if (!token.equals(""))
			{
				query_terms.add(token);
			}
		}
	}
	
	// Returns the hashcode of a key
	public static int hash(Object key)
	{
		return Math.abs(key.hashCode() % dict_records);
	}
	
	// Gets the record in dict based on a location
	public static String getDictRecord(RandomAccessFile dict, int loc) throws IOException
	{
		String record = "";
		if ((loc >= 0) && (loc < dict_records))
	    {
			dict.seek(0);
	        dict.skipBytes(loc * 55);
	        record = dict.readLine();
	    }
		return record;
	}
	
	// Creates the accumulator
	public static void createAccumulator(RandomAccessFile dict)
	{
		int upper_bound = 0;
		try
		{
			for (int i = 0; i < query_terms.size(); i++)
			{
				boolean found = false;
				int loc = hash(query_terms.get(i));
				int index = loc;
				String record = getDictRecord(dict, loc);
				
				// Does linear probing until an empty record is reached
				do
				{
					record = getDictRecord(dict, index);
					if (record.substring(0, 39).trim().equals(query_terms.get(i)))
					{
						upper_bound+=Integer.parseInt(record.substring(40, 43).trim());
						found = true;
					}
					else
						index = (index + 1) % dict_records;
				}
				while(!found && !record.trim().equals("_empty") && index != loc);
			}
			
			// Sizes accumulator based on the upper bound
			acc_size = upper_bound * 3;
			acc = new Hashtable<Integer, Integer>(acc_size, .66f);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error creating accumulator");
		}
	}
	
	// Puts values into accumulator
	public static void putValues(RandomAccessFile dict, RandomAccessFile post)
	{
		try
		{
			for (int i = 0; i < query_terms.size(); i++)
			{
				boolean found = false;
				int loc = hash(query_terms.get(i));
				int index = loc;
				String dict_record = getDictRecord(dict, loc);
				
				// Does linear probing until an empty record is reached
				do
				{
					dict_record = getDictRecord(dict, index);
					if (dict_record.substring(0, 39).trim().equals(query_terms.get(i)))
					{ 
						//upper_bound+=Integer.parseInt(record.substring(40, 43).trim());
						int num_docs = Integer.parseInt(dict_record.substring(40, 43).trim());
						int start = Integer.parseInt(dict_record.substring(44).trim());
						int end = start + num_docs;
						
						// Gets term weights from post and inserts them into accumulator
						for (int j = start; j < end; j++)
						{
							post.seek(0);
					        post.skipBytes(j * 15);
					        String post_record = post.readLine();
					        int key = Integer.parseInt(post_record.substring(0, 3).trim());
					        int tw = Integer.parseInt(post_record.substring(9).trim());
					        
					        if (!acc.containsKey(key))
							{
								acc.put(key, tw);
							}
							else
							{
								acc.put(key, acc.get(key)+tw);
							}
						}
						
						found = true;
					}
					else
						index = (index + 1) % dict_records;
				}
				while(!found && !dict_record.trim().equals("_empty") && index != loc);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error putting values into accumulator");
		}
	}
	
	// Prints the top ranking 10 documents
	public static void printTopResults(RandomAccessFile map)
	{
		try
		{
			int[] top_weights = new int[Math.min(acc.size(), 10)];
			int[] top_documents = new int[Math.min(acc.size(), 10)];
			Enumeration<Integer> keys = acc.keys();
			
			while (keys.hasMoreElements())
			{
				int key = keys.nextElement();
				int insert_loc = 0;
				int index = top_weights.length-1;
				int[] tmp_weights = top_weights.clone();
				int[] tmp_documents = top_documents.clone();
				
				while (index >= 0 && top_weights[index] < acc.get(key))
				{
					index--;
				}
				
				insert_loc = index + 1;
				
				// Sorts document array based on term weight
				if (insert_loc < top_weights.length)
				{
					for (int i = 0; i < top_weights.length; i++)
					{
						if (i < insert_loc)
						{
							tmp_weights[i] = top_weights[i];
							tmp_documents[i] = top_documents[i];
						}
						else if (i > insert_loc)
						{
							tmp_weights[i] = top_weights[i-1];
							tmp_documents[i] = top_documents[i-1];
						}
						else
						{
							tmp_weights[insert_loc] = acc.get(key);
							tmp_documents[insert_loc] = key;
						}
					}
					
					top_weights = tmp_weights.clone();
					top_documents = tmp_documents.clone();
				}
			}
			
			System.out.println("<table border=\"0\">");
			System.out.println("<tr><th>Document</th><th>Match Value</th></tr>");
			System.out.println("<tr><th><hr></th><th><hr></th></tr>");
			
			for (int i = 0; i < top_documents.length; i++)
			{
				map.seek(0);
		        map.skipBytes(top_documents[i] * 12);
		        String filename = map.readLine().trim();
				System.out.println("<tr><td><a href = \"http://www.csce.uark.edu/~alprakas/information_retrieval/input_files300/" 
		        + filename + "\">" + filename + "</a><td>" + top_weights[i] + "</td></tr>");
			}
			
			System.out.println("</table>");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error getting top ranking documents");
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
			// Arguments must be at least 2 to allow for at least one query term and the directory for dict, post, and map
			if(args.length < 2)
				throw new Error ("Wrong number of arguments, expecting at least 2 received " + args.length);
			
			// Checks if last argument is a directory
			File dir = new File(args[args.length-1]);
			
			if (!dir.isDirectory())
				throw new Error ("Not a valid directory in last argument");
			
			// Checks if directory contains dict, post, and map
			boolean contains_files = new File(dir, "dict").exists() && new File(dir, "post").exists() && 
					new File(dir, "map").exists();
			
			if (!contains_files)
				throw new Error ("Not all necessary files exist in directory");
			
			// Tokenizes the strings given as arguments
			for (int i = 0; i < args.length-1; i++)
			{
				tokenize(args[i]);
			}
			
			// Changes number of dict records if dict is bigger than expected
			dict_records = Math.max((int)(new File(dir, "dict").length())/55, dict_records);
			
			RandomAccessFile dict = new RandomAccessFile(args[args.length-1] + "/dict", "r");
			RandomAccessFile post = new RandomAccessFile(args[args.length-1] + "/post", "r");
			RandomAccessFile map = new RandomAccessFile(args[args.length-1] + "/map", "r");
			
			createAccumulator(dict);
			
			if (acc_size == 0)
			{
				System.out.println("No results were found.");
			}
			else
			{
				System.out.println("Showing top results.");
				System.out.println("<br>");
				System.out.println("<br>");
				putValues(dict, post);
				printTopResults(map);
			}
			
			dict.close();
			post.close();
			map.close();
		}
		catch(Exception e)
		{
			System.out.println("Exception");
			System.out.println(e.getMessage());
			System.out.println("StackTrace: ");
			e.printStackTrace();
		}
	}
}
