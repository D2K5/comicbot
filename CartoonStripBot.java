/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/
 
This file is part of ComicBot.
 
This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/
 
$Author: pjm2 $
$Id: CartoonStripBot.java,v 1.4 2004/02/01 13:19:54 pjm2 Exp $
 
*/
 
import java.io.*;
import java.util.*;
 
import org.jibble.pircbot.*;
 
/**
 * This is a big nasty dirty hack.
 * This code does not come with any warranty or technical support whatsoever.
 *
 * @author Paul Mutton http://www.jibble.org/comicbot/
 */
public class CartoonStripBot extends PircBot{
    
    public static final int MAX_QUOTES = 9;
    
    public CartoonStripBot(File outputDirectory, String helpString, String channel) {
        _outputDirectory = outputDirectory;
        _helpString = helpString;
        _channel = channel;
    }
 
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        String lowMsg = message.trim().toLowerCase();
        if (lowMsg.startsWith(getName().toLowerCase()) && lowMsg.indexOf("help") >= 0) {
            sendMessage(channel, _helpString);
            return;
        }
        processMessage(sender, message);
    }
    
    public void onAction(String sender, String login, String hostname, String target, String action) {
        processMessage(sender, "Me " + action);
    }
 
    public void processMessage(String sender, String message) {
        message = message.trim();
        String lowMsg = message.toLowerCase();
        
        String triggers = "lol;rofl;nigger;faggot;fuck you;lmao;hah;heh;aha";
        String[] split = triggers.split(";");
        Boolean found = false;
        
        for (int i = 0; i < split.length; i++)
        {
            if (lowMsg.startsWith(split[i]))
            {
                found = true;
                break;
            }
        }
        
        if (_quotes.size() == MAX_QUOTES && found) {
            // add the current quote, so that it's always the last thing said
            _quotes.add(message);
            _senders.add(sender);
            
            // Let's make a cartoon!
            String[] texts = new String[MAX_QUOTES];
            String[] nicks = new String[MAX_QUOTES];
            Iterator quoteIt = _quotes.iterator();
            Iterator senderIt = _senders.iterator();
            for (int i = 0; i < MAX_QUOTES; i++) {
                String text = (String)quoteIt.next();
                String nick = (String)senderIt.next();
                texts[i] = text;
                nicks[i] = nick;
            }
            try {
                boolean result = ComicTest.createCartoonStrip(_outputDirectory, texts, nicks);
                //sendMessage(_channel, "Generated new comic at: " + _helpString);
            }
            catch (IOException e) {
                //sendMessage(_channel, "Urgh, I'm crap cos I just did this: " + e);
            }
            _quotes.clear();
            _senders.clear();
        }
        else {
            _quotes.add(message);
            _senders.add(sender);
            if (_quotes.size() > MAX_QUOTES) {
                _quotes.removeFirst();
                _senders.removeFirst();
            }
        }
    }
          
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream(new File("./bot.ini")));
        File outputDirectory = new File(p.getProperty("outputDirectory"));
        if (!outputDirectory.isDirectory()) {
            System.out.println("Output directory must be a valid directory, not " + outputDirectory.toString());
            System.exit(1);
        }
        String channel = p.getProperty("channel");
        CartoonStripBot bot = new CartoonStripBot(outputDirectory, p.getProperty("helpString"), channel);
        bot.setVerbose(true);
        bot.setName(p.getProperty("nick"));
        bot.setLogin(p.getProperty("login"));
        bot.connect(p.getProperty("server"));
        bot.joinChannel(channel);
    }
    
    private File _outputDirectory;
    private String _helpString;
    private String _channel;
    private LinkedList _quotes = new LinkedList();
    private LinkedList _senders = new LinkedList();
    
}
