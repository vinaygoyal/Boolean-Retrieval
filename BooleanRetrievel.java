import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

/**
 * CSE535Assignment : Boolean Query Processing based on Postings Lists
 * 
 * @param Index
 *            File
 * @param Log
 *            File
 * @param Integer
 *            parameter to TopK
 * @param Query
 *            File
 * 
 *            CSE535Assignment term.idx output.log 10 query_file.txt
 * 
 * @return Nothing. Generates Log File(param2) file.
 * @exception FileNotFoundException
 *                On Input Error
 * @see FileNotFoundException
 * 
 * @author Vinay Goyal
 * @version 1.0
 * @since 10-19-2015
 */
public class BooleanRetrieval {

	// Array List to store terms in the Index File
	static ArrayList<Term> myTerms = new ArrayList<Term>();

	/*
	 * Main function to fetch data from index file and generate result in log
	 * file using query file and parameter 3 for TopK query.
	 */
	public static void main(String argv[]) throws IOException {

		if (argv.length != 4) {
			System.err
					.println("Invalid command line, exactly four arguments required.");
			System.exit(1);
		}

		String content = new String();
		File index_file = new File(argv[0]);
		File input_file = new File(argv[3]);
		/* To direct output to log file(param2) */
		 PrintStream ps=new PrintStream(new BufferedOutputStream(new
		 FileOutputStream(argv[1])), true);
		 System.setOut(ps);

		/* Create data structure to store the inverted index file */
		try {
			Scanner sc = new Scanner(new FileInputStream(index_file));
			while (sc.hasNextLine()) {
				content = sc.nextLine();
				String[] splitcon = content.split(Pattern.quote("\\"));
				Term e = new Term();
				e.term = splitcon[0];
				e.docfreq = Integer.parseInt(splitcon[1].replaceAll("c", ""));
				String subcon = splitcon[2].replaceAll("m", "")
						.replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll(" ", "");
				String[] splitsub = subcon.split(Pattern.quote(","));
				LinkedList<Post> list = new LinkedList<Post>();
				for (String s : splitsub) {
					String[] sub = s.split(Pattern.quote("/"));
					Post p = new Post();
					p.docId = sub[0];
					p.termFreq = Integer.parseInt(sub[1]);
					list.add(p);
				}
				e.posting = list;
				myTerms.add(e);
			}
			sc.close();
			/* Run TopK Query using param3 */
			 getTopN(Integer.parseInt(argv[2]));
			/* Read query file(param4) */
			sc = new Scanner(new FileInputStream(input_file));
			while (sc.hasNextLine()) {
				content = sc.nextLine();
				String[] splitcon = content.split(Pattern.quote(" "));
				/* Get postings for query terms */
				for (String s : splitcon) {
					getPostings(s);
				}
				/* Call the searches based on query */
				if (splitcon.length > 1) {
					termAtATimeQueryAnd(splitcon);
					termAtATimeQueryOr(splitcon);
					docAtATimeQueryAnd(splitcon);
					docAtATimeQueryOr(splitcon);
				}
			}
			sc.close();
		} catch (FileNotFoundException fnf) {
			fnf.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("\nProgram terminated Safely...");
		}

	}

	/*
	 * getTopK K: This returns the key dictionary terms that have the K largest
	 * postings lists. The result is expected to be an ordered string in the
	 * descending order of result postings, i.e., largest in the first position,
	 * and so on. The output should be formatted as follows (K=10 for an
	 * example) FUNCTION: getTopK 10 Result: term1, term2, term3..., term10 (list
	 * terms)
	 */
	public static void getTopN(int k) {
		int j = 0;
		String result = "";

		/* Sort terms */
		Collections.sort(myTerms, Term.DESCENDING_COMPARATOR);

		Iterator<Term> iterator = myTerms.iterator();

		/* Fetch topK from the sorted collection */
		while (iterator.hasNext() && j < k) {
			result += result.isEmpty() ? iterator.next().getTerm() : ", "
					+ iterator.next().getTerm();
			j++;
		}
		System.out.println("FUNCTION: getTopK " + k);
		System.out.println("Result: " + result);
	}

