/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of ComicBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ComicTest.java,v 1.3 2004/02/01 13:19:54 pjm2 Exp $

*/

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.font.*;
import java.text.*;
import javax.imageio.*;
import java.lang.reflect.Array; 

/**
 * This is a big nasty dirty hack.
 * This code does not come with any warranty or technical support whatsoever.
 *
 * @author Paul Mutton http://www.jibble.org/comicbot/
 */
public class ComicTest {
    
    public static BufferedImage getTextImage(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2d = (Graphics2D)image.getGraphics();

        for (int fontSize = 50; fontSize > 4; fontSize--) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.black);
            //g2d.drawRect(0, 0, width, height);
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font font = new Font("SansSerif", Font.PLAIN, fontSize);
            g2d.setFont(font);
            
            int borderSize = 3;
            Point pen = new Point(borderSize, borderSize);
            FontRenderContext frc = g2d.getFontRenderContext();
       
            // let styledText be an AttributedCharacterIterator containing at least
            // one character
            AttributedString astring = new AttributedString(text, font.getAttributes());
            AttributedCharacterIterator styledText = astring.getIterator();
       
            LineBreakMeasurer measurer = new LineBreakMeasurer(styledText, frc);
            float wrappingWidth = width - 2*borderSize;
       
            while (measurer.getPosition() < text.length()) {
                TextLayout layout = measurer.nextLayout(wrappingWidth);
                pen.y += layout.getAscent();
                float dx = layout.isLeftToRight() ?
                    0 : (wrappingWidth - layout.getAdvance());
                layout.draw(g2d, pen.x + dx, pen.y);
                pen.y += layout.getDescent() + layout.getLeading();
            }
            
