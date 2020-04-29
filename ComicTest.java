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
        // Pick a random ini file to make the cartoon with.
        File file = (File)inis.get((int)(Math.random()*inis.size()));

        // Pick a specific ini file to make the cartoon with.
        //File file = (File)inis.get(tries);

        // Get the properties from the file.
        Properties p = new Properties();
        p.load(new FileInputStream(file));
        
        // create the background image for the cartoon strip.
        String backgroundFilename = p.getProperty("background");
        BufferedImage image = ImageIO.read(new File("./data/" + backgroundFilename));
        
        // write on a datestamp.
        String datestampPos = p.getProperty("datestamp");
        String[] datestampSplit = datestampPos.split("[\\s,]+");
        int x = Integer.parseInt(datestampSplit[0]);
        int y = Integer.parseInt(datestampSplit[1]);
        Date date = new Date();
        Graphics2D g2d = (Graphics2D)image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.black);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.drawString(date.toString(), x, y);
        
        LinkedList positions = new LinkedList();
        
        // Parse up to texts.length things to stick into the cartoon.
        for (int i = 1; i <= texts.size(); i++) {
            String bubblePos = p.getProperty("bubble" + i);
            String nickPos = p.getProperty("nick" + i);
            if (bubblePos == null || nickPos == null) {
                break;
            }
            // Work out the positions of both things.
            String[] bubbleSplit = bubblePos.split("[\\s,]+");
            String[] nickSplit = nickPos.split("[\\s,]+");
            int[] bubbleLoc = new int[4];
            int[] nickLoc = new int[4];
            for (int j = 0; j < 4; j++) {
                bubbleLoc[j] = Integer.parseInt(bubbleSplit[j]);
                nickLoc[j] = Integer.parseInt(nickSplit[j]);
            }
            
            positions.add(bubbleLoc);
            positions.add(nickLoc);
            
            System.out.println("adding caption number " + i);
        }
        
        int numBubbles = positions.size() / 2;
        int bubbleCount = numBubbles;
        int textsCount = texts.size();

        System.out.println("texts: " + textsCount + " | bubbles: " + bubbleCount + " (" + backgroundFilename + ") random try: " + tries + "/" + frameCount + " different frames");
        //too few lines? try a different frame
        if ((tries < frameCount) && (textsCount < bubbleCount)) {
            can_make = false;
            System.out.println(textsCount + " < " + bubbleCount);
            System.out.println("Attempting to find a frame with less bubbles...");
            createCartoonStrip(outputDirectory, texts, nicks, tries + 1);
        }
        //not enough lines? try a different frame
        if ((tries < frameCount) && (textsCount > bubbleCount)) {
            can_make = false;
            System.out.println(textsCount + " > " + bubbleCount);
            System.out.println("Attempting to find a frame with more bubbles...");
            createCartoonStrip(outputDirectory, texts, nicks, tries + 1);
        }
        if (can_make){
            for (int i = 0; i < numBubbles; i++) {
                int maxLength = Integer.parseInt(p.getProperty("maxlength" + (i + 1), "20"));
                int[] b = (int[])positions.removeFirst();
                int[] n = (int[])positions.removeFirst();

                int extraWidth = 0;
                int extraHeight = 0;
                // Add bubble text
                String text = texts.get(texts.size() - numBubbles + i);

                //text too long? try shortening it
                if ((text.length() > maxLength)) {
                    System.out.println(text.length() + " > " + maxLength);
                    System.out.println("Text ("+ text +") is too long, adding more space");
                    extraWidth = text.length() - maxLength;
                    extraHeight = text.length() - maxLength;
                    System.out.println("Before: " + b[2] + "x" + b[3] + " | After: " + (b[2] + extraWidth) + "x" + (b[3] + extraHeight));
                    //text = text.replaceAll("^((?:\\W*\\w+){" + n + "}).*$", "$1"); 
                }

                addText(image, text, b[0], b[1], (b[2] + extraWidth), (b[3] + extraHeight));
                System.out.println("adding caption number " + i);
            }
            String archiveFilename = "cartoon-" + (date.getTime()/1000) + "-" + backgroundFilename;
            ImageIO.write(image, "png", new File(outputDirectory, "cartoon.png"));
            ImageIO.write(image, "png", new File(outputDirectory, archiveFilename));
            return archiveFilename;
        }else{
            return "false";
        }
    }
}
