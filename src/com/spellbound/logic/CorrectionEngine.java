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
        ENCHANTMENT_MAP.put("irregardless", "regardless");
    }

    public String performMagicCheck(String input) {
        if (input == null || input.isEmpty()) return "";
        String processed = input;
        processed = processed.replaceAll("(\\w)-\\n(\\w)", "$1$2"); 
        processed = processed.replaceAll("(?i)\\bteh\\b", "the");
        processed = processed.replaceAll("(?i)\\bcancled\\b", "cancelled");
        processed = processed.replaceAll("(?i)\\bwritting\\b", "writing");
        processed = processed.replaceAll("(?i)\\bclint\\b", "client");
        processed = processed.replaceAll("(?<=[.,!?;])(?=[^\\s])", " ");
        return processed;
    }

    // --- ARCHITECTED EXPORT ---
    public void exportCorrectedPDF(String text, String originalName, File targetFile, DocSettings settings) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font bodyFont = getFontByName(settings.font, false);
            PDType1Font headerFont = getFontByName(settings.font, true);
            
            float margin = settings.margin;
            float startY = 750;
            float currentY = startY;
            float bottomLimit = 60;
            int pageNum = 1;

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

                // Word Wrap Logic
                String[] words = cleanPara.split(" ");
                StringBuilder currentLine = new StringBuilder();

                for (String word : words) {
                    // Wrap limit depends on margin
                    int wrapLimit = (margin > 80) ? 65 : 85;

                    if (currentLine.length() + word.length() > wrapLimit) {
                        writeLine(contents, currentLine.toString(), activeFont, fontSize, margin, currentY, isHeading);
                        currentY -= leading;
                        currentLine = new StringBuilder();

                        if (currentY < bottomLimit) {
                            if (settings.showPageNumbers) drawPageNumber(contents, pageNum, PDRectangle.A4);
                            contents.close();
                            page = new PDPage(PDRectangle.A4);
                            doc.addPage(page);
                            contents = new PDPageContentStream(doc, page);
                            currentY = startY;
                            pageNum++;
                        }
                    }
                    currentLine.append(word).append(" ");
                }
                
                writeLine(contents, currentLine.toString(), activeFont, fontSize, margin, currentY, isHeading);
                currentY -= (leading + 4); // Extra spacing after paragraph
            }

            if (settings.showPageNumbers) drawPageNumber(contents, pageNum, PDRectangle.A4);
            contents.close();
            doc.save(targetFile);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void writeLine(PDPageContentStream stream, String text, PDType1Font font, float size, float x, float y, boolean isHeading) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        if (isHeading) {
            stream.setNonStrokingColor(81/255f, 49/255f, 169/255f); // Purple for headings
        } else {
            stream.setNonStrokingColor(0, 0, 0); // Black for body
        }
        stream.newLineAtOffset(x, y);
        stream.showText(text.trim());
        stream.endText();
    }

    private PDType1Font getFontByName(String name, boolean bold) {
        if (name == null) return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        switch (name) {
            case "Times-Roman": 
                return new PDType1Font(bold ? Standard14Fonts.FontName.TIMES_BOLD : Standard14Fonts.FontName.TIMES_ROMAN);
            case "Courier": 
                return new PDType1Font(bold ? Standard14Fonts.FontName.COURIER_BOLD : Standard14Fonts.FontName.COURIER);
            default: 
                return new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
        }
    }

    private void drawPageNumber(PDPageContentStream stream, int num, PDRectangle rect) throws IOException {
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
        stream.setNonStrokingColor(150/255f, 150/255f, 150/255f);
        stream.newLineAtOffset(rect.getWidth() / 2, 30);
        stream.showText("- " + num + " -");
        stream.endText();
    }

    private boolean isUserHeading(String line, String[] highlights) {
        if (highlights == null || line.length() > 100) return false; 
        for (String h : highlights) {
            if (!h.trim().isEmpty() && line.toLowerCase().contains(h.toLowerCase().trim())) return true;
        }
        return false;
    }
    private String applyEnchantmentMap(String text) {

    	for (Map.Entry<String, String> entry : ENCHANTMENT_MAP.entrySet()) {

    	// \\b ensures we don't change "intentional" inside "unintentional"

    	text = text.replaceAll("(?i)\\b" + entry.getKey() + "\\b", entry.getValue());

    	}

    	return text;

    	}



    	private String fixArticles(String text) {

    	// Fixes "a apple" -> "an apple" and "an car" -> "a car"

    	text = text.replaceAll("(?i)\\ba ([aeiou])", "an $1");

    	text = text.replaceAll("(?i)\\ban ([bcdfghjklmnpqrstvwxyz])", "a $1");

    	return text;

    	}



    	private String fixPunctuation(String text) {

    	// Remove spaces BEFORE punctuation

    	text = text.replaceAll("\\s+([,.!?;:])", "$1");

    	// Ensure space AFTER punctuation if missing (except at end of string)

    	text = text.replaceAll("([,.!?;:])(?=[a-zA-Z])", "$1 ");

    	// Fix "Oxford Comma" style issues (e.g., ",," or " ,")

    	text = text.replaceAll(",{2,}", ",");

    	return text;

    	}



    	private String fixGrammar(String text) {

    	// Fixes "I is" / "He are"

    	text = text.replaceAll("(?i)\\b(I|you|we|they) is\\b", "$1 are");

    	text = text.replaceAll("(?i)\\b(he|she|it|someone|everyone) are\\b", "$1 is");


    	// Fixes "did not had" -> "did not have"

    	text = text.replaceAll("(?i)\\bdid not (had|has)\\b", "did not have");


    	// Fixes Double Negatives (e.g., "don't need no")

    	text = text.replaceAll("(?i)\\bdon't need no\\b", "don't need any");


    	return text;

    	}




    	private String capitalizeSentences(String text) {

    	StringBuilder sb = new StringBuilder(text);

    	// Matches start of string OR punctuation followed by space

    	Pattern p = Pattern.compile("(^|[.!?]\\s+)([a-z])");

    	Matcher m = p.matcher(sb);

    	while (m.find()) {

    	sb.replace(m.start(2), m.end(2), m.group(2).toUpperCase());

    	}

    	return sb.toString();

    	}



    	public boolean isMisspelled(String word) {

    	return ENCHANTMENT_MAP.containsKey(word.toLowerCase());

    	}

    	}

    // --- Rest of your grammar methods stay the same ---