	/*
	 * getPostings query_term: Retrieve the postings list for the given query.
	 * This function executes N times, and output the postings for each term
	 * from both two different ordered postings list.
	 * 
	 * Output: FUNCTION: getPostings query_term Ordered by doc IDs: 100, 200,
	 * 300... (list of document IDs in ascending order) Ordered by TF: 300, 100,
	 * 200... (list the document IDs in decreasing term frequencies)
	 * 
	 * Displays ...term not found... if term is not in the index.
	 */
	public static void getPostings(String query_term) {
		int size = 0;
		Term term1 = new Term();
		/* Fetch the term object for the query */
		term1 = getQueryTerm(query_term);
		/* Get the size of the fetched term */
		size = term1.getPostingSize();
		System.out.println("FUNCTION : getPostings " + query_term);
		if (size == 0) {
			System.out.println("Term not found");
		} else if (size != 0) {
			/* Output sorted result */
			Collections.sort(term1.getPosting(), Post.ASCENDING_COMPARATOR);
			System.out
					.println("Ordered by doc IDs: " + term1.getPostingDocId());
			Collections.sort(term1.getPosting(), Post.DESCENDING_COMPARATOR);
			System.out.println("Ordered by TF: " + term1.getPostingDocId());
		}
	}

	/*
	 * getTerm query_term: Retrieve the term for the given query.
	 */
	private static Term getQueryTerm(String query_term) {
		String result = "";
		Iterator<Term> iterator = myTerms.iterator();
		Term term1 = new Term(); // For iteration
		Term term2 = new Term(); // Return term, empty if no match found
		while (iterator.hasNext()) {
			term1 = iterator.next();
			result = term1.getTerm();
			/* Match found */
			if (result.equals(query_term)) {
				term2 = term1;
				break;
			}
		}
		return term2;
	}

	/*
	 * docAtATimeQueryAnd query_term1, ..., query_termN: multi-term Boolean AND
	 * query on the index with document-at-a-time query. The index is ordered by
	 * increasing document IDs
	 * 
	 * Output:
	 * 
	 * FUNCTION: docAtATimeQueryAnd query_term1, ..., query_termN xx documents are
	 * found yy comparisons are made zz seconds are used Result: 100, 200, 300...
	 * (list the document IDs)
	 * 
	 * Display ...terms not found... if term is not in the index.
	 */
	public static void docAtATimeQueryAnd(String[] args) {
		int flag = 0; /* Count to check if all posting lists are traversed */
		int size = 0;
		int no_args = args.length;
		int smallest = -1; /* Smallest posting */
		int compares = 0; /* Comparisons counter */
		int commoncnt = 0; /* Result counter */
		String result = ""; /* Result */
		String doc = ""; /* docId with smallest posting */

		long lStartTime = new Date().getTime(); // Capture start time
		// Get postings
		ArrayList<LinkedList<String>> myPosts = new ArrayList<LinkedList<String>>();
		// Initialize pointers to postings
		Integer[] pointr = new Integer[no_args];

		for (int i = 0; i < no_args; i++) {
			size = getQueryTerm(args[i]).getPostingSize();
			if (size == 0) {
				result = "Term not found";
				size = 0;
				break;
			} else if (size > 0) {
				Collections.sort(getQueryTerm(args[i]).getPosting(),
						Post.ASCENDING_COMPARATOR);
				myPosts.add(getQueryTerm(args[i]).getPostingDocIdLL());
				pointr[i] = 0;
				/* Get the smallest list */
				if (i == 0
						|| size > getQueryTerm(args[i]).getPostingDocIdLL()
								.size()) {
					smallest = i;
					size = getQueryTerm(args[i]).getPostingDocIdLL().size();
				}
			}

		}

		if (size != 0) {
			/* Start with the smallest posting list */
			ListIterator<String> it = myPosts.get(smallest).listIterator(
					pointr[smallest]);
			/*
			 * Check if the elements of the smallest posting is present in rest
			 * using DAAT
			 */
			while (it.hasNext()) {
				doc = it.next().toString();
				flag = 1;
				for (int i = 0; i < no_args; i++) {
					if (i != smallest && flag > 0) { /* Skip compare to itself */
						Iterator<String> iti = myPosts.get(i).listIterator(
								pointr[i]);
						while (iti.hasNext()) {
							int equal = 2; /* Track matches */
							String doc1 = iti.next().toString();
							if (doc.equals(doc1)) {
								compares++;
								flag++;
								/*
								 * Increment current list pointer, smallest
								 * posting pointer will be handled below
								 */
								pointr[i]++;
								equal = 0;
							} else if (doc.compareTo(doc1) > 0) {
								compares++;
								equal = 1;
								/* Increment current list pointer */
								pointr[i]++;
							} else if (doc1.compareTo(doc) > 0) {
								compares++;
								flag = 0;
								/*
								 * Increment smallest list pointer and break as
								 * DOCID doesn't exist
								 */
								pointr[smallest]++;
								break;
							}
							/* Quit when match found */
							if (equal == 0) {
								break;
							}
						}
					}
				}
				/* When all lists have been traversed */
				if (flag == no_args) {
					result += result.isEmpty() ? doc : ", " + doc;
					commoncnt++;
					pointr[smallest]++; // Increment smallest posting pointer
				}
			}
		}
		long milsecs = new Date().getTime() - lStartTime;

		// Output result
		System.out.println("FUNCTION : docAtATimeQueryAnd "
				+ arrayToString(args));
		if (size != 0) {
			System.out.println(commoncnt + " documents are found ");
			System.out.println(compares + " comparisons are made ");
			System.out.println(milsecs + " milliseconds are used ");
			if (commoncnt == 0) {
				result = "No terms";
			}
		}

		System.out.println("Result : " + result);
	}

