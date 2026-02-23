import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;
    
    // The window length used in this model.
    int windowLength;
    
    // The random number generator used by this model. 
	private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     *  seed value. Generating texts from this model multiple times with the 
     *  same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
	public void train(String fileName) {
		String window = "";
		char c;

        In in = new In(fileName);
		// Constructs the first window
		while ((!in.isEmpty()) && (window.length() < windowLength)) {
			// Gets the next character from standard input
			c  = in.readChar();
			window += c;
		}
		// Processes the entire text, one character at a time
		while (!in.isEmpty()) {
			c  = in.readChar();
			
			// Checks if the window is already in the map
			List probs = CharDataMap.get(window);
			if (probs == null) {
				// The window is not in the map;
				// Creates a new list and adds the (window,list) to the map
				probs = new List();
				CharDataMap.put(window, probs);
			}
			// If the character is not in the CharData list,
			// adds it to the beginning of the list. 
			// Otherwise, increments the character's counter.
			probs.update(c);

			// Advances the window
			window += c;
			window = window.substring(1, window.length());
		}

		// Computes and sets the p and pp fields of all the
		// CharData objects in the each linked list in the map.
		for (List probs : CharDataMap.values())
			calculateProbabilities(probs);
	}

    // Computes and sets the probabilities (p and cp fields) of all the
	// characters in the given list. */
	void calculateProbabilities(List probs) {				
		// Calculate total counts, to be used for probability calculation
		int windowTotal = 0;
		for (int i = 0; i < probs.getSize(); ++i) {
			windowTotal += probs.get(i).count;
		}

		// Calculating probabilities and CDF values
		for (int i = 0; i < probs.getSize(); ++i) {
			// calculate probability
			probs.get(i).p = probs.get(i).count / (double)windowTotal; 

			// update CDF for the current element
			probs.get(i).cp = probs.get(i).p + (i > 0 ? probs.get(i - 1).cp : 0); 
		}
	}

    // Returns a random character from the given probabilities list.
	char getRandomChar(List probs) {
		// Monte Carlo process
		double random = randomGenerator.nextDouble();
		char charToReturn = ' ';
		for (int i = 0; i < probs.getSize(); ++i) {
			if (random < probs.get(i).cp) {
				charToReturn = probs.get(i).chr;
				break;
			}
		}
		return charToReturn;
	}

    /**
	 * Generates a random text, based on the probabilities that were learned during training. 
	 * @param initialText - text to start with. If initialText's last substring of size numberOfLetters
	 * doesn't appear as a key in Map, we generate no text and return only the initial text. 
	 * @param numberOfLetters - the size of text to generate
	 * @return the generated text
	 */
	public String generate(String initialText, int textLength) {
		// If initial text is shorter than the window length, we can't generate anything further.
		if (initialText.length() < windowLength)
			return initialText;

		String generatedText = initialText;
		for (int i = 0; i < textLength; ++i) {
			// Extracts the window from the generated text, which is exactly the last windowLength characters
			String window = generatedText.substring(generatedText.length() - windowLength, generatedText.length());
			List probs = CharDataMap.get(window);
			if (probs == null)
				return generatedText;

			generatedText += getRandomChar(probs);
		}
		return generatedText;
	}

    /** Returns a string representing the map of this language model. */
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (String key : CharDataMap.keySet()) {
			List keyProbs = CharDataMap.get(key);
			str.append(key + " : " + keyProbs + "\n");
		}
		return str.toString();
	}

    public static void main(String[] args) {
        int windowLength = Integer.parseInt(args[0]);
		String initialText = args[1];
		int generatedTextLength = Integer.parseInt(args[2]);
		Boolean randomGeneration = args[3].equals("random");
		String fileName = args[4];

        // Create the LanguageModel object
        LanguageModel lm;
        if (randomGeneration)
            lm = new LanguageModel(windowLength);
        else
            lm = new LanguageModel(windowLength, 20);


		// Trains the model, creating the map.
		lm.train(fileName);

		// Generates text, and prints it.
		System.out.println(lm.generate(initialText, generatedTextLength));
    }
}
