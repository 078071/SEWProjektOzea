package org.example;

import java.util.concurrent.Callable;
import java.util.List;

public class BookAnalysisTask implements Callable<String> {
    private final int id;
    private final String title;
    private final String text;

    public BookAnalysisTask(int id, String title, String text) {
        this.id = id;
        this.title = title;
        this.text = text;
    }

    @Override
    public String call() {
        BookAnalysis analysis = new BookAnalysis(title, text);

        return String.format(
                "%d,%s,%d,%d,%d,%s",
                id,
                title.replace(",", ""), // Entfernt Kommas, um CSV nicht zu zerstören
                analysis.getWordCount(),
                analysis.getMainWordCount(),
                analysis.getMenschCount(),
                String.join(" ", analysis.getLongWords()) // Liste in eine CSV-Zelle umwandeln
        );
    }
}