	/*
	 * docAtATimeQueryOr query_term1, ..., query_termN Multi-term Boolean OR query
	 * on the index with document-at-a-time query. The index is ordered by
	 * increasing document IDs
	 * 
	 * Output: FUNCTION: docAtATimeQueryOr query_term1, ..., query_termN xx
	 * documents are found yy comparisons are made zz seconds are used Result:
	 * 100, 200, 300... (list the document IDs)
	 * 
	 * Displays ...terms not found... if term is not in the index.
	 */
	public static void docAtATimeQueryOr(String[] args) {
		int flag = 0; /* Count to check if all posting lists are traversed */
		int size = 0;
		int no_args = args.length;
		int smallest = -1; /* Smallest posting */
		int compares = 0; /* Comparisons counter */
		int commoncnt = 0; /* Result counter */
		String result = ""; /* Result */
		String doc = ""; /* initial docId */
		String doc1 = ""; /* docId */
		String tmpdoc = ""; /* smallest docId */
		int exit_flag = no_args; /* Keeps counts of lists to compare */

		long lStartTime = new Date().getTime();
		// Get posting lists for query terms
		ArrayList<LinkedList<String>> myPosts = new ArrayList<LinkedList<String>>();
		Integer[] pointr = new Integer[no_args];
		for (int i = 0; i < no_args; i++) {
			size = getQueryTerm(args[i]).getPostingSize();
			if (size == 0) {
				result = "Term not found";
				size = 0;
				break;
			} else if (size > 0) {
				Collections.sort(getQueryTerm(args[i]).getPosting(),
						Post.ASCENDING_COMPARATOR);
				myPosts.add(getQueryTerm(args[i]).getPostingDocIdLL());
				pointr[i] = 0;
			}

		}

		smallest = 0;
		if (size != 0) {
			result = "";
			/* Till there are lists to compare */
			while (exit_flag > 0) {
				flag = 1;
				tmpdoc = "";

				/* Select the first list for comparison */
				/* Pointer = -1 means list has reached its end */
				for (int i = 0; i < no_args; i++) {
					if (pointr[i] != -1) {
						smallest = i;
						break;
					}
				}

				// Traverse through posting lists i and i+1
				for (int i = smallest, j = i + 1; i < no_args && j < no_args;) {

					/* -1 value represents end of the posting list */
					if (pointr[j] == -1) {
						j++;
						continue;
					}

					Iterator<String> iti = myPosts.get(i).listIterator(
							pointr[i]);
					Iterator<String> itj = myPosts.get(j).listIterator(
							pointr[j]);

					if (iti.hasNext()) {
						doc = iti.next().toString();
						doc = doc.replaceAll(" ", "");
					} else { // List hit end
						pointr[i] = -1; // Pointer -1
						exit_flag--; // Minus the list, no more elements in
						// list to compare
						break;
					}
					;
					if (itj.hasNext()) {
						doc1 = itj.next().toString();
						doc1 = doc1.replaceAll(" ", "");
					} else { // List hit end
						pointr[j] = -1; // Pointer -1
						exit_flag--; // Minus the list, no more elements in
										// list to compare
						break;
					}
					;
					if (tmpdoc == "") { // Initialize tmpdoc
						tmpdoc = doc;
						smallest = i;
					}
					if (tmpdoc.equals(doc1)) {
						compares++;
						flag++;
						pointr[j]++; // Increment j pointer, other pointer
										// handled below
						j++;
					} else if (tmpdoc.compareTo(doc1) > 0) {
						compares++;
						flag++;
						smallest = j; // smallest is j, not i now
						tmpdoc = doc1; // tmpdoc becomes docid of j
						j++;
					} else if (doc1.compareTo(tmpdoc) > 0) {
						compares++;
						flag++;
						j++;
					}

					// }

					if (flag == exit_flag) { // if no. of comparisons = lists to
												// compare
						result += result.isEmpty() ? tmpdoc : ", " + tmpdoc; // add
																				// to
																				// result
						pointr[smallest]++;// Increment smallest pointer
						flag = 1;
						commoncnt++;
						tmpdoc = "";
						j--; // j becomes more than no. of lists+1 after the
								// last comparison

						/* Check if any of the posting lists has reached its end */
						if (pointr[smallest] > myPosts.get(smallest).size()
								&& pointr[smallest] != -1) {
							pointr[smallest] = -1;
							exit_flag--;
						}
						;
						if (pointr[j] > myPosts.get(j).size()
								&& pointr[j] != -1 && smallest != j) {
							pointr[j] = -1;
							exit_flag--;
						}
						break;
					}

				}
				/*
				 * If only one list is left i.e. rest all have reached their end
				 * Nothing to compare hence add the remaining docids to result
				 */
				if (exit_flag == 1) {
					for (int i = 0; i < no_args; i++) {
						if (pointr[i] != -1
								&& pointr[i] < myPosts.get(i).size()) {
							Iterator<String> iti = myPosts.get(i).listIterator(
									pointr[i]);
							while (iti.hasNext()) {
								result = result + ", " + iti.next().toString();
								commoncnt++;
							}

						}
					}
					exit_flag--;
					break;
				}

			}
		}

		long milsecs = new Date().getTime() - lStartTime;

		// Output result
		System.out.println("FUNCTION : docAtATimeQueryOr "
				+ arrayToString(args));
		if (size != 0) {
			System.out.println(commoncnt + " documents are found ");
			System.out.println(compares + " comparisons are made ");
			System.out.println(milsecs + " milliseconds are used ");
			if (commoncnt == 0) {
				result = "No terms";
			}
		}
		System.out.println("Result : " + result);
	}

