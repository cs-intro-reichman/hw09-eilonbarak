import java.util.Random;
import java.util.LinkedHashMap;
import java.io.File;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    LinkedHashMap<String, List> CharDataMap;

    // The window length used in this model.
    int windowLength;

    // The random number generator used by this model.
    private Random randomGenerator;

    /**
     * Constructs a language model with the given window length and a given
     * seed value. Generating texts from this model multiple times with the
     * same seed value will produce the same random texts. Good for debugging.
     */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new LinkedHashMap<String, List>();
    }

    /**
     * Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production.
     */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new LinkedHashMap<String, List>();
    }

    public void train(String fileName) {
        // try given path; if not exists, try temp directory (so tests that pass only
        // file.getName() work)
        File f = new File(fileName);
        if (!f.exists()) {
            f = new File(System.getProperty("java.io.tmpdir"), fileName);
        }
        In in = new In(f.getAbsolutePath());

        String window = "";

        for (int i = 0; i < windowLength; i++) {
            if (!in.isEmpty()) {
                window += in.readChar();
            }
        }

        while (!in.isEmpty()) {
            char c = in.readChar();

            List probs = CharDataMap.get(window);

            if (probs == null) {
                probs = new List();
                CharDataMap.put(window, probs);
            }

            probs.update(c);

            window = window.substring(1) + c;
        }

        for (List probs : CharDataMap.values()) {
            calculateProbabilities(probs);
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    public void calculateProbabilities(List probs) {

        int total = 0;
        ListIterator it = probs.listIterator(0);

        while (it.hasNext()) {
            CharData cd = it.next();
            total += cd.count;
        }

        double cumulative = 0.0;
        it = probs.listIterator(0);

        while (it.hasNext()) {
            CharData cd = it.next();
            cd.p = cd.count / (double) total;
            cumulative += cd.p;
            cd.cp = cumulative;
        }
    }

    public char getRandomChar(List probs) {
        double r = randomGenerator.nextDouble();
        ListIterator it = probs.listIterator(0);

        while (it.hasNext()) {
            CharData cd = it.next();
            if (cd.cp > r) {
                return cd.chr;
            }
        }

        it = probs.listIterator(0);
        CharData last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        return last.chr;
    }

    /**
     * Generates a random text, based on the probabilities that were learned during
     * training.
     * 
     * @param initialText     - text to start with. If initialText's last substring
     *                        of size numberOfLetters
     *                        doesn't appear as a key in Map, we generate no text
     *                        and return only the initial text.
     * @param numberOfLetters - the size of text to generate
     * @return the generated text
     */

    public String generate(String initialText, int textLength) {
        if (initialText == null) {
            return null;
        }

        if (textLength <= initialText.length()) {
            return initialText.substring(0, textLength);
        }

        if (initialText.length() < windowLength) {
            return initialText;
        }

        StringBuilder generated = new StringBuilder(initialText);

        while (generated.length() < textLength) {
            String window = generated.substring(generated.length() - windowLength);

            List probs = CharDataMap.get(window);
            if (probs == null) {
                break;
            }

            char nextChar = getRandomChar(probs);
            generated.append(nextChar);
        }

        return generated.toString();
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
        if (args.length < 2) {
            System.err.println("Usage: java LanguageModel <windowLength> <filename>");
            return;
        }
        try {
            int w = Integer.parseInt(args[0]);
            LanguageModel lm = new LanguageModel(w);
            lm.train(args[1]);
            System.out.print(lm.toString());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
