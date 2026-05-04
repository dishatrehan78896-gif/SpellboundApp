package com.spellbound.logic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import com.spellbound.ui.WorkplaceWindow.DocSettings;

public class CorrectionEngine {

    private static final Map<String, String> ENCHANTMENT_MAP = new HashMap<>();

    static {
        ENCHANTMENT_MAP.put("teh", "the");
        ENCHANTMENT_MAP.put("i", "I");
        ENCHANTMENT_MAP.put("dont", "don't");
        ENCHANTMENT_MAP.put("cant", "can't");
        ENCHANTMENT_MAP.put("recieve", "receive");
        ENCHANTMENT_MAP.put("alot", "a lot");
        ENCHANTMENT_MAP.put("should of", "should have");
        ENCHANTMENT_MAP.put("could of", "could have");
        ENCHANTMENT_MAP.put("clint", "client");
    }

    public String performMagicCheck(String input) {
        if (input == null || input.isEmpty()) return "";

        String processed = fixGrammar(input);

        for (Map.Entry<String, String> entry : ENCHANTMENT_MAP.entrySet()) {
            String typo = entry.getKey();
            String correction = entry.getValue();
            processed = processed.replaceAll("(?i)\\b" + typo + "\\b", correction);
        }

        String[] lines = processed.split("\\r?\\n");
        StringBuilder finalResult = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                finalResult.append("\n");
                continue;
            }
            String currentLine = fixArticles(line);
            currentLine = currentLine.replaceAll("\\s+([.,!?;])", "$1"); 
            finalResult.append(capitalizeSentences(currentLine)).append("\n");
        }
        
        return finalResult.toString().trim();
    }
    public boolean isACorrectedWord(String word) {
        if (word == null) return false;
        String clean = word.trim().toLowerCase().replaceAll("[.,!?;]", "");
        
        if (clean.matches("is|are|the|an|a|i|don't|can't|receive|client|a lot|should have|could have")) {
            return true;
        }
        for (String correctedValue : ENCHANTMENT_MAP.values()) {
            if (correctedValue.toLowerCase().equals(clean)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isMisspelled(String word) {
        if (word == null) return false;
        String clean = word.trim().toLowerCase().replaceAll("[.,!?;]", "");
        
        return ENCHANTMENT_MAP.containsKey(clean);
    }
    public void exportCorrectedPDF(String text, String originalName, File targetFile, DocSettings settings) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font bodyFont = getFontByName(settings.font, false);
            PDType1Font headerFont = getFontByName(settings.font, true);
            
            float margin = settings.margin;
            float currentY = 750;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream contents = new PDPageContentStream(doc, page);

            String[] paragraphs = text.split("\\r?\\n");

            for (String para : paragraphs) {
                String cleanPara = para.replace("\t", "    ").replaceAll("[^\\x20-\\x7E]", "");
                if (cleanPara.trim().isEmpty()) {
                    currentY -= 16;
                    continue;
                }

                boolean isHeading = isUserHeading(cleanPara, settings.highlights);
                float fontSize = isHeading ? 15 : 11;
                float leading = isHeading ? 22 : 16;
                PDType1Font activeFont = isHeading ? headerFont : bodyFont;

                String[] words = cleanPara.split(" ");
                float currentX = margin;

                for (String word : words) {
                    String cleanWord = word.replaceAll("[.,!?;]", "");
                    float wordWidth = activeFont.getStringWidth(word + " ") / 1000 * fontSize;
                    
                    if (currentX + wordWidth > (PDRectangle.A4.getWidth() - margin)) {
                        currentY -= leading;
                        currentX = margin;
                        if (currentY < 60) {
                            contents.close();
                            page = new PDPage(PDRectangle.A4);
                            doc.addPage(page);
                            contents = new PDPageContentStream(doc, page);
                            currentY = 750;
                        }
                    }

                    if (isMisspelled(cleanWord)) {
                        drawHighlight(contents, currentX, currentY, wordWidth, fontSize, 255, 243, 176);
                    }

                    writeWord(contents, word + " ", activeFont, fontSize, currentX, currentY, isHeading);
                    currentX += wordWidth;
                }
                currentY -= (leading + 4); 
            }
            contents.close();
            doc.save(targetFile);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void drawHighlight(PDPageContentStream stream, float x, float y, float width, float size, int r, int g, int b) throws IOException {
        stream.saveGraphicsState();
        stream.setNonStrokingColor(r/255f, g/255f, b/255f);
        stream.addRect(x, y - 2, width, size + 2);
        stream.fill();
        stream.restoreGraphicsState();
    }

    private void writeWord(PDPageContentStream stream, String text, PDType1Font font, float size, float x, float y, boolean isHeading) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        if (isHeading) stream.setNonStrokingColor(81/255f, 49/255f, 169/255f);
        else stream.setNonStrokingColor(0, 0, 0);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private PDType1Font getFontByName(String name, boolean bold) {
        if (name == null) return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        switch (name) {
            case "Times-Roman": return new PDType1Font(bold ? Standard14Fonts.FontName.TIMES_BOLD : Standard14Fonts.FontName.TIMES_ROMAN);
            case "Courier": return new PDType1Font(bold ? Standard14Fonts.FontName.COURIER_BOLD : Standard14Fonts.FontName.COURIER);
            default: return new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
        }
    }

    private boolean isUserHeading(String line, String[] highlights) {
        if (highlights == null) return false;
        for (String h : highlights) {
            if (!h.trim().isEmpty() && line.toLowerCase().contains(h.toLowerCase().trim())) return true;
        }
        return false;
    }

    private String fixArticles(String text) {
        text = text.replaceAll("(?i)\\ba\\s+([aeiou])", "an $1");
        text = text.replaceAll("(?i)\\ban\\s+([bcdfghjklmnpqrstvwxyz])", "a $1");
        return text;
    }

    private String fixGrammar(String text) {
        text = text.replaceAll("(?i)\\b(I|you|we|they)\\s+is\\b", "$1 are");
        text = text.replaceAll("(?i)\\b(he|she|it|someone|everyone)\\s+are\\b", "$1 is");
        return text;
    }

    private String capitalizeSentences(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder(text);
        Pattern p = Pattern.compile("(?<=[.!?]\\s)([a-z])|(?<=^)([a-z])");
        Matcher m = p.matcher(sb);
        while (m.find()) {
            sb.replace(m.start(), m.end(), m.group().toUpperCase());
        }
        return sb.toString();
    }
}