	/*
	 * termAtATimeQueryAnd query_term1, ..., query_termN: Multi-term Boolean AND
	 * query on the index with term-at-a-time query.
	 * 
	 * Output :FUNCTION: termAtATimeQueryAnd query_term1, ..., query_termN xx
	 * documents are found yy comparisons are made zz seconds are used nn
	 * comparisons are made with optimization Result: 100, 200, 300 ... (list the
	 * document IDs, re-ordered by docIDs)
	 * 
	 * Displays ...terms not found... if term is not in the index.
	 */
	public static void termAtATimeQueryAnd(String[] args) {

		int size = 0;
		int no_args = args.length;
		int compares = 0; /* Comparisons counter */
		int commoncnt = 0; /* Result counter */
		int optimization = 0;
		long milsecs = 0;
		String result = "";

		long lStartTime = new Date().getTime(); /* Capture start time */

		/* Get postings */
		ArrayList<LinkedList<String>> myPosts = new ArrayList<LinkedList<String>>();
		Integer[] pointr = new Integer[no_args];
		for (int i = 0; i < no_args; i++) {
			size = getQueryTerm(args[i]).getPostingSize();
			if (size == 0) {
				result = "Term not found";
				size = 0;
				break;
			} else if (size > 0) {
				myPosts.add(getQueryTerm(args[i]).getPostingDocIdLL());
				pointr[i] = 0;
			}
		}

		if (size != 0) {

			String[] res = new String[] {};
			// Call the function for TAATAnd that actually processes the query
			res = TAATAnd(myPosts, no_args);
			commoncnt = Integer.parseInt(res[0]);
			compares = Integer.parseInt(res[1]);
			milsecs = Integer.parseInt(res[2]);
			result = res[3];
			// Sort posts for optimization and call TAATAnd
			Collections.sort(myPosts, new customComparator());
			res = TAATAnd(myPosts, no_args);
			optimization = Integer.parseInt(res[1]);
		}
		milsecs = new Date().getTime() - lStartTime;

		// Output result
		System.out.println("FUNCTION : termAtATimeQueryAnd "
				+ arrayToString(args));
		if (size != 0) {
			System.out.println(commoncnt + " documents are found ");
			System.out.println(compares + " comparisons are made ");
			System.out.println(milsecs + " milliseconds are used ");
			System.out.println(optimization
					+ " comparisons are made with optimization");
			if (commoncnt == 0) {
				result = "No terms";
			}
		}
		System.out.println("Result : " + result);
	}

