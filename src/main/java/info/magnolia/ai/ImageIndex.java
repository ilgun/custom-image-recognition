package info.magnolia.ai;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ImageIndex {

    /**
     * Mapping from url to labels.
     */
    private final Map<String, Set<IndexWord>> images = new HashMap<>();

    private final List<IndexWord> labels;

    public ImageIndex() {
        try {
            labels = loadLabels();
        } catch (IOException | JWNLException | URISyntaxException e) {
            throw new RuntimeException("Failed to load label list", e);
        }
        loadImageInfo();
    }

    private List<IndexWord> loadLabels() throws IOException, JWNLException, URISyntaxException {
        Path path = Paths.get(getClass().getResource("labels.txt").toURI());
        List<String> labelStrings = Files.readAllLines(path, Charset.forName("utf-8"));

        Dictionary dictionary = Dictionary.getDefaultResourceInstance();
        List<IndexWord> labels = new ArrayList<>();
        for (String labelString : labelStrings)
            labels.add(dictionary.lookupIndexWord(POS.NOUN, labelString));
        return labels;
    }

    private void loadImageInfo() {
        labels.forEach(this::loadForLabel);
    }

    private void loadForLabel(IndexWord label) {
        System.out.println("Loading image URLs: " + label.getLemma());
        final Synset synset = label.getSenses().get(0);
        String synsetId = String.format("n%08d", synset.getOffset());
        List<String> urls = fetchLines("http://www.image-net.org/api/text/imagenet.synset.geturls?wnid=" + synsetId);
        for (String url : urls) {
            if (!images.containsKey(url)) images.put(url, new HashSet<>());
            images.get(url).add(label);
        }
    }

    private List<String> fetchLines(String url) {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(new URL(url).openStream())) {
            while (scanner.hasNextLine()) lines.add(scanner.nextLine());
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<IndexWord> getLabels() {
        return new ArrayList<>(labels);
    }

    @Override
    public String toString() {
        return String.format("ImageIndex (%s labels, %s image URLs)", labels.size(), images.keySet().size());
    }
}