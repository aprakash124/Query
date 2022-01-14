
import java.io.*;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.LinkedList;
import org.jsoup.Jsoup;

public class index
{
	static Hashtable<String, Integer> doc_ht = new Hashtable<>(43542, 0.66f);
	static LPHashtable<String, LinkedList<double[]>> global_ht = new LPHashtable<>(480462, 0.66f);
	static Hashtable<String, String> stopword_ht = new Hashtable<>(2400, 0.66f);
	static boolean multiline_tag = false;
	static boolean style_tag = false;
	static int total = 0;
	static int uniq = 0;
	static int doc_total = 0;
	
	// Inserts from document hashtable to global hashtable
	public static void globalHTInsert(int doc)
	{
		Enumeration<String> keys = doc_ht.keys();
		
		while (keys.hasMoreElements())
		{
			String token = keys.nextElement();
			
			// Calculates rtf for term weight
			double rtf = (double)doc_ht.get(token)/total;
			
			double[] document_freq = new double[3];
			document_freq[0] = doc;
			document_freq[1] = doc_ht.get(token);
			document_freq[2] = rtf;
			
			if (!global_ht.containsKey(token))
			{
				LinkedList<double[]> postings = new LinkedList();
				postings.add(document_freq);
				global_ht.put(token, postings);
			}
			else
			{
				LinkedList<double[]> postings = (LinkedList<double[]>)global_ht.get(token);
				postings.add(document_freq);
				global_ht.put(token, postings);
			}
		}
	}
	
	// Creates dict and post file
	public static void createGlobalHTFiles(String outputDir)
	{
		try
		{
			int max = global_ht.getMax();
			int post_record = 0;
			FileWriter dict = new FileWriter(new File(outputDir, "dict"));
			FileWriter post = new FileWriter(new File(outputDir, "post"));
			
			for (int i = 0; i < max; i++)
			{
				if (global_ht.getKey(i) == null)
				{
					// Makes records for unused buckets fixed length
					dict.write(String.format("%-54s", "_empty") + "\n");
				}
				else
				{
					LinkedList<double[]> postings = (LinkedList<double[]>)global_ht.get((String)global_ht.getKey(i));
					String key = (String)global_ht.getKey(i);
					String num_docs = Integer.toString(postings.size());
					String start = Integer.toString(post_record);
					
					// Prevents low frequency words from going to dict and post file
					if (postings.size() == 1 && (int)postings.getFirst()[1] == 1)
					{
						// Creates records for low frequency words in dict and makes all fields fixed length
						key = String.format("%-39s", "_deleted");
						num_docs = String.format("%-3s", "");
						start = String.format("%-10s", "");
					}
					else
					{
						// Makes all fields in dict file fixed length
						key = String.format("%-39s", key);
						num_docs = String.format("%-3s", num_docs);
						start = String.format("%-10s", start);
						
						// Calculates idf for term weight
						double idf = 1 + Math.log10(doc_total/postings.size());
						
						// Puts fields into post file
						for (int j = 0; j < postings.size(); j++)
						{
							// Makes all fields in post file fixed length and makes all term weights integers ranged from 0 to 99999
							String doc_id = String.format("%-3s", (int)postings.get(j)[0]);
							String tf = String.format("%-4s", (int)postings.get(j)[1]);
							String tw = String.format("%-5s", (int)(postings.get(j)[2] * idf * 100000));
							
							post_record++;
							post.write(doc_id + " " + tf + " " + tw + "\n");
						}
					}
					
					// Puts fields into dict file
					dict.write(key + " " + num_docs + " " + start + "\n");
				}
			}
			
			dict.close();
			post.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error creating dict and post files.");
		}
	}
	