	/* Function that implements term at a time and */
	private static String[] TAATAnd(ArrayList<LinkedList<String>> myPosts,
			int no_args) { // this function is called for query optimization
		int flag = 0;
		int compares = 0; /* Comparisons counter */
		int commoncnt = 0; /* Result counter */
		String[] result = new String[4];

		List<String> answer = new ArrayList<String>(); /* Result */
		List<String> intermediate = new ArrayList<String>();

		long lStartTime = new Date().getTime(); /* Capture start time */
		/* Get postings */

		// Add first list to
		intermediate.addAll(myPosts.get(0));

		int equal = 0; /* track matches */
		String docid = ""; /* candidate of remove */

		for (int i = 1; i < no_args; i++) // for all lists after first
		{

			for (int a = 0; a < intermediate.size(); a++) {
				Iterator<String> it = myPosts.get(i).listIterator();
				docid = intermediate.get(a);
				equal = 0;
				while (it.hasNext()) {
					String next1 = (String) it.next();
					String next = next1.replaceAll(" ", "");
					compares++;
					if (docid.equals(next)) {
						equal = 1;
						flag++;
						break;
					}
				}
				// If no match, remove from intermediate
				if (equal == 0) {
					intermediate.remove(docid);
					a--;
				}
			}

		}
		if (flag > 0) {
			answer.addAll(intermediate);
		}
		commoncnt = answer.size();
		Collections.sort(answer);

		long milsecs = new Date().getTime() - lStartTime;
		// Return result
		result[0] = "" + commoncnt;
		result[1] = "" + compares;
		result[2] = "" + milsecs;
		result[3] = answer.toString().replaceAll("\\[", "")
				.replaceAll("\\]", "");

		return result;
	}

