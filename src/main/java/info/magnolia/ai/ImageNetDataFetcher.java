package info.magnolia.ai;

import net.sf.extjwnl.data.IndexWord;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.fetcher.BaseDataFetcher;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class ImageNetDataFetcher extends BaseDataFetcher {

    private final NativeImageLoader imageLoader = new NativeImageLoader(224, 224, 3);
    private final VGG16ImagePreProcessor preProcessor = new VGG16ImagePreProcessor();

    private final Map<String, Set<IndexWord>> images;
    private final List<String> urls;
    private final List<IndexWord> labels;

    public ImageNetDataFetcher(Map<String, Set<IndexWord>> images, List<IndexWord> labels) {
        this.images = images;
        this.urls = new ArrayList<>(images.keySet());
        this.labels = labels;
    }

    @Override
    public void fetch(int numExamples) {
        List<String> toFetch = urls.subList(cursor, Math.min(cursor + numExamples, urls.size()));
        List<DataSet> dataSets = toFetch.stream()
                .map(this::fetchImage)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
        this.initializeCurrFromList(dataSets);

        cursor += numExamples;
    }

    private Optional<DataSet> fetchImage(String url) {
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            INDArray matrix = imageLoader.asMatrix(image);
            preProcessor.transform(matrix);

            return Optional.of(new DataSet(matrix, oneHotEncode(images.get(url))));
        } catch (IOException e) {
            System.out.println("Skipping image; failed to fetch: " + url);
            return Optional.empty();
        }
    }

    private INDArray oneHotEncode(Set<IndexWord> indexWords) {
        float[] array = new float[labels.size()];
        indexWords.forEach(word -> array[labels.indexOf(word)] = 1);
        return new NDArray(array);
    }
}