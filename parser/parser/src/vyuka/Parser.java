package vyuka;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

class URIInfo {
	URI uri;
	int depth;

	URIInfo(URI uri, int depth) {
		this.uri = uri;
		this.depth = depth;
	}

	URIInfo(String str, int depth) {
		try {
			this.uri = new URI(str);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		this.depth = depth;
	}
}

class ParserCallback {
	URI pageURI;
	int depth = 0, maxDepth = 5;

	HashSet<URI> visitedURIs;
	LinkedList<URIInfo> foundURIs;

	int debugLevel = 0;

	HashMap<String, Integer> wordCounts = new HashMap<>();

	ParserCallback(HashSet<URI> visitedURIs, LinkedList<URIInfo> foundURIs) {
		this.foundURIs = foundURIs;
		this.visitedURIs = visitedURIs;
	}

	public void handleStartTag(String tag, String href) {
		URI uri;
		if (depth <= maxDepth && ("a".equalsIgnoreCase(tag) || "frame".equalsIgnoreCase(tag) || "iframe".equalsIgnoreCase(tag))) {
			if (href != null)
				try {
					uri = pageURI.resolve(href);
					if (!uri.isOpaque() && !visitedURIs.contains(uri)) {
						visitedURIs.add(uri);
						foundURIs.add(new URIInfo(uri, depth + 1));
						if (debugLevel > 0)
							System.err.println("Adding URI: " + uri.toString());
					}
				} catch (Exception e) {
					System.err.println("Nalezeno nekorektní URI: " + href);
					e.printStackTrace();
				}
		}
	}

	public void handleText(String text) {
		String[] words = text.trim().split("\\s+");

		for (String word : words) {
			wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
		}
	}

	public HashMap<String, Integer> returnMap() {
		return wordCounts;
	}
}

public class Parser {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Missing parameter - start URL");
			return;
		}

		LinkedList<URIInfo> foundURIs = new LinkedList<>();
		HashSet<URI> visitedURIs = new HashSet<>();
		URI uri;

		try {
			uri = new URI(args[0] + "/");
			foundURIs.add(new URIInfo(uri, 0));
			visitedURIs.add(uri);

			ParserCallback callBack = new ParserCallback(visitedURIs, foundURIs);

			if (args.length >= 2) {
				callBack.maxDepth = Integer.parseInt(args[1]);
			}
			if (args.length >= 3) {
				callBack.debugLevel = Integer.parseInt(args[2]);
			}

			ExecutorService executor = Executors.newFixedThreadPool(500); // Adjust the number of threads as needed

			while (!foundURIs.isEmpty()) {
				URIInfo URIInfo = foundURIs.removeFirst();
				callBack.depth = URIInfo.depth;
				callBack.pageURI = uri = URIInfo.uri;
				System.err.println("Analyzing " + uri);

				URI finalUri = uri;
				Future<?> future = executor.submit(() -> {
					try {
						Document document = Jsoup.connect(finalUri.toString()).get();
						Elements links = document.select("a, frame, iframe");

						for (Element link : links) {
							String tag = link.tagName();
							String href = link.attr("href");
							callBack.handleStartTag(tag, href);
						}

						String text = document.body().text();
						callBack.handleText(text);
					} catch (IOException e) {
						System.err.println("Error loading page - " + e.getMessage());
					}
				});

				try {
					future.get(); // Wait for the task to complete
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			executor.shutdown(); // Shut down the executor service

			HashMap<String, Integer> words = callBack.returnMap();
			List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(words.entrySet());
			sortedWords.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

			int iterator = 0;
			for (Map.Entry<String, Integer> entry : sortedWords) {
				if (iterator >= 19) break;
				System.out.println(iterator + ". " + entry.getKey() + ": " + entry.getValue());
				iterator++;
			}
		} catch (Exception e) {
			System.err.println("Exception caught, exiting...");
			e.printStackTrace();
		}
	}
}