	/*
	 * termAtATimeQueryOr query_term1, ..., query_termN: Multi-term Boolean OR
	 * query on the index with term-at-a-time query.
	 * 
	 * Output FUNCTION: termAtATimeQueryOr query_term1, ..., query_termN xx
	 * documents are found yy comparisons are made zz seconds are used nn
	 * comparisons are made with optimization Result: 300, 100, 200... (list the
	 * document IDs, re-ordered by docIDs) Displays "terms not found" if it is
	 * not in the index.
	 */
	public static void termAtATimeQueryOr(String[] args) {
		int size = 0;
		int no_args = args.length;
		int compares = 0; /* Comparisons counter */
		int commoncnt = 0; /* Result counter */
		int optimization = 0;
		long milsecs = 0;
		String result = "";

		long lStartTime = new Date().getTime(); /* Capture start time */
		/* Get postings */

		ArrayList<LinkedList<String>> myPosts = new ArrayList<LinkedList<String>>();
		Integer[] pointr = new Integer[no_args];
		for (int i = 0; i < no_args; i++) {
			size = getQueryTerm(args[i]).getPostingSize();
			if (size == 0) {
				result = "Term not found";
				size = 0;
				break;
			} else if (size > 0) {
				myPosts.add(getQueryTerm(args[i]).getPostingDocIdLL());
				pointr[i] = 0;
			}
		}

		if (size != 0) {

			String[] res = new String[] {};
			res = TAATOr(myPosts, no_args);
			commoncnt = Integer.parseInt(res[0]);
			compares = Integer.parseInt(res[1]);
			milsecs = Integer.parseInt(res[2]);
			result = res[3];
			Collections.sort(myPosts, new customComparator());
			res = TAATOr(myPosts, no_args);
			optimization = Integer.parseInt(res[1]);
		}
		System.out.println("FUNCTION : termAtATimeQueryOr "
				+ arrayToString(args));
		milsecs = new Date().getTime() - lStartTime;

		if (size != 0) {
			System.out.println(commoncnt + " documents are found ");
			System.out.println(compares + " comparisons are made ");
			System.out.println(milsecs + " milliseconds are used ");
			System.out.println(optimization
					+ " comparisons are made with optimization");
			if (commoncnt == 0) {
				result = "No terms";
			}
		}
		System.out.println("Result : " + result);
	}

	/* Function that implements term at a time or */
	private static String[] TAATOr(ArrayList<LinkedList<String>> myPosts,
			int no_args) {
		int flag = 0;

		int compares = 0; /* Comparisons counter */
		int commoncnt = 0; /* Result counter */
		String[] result = new String[4];

		List<String> answer = new ArrayList<String>(); /* Result */
		List<String> intermediate = new ArrayList<String>();

		long lStartTime = new Date().getTime(); /* Capture start time */
		/* Get postings */

		intermediate.addAll(myPosts.get(0));

		int equal = 0; /* track matches */
		String docid = ""; /* candidate of remove */

		LinkedList<String> current = new LinkedList<String>();

		for (int i = 1; i < no_args; i++)
		// for all lists after first
		{
			current = myPosts.get(i);
			// To check if the element is present in intermediate list
			for (int a = 0; a < current.size(); a++) {
				{
					Iterator<String> it = intermediate.listIterator();
					docid = current.get(a).toString();
					equal = 0;
					while (it.hasNext()) {
						String next1 = (String) it.next();
						String next = next1.replaceAll(" ", "");
						compares++;
						// Do nothing if present
						if (docid.equals(next)) {
							equal = 1;
							flag++;
							break;
						}
					}
					// If not present, add to intermediate
					if (equal == 0) {
						intermediate.add(docid);
					}
				}

			}
		}
		// Add intermediate list to answer
		if (flag > 0) {
			answer.addAll(intermediate);
		}

		commoncnt = answer.size();
		Collections.sort(answer);

		long milsecs = new Date().getTime() - lStartTime;
		// Return result
		result[0] = "" + commoncnt;
		result[1] = "" + compares;
		result[2] = "" + milsecs;
		result[3] = answer.toString().replaceAll("\\[", "")
				.replaceAll("\\]", "");

		return result;
	}