            if (pen.y < height) {
                break;
            }
        }
        return image;
    }
    
    public static void addText(BufferedImage image, String text, int x, int y, int width, int height) {
        BufferedImage textImage = getTextImage(text, width, height);
        Graphics2D g2d = (Graphics2D)image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(textImage, x, y, null);
    }

    // Takes an array of texts (things people say) and an associated
    // array that contains the nick (the person who said the thing).
    public static String createCartoonStrip(File outputDirectory, ArrayList<String> texts, ArrayList<String> nicks, int tries) throws IOException {
        // Find all ini files in the data directory.
        boolean can_make = true;
        File[] filenames = new File("./data").listFiles();
        ArrayList inis = new ArrayList();
        int frameCount = 0;
        for (int i = 0; i < filenames.length; i++) {
            if (filenames[i].getName().endsWith(".ini")) {
                inis.add(filenames[i]);
                frameCount++;
            }
        }
        ArrayList frames = new ArrayList();
        ArrayList bubbles = new ArrayList();
        ArrayList positions = new ArrayList();
        ArrayList lengths = new ArrayList();
        ArrayList goodCandidates = new ArrayList();
        ArrayList badCandidates = new ArrayList();
        
        for (int i = 0; i < inis.size(); i++) {
            File file = (File)inis.get(i);
            Properties p = new Properties();
            p.load(new FileInputStream(file));
            //System.out.println("Frame #" + i + ": " + p.getProperty("background"));
            frames.add(p.getProperty("background"));
            positions.add(0);
            String bubblePositions = "";
            String bubbleLenghts = "";
            for (int b = 1; b <= 9; b++) {
                String bubblePos = p.getProperty("bubble" + b);
                String nickPos = p.getProperty("nick" + b);
                String strLength = p.getProperty("maxlength" + b);
                if (bubblePos != null || nickPos != null) {
                    //System.out.println("Bubble #" + b + ": " + bubblePos);
                    bubblePositions = bubblePositions + bubblePos + ";";
                    positions.set(i, (int)positions.get(i) + 1);
                }
                if (strLength != null) {
                    //System.out.println("Bubble #" + b + ": " + bubblePos);
                    bubbleLenghts = bubbleLenghts + strLength + ";";
                }
            }
            bubbles.add(bubblePositions);
            lengths.add(bubbleLenghts);
        }

        //System.out.println("Text length: " + texts.size());

        for (int i = 0; i < frames.size(); i++) {
            //System.out.println(frames.get(i));
            //System.out.println(bubbles.get(i));
            //System.out.println(texts + " vs " + positions.get(i));
            // String bubbleString = (String) bubbles.get(i);
            // String[] bubblePos = bubbleString.split(";");
            // int bubbleCount = bubblePos.length;
            // System.out.println("Bubble positions: " + bubbleCount);
            // if (texts < (int)positions.get(i)){
            //     System.out.println("texts is shorter than positions");
            // }
            // if (texts > (int)positions.get(i)){
            //     System.out.println("texts is longer than positions");
            // }
            int result = texts.size() - (int)positions.get(i);
            if (result < 0) {
                result *= -1;
            }
            if (texts.size() >= (int)positions.get(i)){
                if (result <= 2){
                    System.out.println("Good candidate: " + frames.get(i) + " (" + positions.get(i) + " bubbles)");
                    goodCandidates.add(i);
                }else{
                    System.out.println("Bad candidate: " + frames.get(i) + " (" + positions.get(i) + " bubbles)");
                    badCandidates.add(i);
                }
            }
        }

        // for (int i = 0; i < goodCandidates.size(); i++) {
        //     //System.out.println(goodCandidates.get(i));
        //     for (int f = 0; f < frames.size(); f++) {
        //         if (f == (int)goodCandidates.get(i)){
        //             System.out.println("Good gandidate: " + frames.get(f));
        //         }
        //     }
        // }

        // for (int i = 0; i < badCandidates.size(); i++) {
        //     //System.out.println(goodCandidates.get(i));
        //     for (int f = 0; f < frames.size(); f++) {
        //         if (f == (int)badCandidates.get(i)){
        //             System.out.println("Bad candidate: " + frames.get(f));
        //         }
        //     }
        // }

    //System.out.println("Trying 100 times");
    //for (int i = 0; i < 100; i++) {
        int framePicked = 0;
        Random rand = new Random(); 
        if (Math.random() < 0.5 && goodCandidates.size() >= 1){
            framePicked = (int)goodCandidates.get(rand.nextInt(goodCandidates.size()));
            System.out.println("i1 Picked good candidate: " + frames.get(framePicked));
        }else if (Math.random() < 0.7){
            if (Math.random() < 0.5 && goodCandidates.size() >= 1){
                framePicked = (int)goodCandidates.get(rand.nextInt(goodCandidates.size()));
                System.out.println("i2 Picked good candidate: " + frames.get(framePicked));
            }else if (badCandidates.size() >= 1){
                framePicked = (int)badCandidates.get(rand.nextInt(badCandidates.size()));
                System.out.println("i2 Picked bad candidate: " + frames.get(framePicked));
            }else{
                return "false";
            }
        }else if (badCandidates.size() >= 1){
            framePicked = (int)badCandidates.get(rand.nextInt(badCandidates.size()));
            System.out.println("i1 Picked bad candidate: " + frames.get(framePicked));
        }else{
            return "false";
        }
    //}
        
        ArrayList bubblePos = new ArrayList();
        ArrayList lengthAmount = new ArrayList();

        String backgroundFilename = (String)frames.get(framePicked);
        BufferedImage image = ImageIO.read(new File("./data/" + backgroundFilename));

        String bubbleString = (String) bubbles.get(framePicked);
        String lengthString = (String) lengths.get(framePicked);

        for (String s : bubbleString.split(";")) {
            bubblePos.add(s);
        }

        for (String s : lengthString.split(";")) {
            lengthAmount.add(s);
        }

        
        int numBubbles = (int) positions.get(framePicked);
        
        if (can_make){
            for (int i = 0; i < numBubbles; i++) {
                int extraWidth = 0;
                int extraHeight = 0;

                // Add bubble text
                ArrayList bubblePositions = new ArrayList();
                String bubblePosString = (String) bubblePos.get(i);

                for (String s : bubblePosString.split(",")) {
                    bubblePositions.add(s);
                }
                String text = (String) texts.get(texts.size() - numBubbles + i);

                int maxLength = Integer.parseInt((String)lengthAmount.get(i));
                //System.out.println(bubblePositions);
                //text too long? try making the bubbles bigger
                if ((text.length() > maxLength)) {
                    System.out.println(text.length() + " > " + maxLength);
                    System.out.println("Text ("+ text +") is too long, adding more space");
                    int result = texts.size() - (int)positions.get(i);
                    if (result < 0) {
                        result *= -1;
                    }
                    if (texts.size() >= (int)positions.get(i)){
                        if (result <= 2){
                            extraWidth = 5; //text.length() - maxLength;
                            extraHeight = 5; //text.length() - maxLength;
                        }else{
                            extraWidth = 10; //text.length() - maxLength;
                            extraHeight = 10; //text.length() - maxLength;
                        }
                    }

                    //System.out.println("Before: " + bubblePositions.get(2) + "x" + bubblePositions.get(3) + " | After: " + ((int)bubblePositions.get(2) + extraWidth) + "x" + ((int)bubblePositions.get(3) + extraHeight));
                }

                addText(image, text, Integer.parseInt((String)bubblePositions.get(0)), Integer.parseInt((String)bubblePositions.get(1)), (Integer.parseInt((String)bubblePositions.get(2)) + extraWidth), (Integer.parseInt((String)bubblePositions.get(3)) + extraHeight));
                System.out.println("adding caption number " + i);
            }
            Date date = new Date();
            String archiveFilename = "cartoon-" + (date.getTime()/1000) + "-" + backgroundFilename;
            ImageIO.write(image, "png", new File(outputDirectory, "cartoon.png"));
            ImageIO.write(image, "png", new File(outputDirectory, archiveFilename));
            return archiveFilename;
        }else{
            return "false";
        }
    }
}
