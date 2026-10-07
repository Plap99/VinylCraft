package com.radig.vinylcraft.client.library;

import java.nio.file.Path;
import java.text.Normalizer;

/**
 * Limpieza defensiva de textos provenientes de tags de audio.
 * Algunos MP3 antiguos traen ID3 mal codificado y producen caracteres como �.
 */
public final class MetadataTextSanitizer {

    private MetadataTextSanitizer() {
    }

    public static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String cleaned = Normalizer.normalize(value, Normalizer.Form.NFC)
                .replace("\uFEFF", "")
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u2060", "")
                .replace("\u00A0", " ")
                .replace("â€™", "’")
                .replace("â€˜", "‘")
                .replace("â€œ", "“")
                .replace("â€", "”")
                .replace("â€“", "–")
                .replace("â€”", "—")
                .replace("ï»¿", "")
                .trim();

        // Quita controles invisibles salvo espacios normales.
        cleaned = cleaned.replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", "");
        return cleaned.trim();
    }

    public static boolean looksCorrupted(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return value.indexOf('\uFFFD') >= 0
                || value.contains("ï¿½")
                || value.contains("�");
    }

    public static String titleOrFileName(String title, Path audioFile) {
        String cleaned = clean(title);

        if (!cleaned.isBlank() && !looksCorrupted(cleaned)) {
            return cleaned;
        }

        String fileTitle = titleFromFileName(audioFile);
        return fileTitle.isBlank()
                ? cleaned.replace("�", "'")
                : fileTitle;
    }

    public static String titleFromFileName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }

        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');

        if (dot > 0) {
            fileName = fileName.substring(0, dot);
        }

        // Quita prefijos típicos de track: "05 ", "05 - ", "05. ", "05_".
        fileName = fileName.replaceFirst("^\\s*\\d{1,3}\\s*(?:[-._]\\s*|\\s+)", "");
        return clean(fileName);
    }
}