	/* Function to convert array to string */
	private static String arrayToString(String array[]) {
		if (array.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; ++i) {
			sb.append(", ").append(array[i]);
		}
		return sb.substring(1);
	}

}

/**
 * Provides class to hold Term Objects and it's corresponding postings.
 * 
 */
class Term implements Comparable<Term> {
	String term;
	int docfreq;
	LinkedList<Post> posting;
	private String ll;
	private String linked;

	/* Returns the posting as a string */
	String getPostingAsString() {
		ll = "";
		for (int i = 0; i < this.posting.size(); i++) {
			ll += ll.isEmpty() ? this.posting.get(i).toString() : ", "
					+ this.posting.get(i).toString();
		}
		return ll;
	}

	/* Returns the posting DOCID as a string */
	String getPostingDocId() {
		ll = "";
		for (int i = 0; i < this.posting.size(); i++) {
			ll += ll.isEmpty() ? this.posting.get(i).getDocId() : ", "
					+ this.posting.get(i).getDocId();
		}
		return ll;
	}

	/* Returns the posting DOCID as linked list */
	LinkedList<String> getPostingDocIdLL() {
		LinkedList<String> doc = new LinkedList<String>();
		for (int i = 0; i < this.posting.size(); i++) {
			doc.add(this.posting.get(i).getDocId());
		}
		return doc;
	}

	/* Returns the posting term frequency as string */
	String getPostingTermFreq() {
		ll = "";
		for (int i = 0; i < this.posting.size(); i++) {
			ll += ll.isEmpty() ? this.posting.get(i).getTermFreq() : ", "
					+ this.posting.get(i).getTermFreq();
		}
		return ll;
	}

	/* Returns the posting linked list */
	LinkedList<Post> getPosting() {
		return posting;
	}

	/* Custom descending comparator to sort document frequency */
	public static final Comparator<Term> DESCENDING_COMPARATOR = new Comparator<Term>() {
		// Overriding the compare method
		@Override
		public int compare(Term t, Term t1) {
			return ((Integer) t1.docfreq).compareTo(t.docfreq);
		}
	};

	/* Overriding the toString function to return posting as string */
	@Override
	public String toString() {
		linked = this.getPostingAsString();
		return term + "/c" + docfreq + "/m[" + linked;
	}

	/* Return term */
	public String getTerm() {
		return term;
	}

	/* Return document frequency */
	public int getDocFreq() {
		return docfreq;
	}

	/* Return the size of the posting */
	public int getPostingSize() {
		if (posting != null) {
			return posting.size();
		} else {
			return 0;
		}
	}

	@Override
	public int compareTo(Term arg0) {
		// Auto-generated method stub
		return 0;
	}

}

/**
 * Provides class to hold a post
 * 
 */
class Post implements Comparable<Post> {
	String docId;
	int termFreq;

	/* Overriding the toString function to return post as string */
	@Override
	public String toString() {
		return docId + ":" + termFreq;
	}

	/* To return DOCID */
	public String getDocId() {
		return docId;
	}

	/* To return Term Frequency */
	public int getTermFreq() {
		return termFreq;
	}

	/* Custom descending comparator to sort term frequency */
	public static final Comparator<Post> DESCENDING_COMPARATOR = new Comparator<Post>() {
		// Overriding the compare method
		@Override
		public int compare(Post t, Post t1) {
			return ((Integer) t1.termFreq).compareTo(t.termFreq);
		}
	};
	/* Custom ascending comparator to sort DOCID */
	public static final Comparator<Post> ASCENDING_COMPARATOR = new Comparator<Post>() {
		// Overriding the compare method
		@Override
		public int compare(Post t, Post t1) {
			return t.docId.compareTo(t1.docId);
		}
	};

	@Override
	public int compareTo(Post arg0) {
		// Auto-generated method stub
		return 0;
	}
}
