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

/** T��da ParserCallback je pou��v�na parserem DocumentParser,
 * je implementov�n p��mo v JDK a um� parsovat HTML do verze 3.0. 
 * P�i parsov�n� (anal�ze) HTML str�nky vol� tento parser
 * jednotliv� metody t��dy ParserCallback, co� n�m umo��uje
 * prov�d�t s ��stmi HTML str�nky na�e vlastn� akce.
 * @author Tom� Dul�k
 */
class ParserCallback extends HTMLEditorKit.ParserCallback {
	/**
	 * pageURI bude obsahovat URI aktu�ln� parsovan� str�nky. Budeme
	 * jej vyu��vat pro resolving v�ech URL, kter� v k�du str�nky najdeme
	 * - p�edt�m, ne� najden� URL ulo��me do foundURLs, mus�me z n�j ud�lat 
	 * absolutn� URL!
	 */
	URI pageURI;
	/**
	 * depth bude obsahovat aktu�ln� hloubku zano�en�
	 */
	int depth=0, maxDepth=5;
	/** visitedURLs je mno�ina v�ech URL, kter� jsme ji� nav�t�vili
	 * (parsovali). Pokud najdeme na str�nce URL, kter� je v t�to mno�in�, 
	 * nebudeme jej u� d�le parsovat 
	 */
	HashSet<URI> visitedURIs;
	/**
	 * foundURLs jsou v�echna nov� (zat�m nenav�t�ven�) URL, kter� na str�nce
	 * najdeme. Pot�, co projdeme celou str�nku, budeme z tohoto seznamu 
	 * jednotliv� URL br�t a zpracov�vat.
	 */
	LinkedList<URIinfo> foundURIs;
	/** pokud debugLevel>1, budeme vypisovat debugovac� hl�ky na std. error */
	int debugLevel=0;
	
	HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
	
	ParserCallback (HashSet<URI> visitedURIs, LinkedList<URIinfo> foundURIs) {
		this.foundURIs=foundURIs;
		this.visitedURIs=visitedURIs;
	}
	            
	/** 
	 *  metoda handleSimpleTag se vol� nap�. u zna�ky <FRAME>
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
					System.err.println("Nalezeno nekorektn� URI: "+href);
					e.printStackTrace();
				}
			
     }
/******************************************************************
 * V metod� handleText bude prob�hat ve�ker� �innost, souvisej�c� se
 * zji��ov�n�m �etnosti slov v textov�m obsahu HTML str�nek.
 * IMPLEMENTACE T�TO METODY JE V T�TO �LOZE VA��M �KOLEM !!!!
 * Mo�n� postup:
 * Ve t��d� Parser (klidn� v jej� metod� main) si vyrobte vyhled�vac� tabulku
 * =instanci t��dy HashMap<String,Integer> nebo TreeMap<String,Integer>.
 * Do t�to tabulky si ukl�dejte dvojice kl��-data, kde 
 * kl��em nech� jsou jednotliv� slova z textov�ho obsahu HTML str�nek,
 * data typu Integer bude dosavadn� po�et v�skyt� dan�ho slova v 
 * HTML str�nk�ch.
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
			 * zde zpracujte dal�� parametry - maxDepth a debugLevel...
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
			System.err.println("Zachycena neo�et�en� v�jimka, kon��me...");
			e.printStackTrace();
		}
	}
		
}


