import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.text.html.parser.*;
import javax.swing.text.html.*;
import javax.swing.RowFilter.Entry;
import javax.swing.text.*;

class URIinfo {
	URI uri;
	int depth;
	URIinfo(URI uri, int depth) {
		this.uri=uri;
		this.depth=depth;
	}
	URIinfo(String str, int depth) {
		try {
			this.uri=new URI(str);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.depth=depth;
	}

}

/** Tøída ParserCallback je pouívána parserem DocumentParser,
 * je implementován pøímo v JDK a umí parsovat HTML do verze 3.0. 
 * Pøi parsování (analıze) HTML stránky volá tento parser
 * jednotlivé metody tøídy ParserCallback, co nám umoòuje
 * provádìt s èástmi HTML stránky naše vlastní akce.
 * @author Tomáš Dulík
 */
class ParserCallback extends HTMLEditorKit.ParserCallback {
	/**
	 * pageURI bude obsahovat URI aktuálnì parsované stránky. Budeme
	 * jej vyuívat pro resolving všech URL, které v kódu stránky najdeme
	 * - pøedtím, ne najdené URL uloíme do foundURLs, musíme z nìj udìlat 
	 * absolutní URL!
	 */
	URI pageURI;
	/**
	 * depth bude obsahovat aktuální hloubku zanoøení
	 */
	int depth=0, maxDepth=5;
	/** visitedURLs je mnoina všech URL, které jsme ji navštívili
	 * (parsovali). Pokud najdeme na stránce URL, které je v této mnoinì, 
	 * nebudeme jej u dále parsovat 
	 */
	HashSet<URI> visitedURIs;
	/**
	 * foundURLs jsou všechna nová (zatím nenavštívená) URL, která na stránce
	 * najdeme. Poté, co projdeme celou stránku, budeme z tohoto seznamu 
	 * jednotlivá URL brát a zpracovávat.
	 */
	LinkedList<URIinfo> foundURIs;
	/** pokud debugLevel>1, budeme vypisovat debugovací hlášky na std. error */
	int debugLevel=0;
	
	HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
	
	ParserCallback (HashSet<URI> visitedURIs, LinkedList<URIinfo> foundURIs) {
		this.foundURIs=foundURIs;
		this.visitedURIs=visitedURIs;
	}
	            
	/** 
	 *  metoda handleSimpleTag se volá napø. u znaèky <FRAME>
	 */
	public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
		handleStartTag(t, a, pos);
	}
	
	public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
    	 URI uri;
    	 String href=null;
    	 if (debugLevel>1)
    		 System.err.println("handleStartTag: "+t.toString()+", pos="+pos+", attribs="+a.toString());
    	 if (depth<=maxDepth)
	    	 if (t==HTML.Tag.A) href=(String)a.getAttribute(HTML.Attribute.HREF);
	    	 else if (t==HTML.Tag.FRAME) href=(String)a.getAttribute(HTML.Attribute.SRC);
			 if (href!=null)
				 try {
					uri = pageURI.resolve(href);
					if (!uri.isOpaque() && !visitedURIs.contains(uri)) {
						visitedURIs.add(uri);
						foundURIs.add(new URIinfo(uri,depth+1));
						if (debugLevel>0)
				    		 System.err.println("Adding URI: "+uri.toString());
					}
				} catch (Exception e) {
					System.err.println("Nalezeno nekorektní URI: "+href);
					e.printStackTrace();
				}
			
     }
/******************************************************************
 * V metodì handleText bude probíhat veškerá èinnost, související se
 * zjišováním èetnosti slov v textovém obsahu HTML stránek.
 * IMPLEMENTACE TÉTO METODY JE V TÉTO ÚLOZE VAŠÍM ÚKOLEM !!!!
 * Monı postup:
 * Ve tøídì Parser (klidnì v její metodì main) si vyrobte vyhledávací tabulku
 * =instanci tøídy HashMap<String,Integer> nebo TreeMap<String,Integer>.
 * Do této tabulky si ukládejte dvojice klíè-data, kde 
 * klíèem nech jsou jednotlivá slova z textového obsahu HTML stránek,
 * data typu Integer bude dosavadní poèet vıskytù daného slova v 
 * HTML stránkách.
 *******************************************************************/	            
     public void handleText(char[] data, int pos) {
		 //System.out.println("handleText: "+String.valueOf(data)+", pos="+pos);
		 String[] words = String.valueOf(data).trim().split(" ");
    	 
		 for(String word : words) {
			 if(!wordCounts.containsKey(word)){
				 wordCounts.put(word, 1); 
			 } else {
				 wordCounts.put(word, wordCounts.get(word) + 1);
			 }
		 }
     }
     
     public HashMap<String, Integer> returnMap() {
    	 return wordCounts;
     }
}

public class Parser {

	public static void main(String[] args) {
		LinkedList<URIinfo> foundURIs=new LinkedList<URIinfo>();
		HashSet<URI> visitedURIs=new HashSet<URI>();
		URI uri;
		try {
			uri = new URI(args[0]+"/");
			foundURIs.add(new URIinfo(uri, 0));
			visitedURIs.add(uri);
			if (args.length<1) {
				System.err.println("Missing parameter - start URL");
				return;
			}
			
			/** 
			 * zde zpracujte další parametry - maxDepth a debugLevel...
			 */
			ParserCallback callBack=new ParserCallback(visitedURIs, foundURIs);
			ParserDelegator parser=new ParserDelegator();
			
			if (args.length == 2) {
				callBack.maxDepth = Integer.parseInt(args[1]);
			}
			if (args.length == 3) {
				callBack.debugLevel = Integer.parseInt(args[2]);
			}
			
			while (!foundURIs.isEmpty()) {
				URIinfo URIinfo=foundURIs.removeFirst();
				callBack.depth=URIinfo.depth;
				callBack.pageURI=uri=URIinfo.uri;
				System.err.println("Analyzing "+uri);
				try {
					BufferedReader reader=new BufferedReader(new InputStreamReader(uri.toURL().openStream()));
					parser.parse(reader, callBack, true);
					reader.close();
				} catch (FileNotFoundException e) {
					System.err.println("Error loading page - does it exist?");
				}
			}
			
			HashMap<String, Integer> words = callBack.returnMap();
			
			List<Map.Entry<String, Integer>> set = new LinkedList<Map.Entry<String, Integer>>(words.entrySet());
			
			Comparator<Map.Entry<String, Integer>> valueComp = new Comparator<Map.Entry<String,Integer>>() {
				
				@Override
				public int compare(java.util.Map.Entry<String, Integer> o1, java.util.Map.Entry<String, Integer> o2) {
					return (o2.getValue()).compareTo(o1.getValue());
				}
			};
			Collections.sort(set, valueComp);
			int iterator = 0;
			for(Map.Entry<String, Integer> entry: set) {
				if(iterator >= 19) break;
				System.out.println(iterator + ". " + entry.getKey() + ": " + entry.getValue());
				iterator++;
			}
		} catch (Exception e) {
			System.err.println("Zachycena neošetøená vıjimka, konèíme...");
			e.printStackTrace();
		}
	}
		
}