	// Creates tokens from a line
	public static void tokenize(String line)
	{	
		//If tag covers multiple lines, then searches for end of tag
		if (multiline_tag)
		{
			if (line.contains(">"))
			{
				//Makes sure line starts at the end of tag
				while (line.length() > 0 && line.charAt(0) != '>')
				{
					if (line.length() > 1)
						line = line.substring(1); 
					else
						line = "";
				}
					
				if (line.length() > 1)
					line = line.substring(1);
					
				multiline_tag = false;
			}
			else
				line = "";
		}
			
		//Checks if tag covers multiple lines
		if (line.contains("<") && line.lastIndexOf("<") > line.lastIndexOf(">"))
		{
			multiline_tag = true;
			line = line.substring(0, line.lastIndexOf("<"));
		}
			
		//Checks if style tag is used
		if (line.contains("<style"))
		{
			style_tag = true;
		}
		
		// Checks for the end of style tag
		if (style_tag)
		{
			if (line.contains("</style"))
			{
				line = line.substring(line.indexOf("</style"));
				style_tag = false;
			}
			else
			{
				line = "";
			}
		}
		
		//Removes html tags from the line
		line = Jsoup.parse(line).text().replaceAll("[^a-zA-Z0-9.'-]", " ");
		StringTokenizer tokenizer = new StringTokenizer(line);
		while (tokenizer.hasMoreTokens())
		{	
			//Lowercases all tokens
			String token = tokenizer.nextToken().toLowerCase();
			
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
				
			//Only adds token to outfile if token length greater than 1 and if token is not a stopword
			if (token.length() > 1 && !stopword_ht.containsKey(token))
			{
				if (!doc_ht.containsKey(token))
				{
					doc_ht.put(token, 1);
					uniq++;
				}
				else
				{
					doc_ht.put(token, doc_ht.get(token)+1);
				}
				
				total++;
			}
		}
	}
	
	public static void main (String args[])
	{
		File  inputDir = null;
		File  outputDir = null;
		PrintStream console = System.out;

		try
		{
			//Validate args\
			if(args.length != 2)
				throw new Error ("Wrong number of arguments, expecting 2 received " + args.length);
			for (int i = 0; i <args.length; i++)
			{
				File f = new File(args[i]);
				if (!f.isDirectory())
				{
					throw new Error ("Not a valid directory in args " + i);
				}
				else
				{
					if(i == 0)
						inputDir = f; //Store user input for Input directory
					else
						outputDir = f; //Store user input for Output directory
				}
			}
			
			File[] listOfInputs = inputDir.listFiles(); // Array of Files (documents we will tokenize and index)
			doc_total = listOfInputs.length;
			
			BufferedReader infile = new BufferedReader(new FileReader("stopwords.txt"));
			FileWriter map = new FileWriter(new File(args[1], "map"));
			
			String stopword = infile.readLine();
			
			//Puts all stopwords into a hashtable
			while (stopword != null)
			{
				stopword_ht.put(stopword, "");
				stopword = infile.readLine();
			}
			
			infile.close();
			
			
			int totalTokens = 0;
			int current_doc = 0;
			
			// Loops through all files in input directory
			for(File file : listOfInputs)
			{
				if(file.isFile())
				{	
					infile = new BufferedReader(new FileReader(file));
					
					// Formats map records to have a fixed length of 11
					String map_entry = String.format("%-11s", file.getName());
					map.write(map_entry + "\n");
					
					String line = infile.readLine();
					
					total = 0;
					uniq = 0;
					multiline_tag = false;
					
					while(line != null)
					{
						tokenize(line);
						line = infile.readLine();
					}
					
					// Used for testing purposes
					//System.out.println("Filename: " + file.getName() + " Total: " + total + " Unique: " + uniq);
					
					totalTokens += total;
					globalHTInsert(current_doc);
					current_doc++;
					
					infile.close();
					doc_ht.clear();
				}
			}
			
			map.close();
			
			createGlobalHTFiles(args[1]);

		}
		catch (Exception e)
		{
			System.out.println("Exception");
			System.out.println(e.getMessage());
			System.out.println("StackTrace: ");
			e.printStackTrace();
		}
	}
}
