
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.*;

class WebsiteDisplay extends JFrame
{
    private Container cont;

    public WebsiteDisplay(Website displayed_website, CreatedWebsites created_db)
    {
        setTitle(displayed_website.domain);
        setBounds(500, 200, 1000, 1000);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        cont = getContentPane();
        cont.setLayout(new FlowLayout());

        for(int i = 0; i < displayed_website.parsed_content.size(); i++)
        {
            JLabel label = displayed_website.parsed_content.get(i);

            if(label.getName() == "link")
            {
                label.addMouseListener(new MouseListener(){
                    public void mouseClicked(MouseEvent e)
                    {
                        Website clicked_website = created_db.get(label.getText());
                        clicked_website.display(created_db);
                    }
                    public void mousePressed(MouseEvent e) {}
                    public void mouseReleased(MouseEvent e) {}
                    public void mouseEntered(MouseEvent e) {}
                    public void mouseExited(MouseEvent e) {}
                });                
            }

            cont.add(label);
        }

        setVisible(true);
    }
}

class Website implements Comparable < Website >, Serializable
{
    String domain;
    String author;
    ArrayList < JLabel > parsed_content;
    float rank;

    public Website(String domain, String author)
    {
        this.domain = domain;
        this.author = author;
        this.parsed_content = new ArrayList<>();
    }
    public void parse(String content)
    {
        parsed_content.clear();
        String reg = "reg";
        String link = "link";
        String text = "";        

        for(int i = 0; i < content.length(); i++)
        {
            char l = content.charAt(i);
            if(l == '<')
            {
                if(text != "")
                {
                    JLabel label = new JLabel(text);
                    label.setName(reg);
                    parsed_content.add(label);
                    text = "";
                }
            }
            else if(l == '>')
            {
                JLabel label = new JLabel(text);
                label.setName(link);
                parsed_content.add(label);
                text = "";
            }
            else
            {
                text = text + l;
            }
        }

        if(text != "")
        {
            JLabel label = new JLabel(text);
            label.setName(reg);
            parsed_content.add(label);
        }
    }
    public void display(CreatedWebsites created_db)
    {
        new WebsiteDisplay(this, created_db);
    }
    public int outgoing_links()
    {
        int out = 0;
        for(JLabel label : parsed_content)
        {
            if(label.getName() == "link")
            {
                out++;
            }
        }
        return out;
    }
    public int compareTo(Website a)
    {
        if(this.rank - a.rank >= 0)
        {
            return 0;
        }
        else
        {
            return -1;
        }
    }
    public String toString()
    {
        return "";
    }
}

class DB implements Serializable
{
    ConcurrentHashMap < String, Website > websites;
    public DB()
    {
        websites = new ConcurrentHashMap<>();
    }
    public Website get(String domain)
    {
        return websites.get(domain);
    }
    public void remove(String domain)
    {
        websites.remove(domain);
    }
    public void add(String domain, Website w)
    {
        websites.put(domain, w);
    }
    public int size()
    {
        return websites.size();
    }
}

class RankedWebsites extends DB
{
    public RankedWebsites()
    {
        super();
    }
}

class CreatedWebsites extends DB
{
    public CreatedWebsites()
    {
        super();
    }
}

class Crawler extends Thread
{
    Queue < Website > queue;
    RankedWebsites ranked_db;
    CreatedWebsites created_db;
    private Set<String> processedWebsites;

    public Crawler(RankedWebsites ranked_db, CreatedWebsites created_db)
    {
        queue = new LinkedList < Website >();
        this.created_db = created_db;
        this.ranked_db = ranked_db;
        this.processedWebsites = new HashSet<>();

        for (Website w : ranked_db.websites.values())
        {
            queue.add(w);
            processedWebsites.add(w.domain);
        }
    }
    private void parse(Website w)
    {
        ArrayList < JLabel > content = w.parsed_content;

        for(int i = 0; i < content.size(); i++)
        {
            JLabel label = content.get(i);

            String domain_name = label.getText();
            if(label.getName() == "link" && !processedWebsites.contains(domain_name))
            {
                queue.add(created_db.get(domain_name));
                processedWebsites.add(domain_name);
            } 
        }
    }
    private void calculate_rank()
    {
        for (Website w : ranked_db.websites.values())
        {
            w.rank = 1.0f;
        }

        // Iterative PageRank computation
        boolean converged = false;
        while (!converged)
        {
            converged = true;
            for (Website w : ranked_db.websites.values())
            {
                float newRank = 0.0f;
                for (Website v : ranked_db.websites.values())
                {
                    if(v != w)
                    {
                        for(JLabel label : v.parsed_content)
                        {
                            if(label.getName() == "link" && label.getText() == w.domain)
                            {
                                newRank += v.rank / v.outgoing_links();
                            }
                        }
                    }
                }

                if (Math.abs(w.rank - newRank) > 0.0001)
                {
                    converged = false;
                }
                
                w.rank = newRank;
            }
        }
    }
    public void run()
    {
        int timer = 0;
        while(queue.size() > 0)
        {
            if(timer == 1000 || queue.size() == 1)
            {
                System.gc();
                calculate_rank();
                timer = 0;
                processedWebsites.clear();
                queue.addAll(ranked_db.websites.values());
            }
            Website w = queue.poll();

            if(ranked_db.get(w.domain) == null)
            {
                ranked_db.add(w.domain, w);   
            }
            parse(w);
            timer++;

            try
            {
                Thread.sleep(5);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}

class Model_Website_Form extends JFrame
{
    private Container cont;
    private JLabel lblTitle; 
    private JLabel lblDomain;
    private JTextField txtDomain;
    private JLabel lblAuthor;
    private JTextField txtAuthor;
    private JLabel lblContent;
    private JTextArea txaContent;
    private JLabel lblInfo;
    private JLabel lblInfo2;
    private JButton btnSubmit;
    private JLabel lblMsg;

    public Model_Website_Form(Website website, CreatedWebsites created_db)
    {
        setTitle("Website Creation Form");
        setBounds(500, 200, 1000, 1000);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        cont = getContentPane();
        cont.setLayout(null);
        
        lblTitle = new JLabel("Create/Edit Website");
        lblTitle.setFont(new Font("Ariel", Font.PLAIN, 25));
        lblTitle.setSize(300, 30);
        lblTitle.setLocation(400, 50);
        cont.add(lblTitle);        

        lblDomain = new JLabel("domain");
        lblDomain.setFont(new Font("Ariel", Font.PLAIN, 13));
        lblDomain.setSize(100, 20);
        lblDomain.setLocation(100, 130);
        cont.add(lblDomain);

        txtDomain = new JTextField(website.domain);
        txtDomain.setFont(new Font("Ariel", Font.PLAIN, 13));
        txtDomain.setSize(200, 20);
        txtDomain.setLocation(200, 130);
        cont.add(txtDomain);

        lblAuthor = new JLabel("author");
        lblAuthor.setFont(new Font("Ariel", Font.PLAIN, 13));
        lblAuthor.setSize(100, 20);
        lblAuthor.setLocation(100, 160);
        cont.add(lblAuthor);

        txtAuthor = new JTextField(website.author);
        txtAuthor.setFont(new Font("Ariel", Font.PLAIN, 13));
        txtAuthor.setSize(200, 20);
        txtAuthor.setLocation(200, 160);
        cont.add(txtAuthor);

        lblContent = new JLabel("content");
        lblContent.setFont(new Font("Ariel", Font.PLAIN, 13));
        lblContent.setSize(100, 20);
        lblContent.setLocation(100, 190);
        cont.add(lblContent);

        String content = "";
        for(int i = 0; i < website.parsed_content.size(); i++)
        {
            JLabel label = website.parsed_content.get(i);
            if(label.getName() == "link")
            {
                content = content + "<" + label.getText() + ">";
            }
            else
            {
                content = content + label.getText();
            }
        }
        txaContent = new JTextArea(content);
        txaContent.setFont(new Font("Ariel", Font.PLAIN, 13));
        txaContent.setSize(300, 300);
        txaContent.setLocation(200, 190);
        txaContent.setLineWrap(true);
        txaContent.setWrapStyleWord(true);
        cont.add(txaContent);

        lblInfo = new JLabel("To add link put domain name in brackets (with no spaces on the left & right side): <google.com>");
        lblInfo.setFont(new Font("Ariel", Font.PLAIN, 13));
        lblInfo.setSize(800, 20);
        lblInfo.setLocation(100, 520);
        cont.add(lblInfo);

        lblInfo2 = new JLabel("To add your page to ranking, edit websites A, B or C, by adding link <your_website_name> in their content");
        lblInfo2.setFont(new Font("Ariel", Font.PLAIN, 13));
        lblInfo2.setSize(800, 20);
        lblInfo2.setLocation(100, 560);
        cont.add(lblInfo2);

        lblMsg = new JLabel("");
        lblMsg.setFont(new Font("Ariel", Font.PLAIN, 13));
        lblMsg.setSize(200, 20);
        lblMsg.setLocation(100, 600);
        cont.add(lblMsg);

        btnSubmit = new JButton("submit");
        btnSubmit.setSize(100, 20);
        btnSubmit.setLocation(100, 700);
        btnSubmit.addActionListener(e -> {
            website.domain = txtDomain.getText();
            website.author = txtAuthor.getText();

            if(verify_content(txaContent.getText()))
            {
                website.parse(txaContent.getText());
                created_db.add(website.domain, website);
            }
            else
            {
                lblMsg.setText("Podano błędny link");
                cont.repaint();
                cont.revalidate();
            }
            this.dispose();
        });
        cont.add(btnSubmit);

        setVisible(true);
    }
    public Boolean verify_content(String content)
    {
        Boolean is_correct = true;
        Boolean lparen = false;

        for(int i = 0; i < txaContent.getText().length(); i++)
        {
            char l = txaContent.getText().charAt(i);
            if(l == '<')
            {
                if(lparen == true)
                {
                    is_correct = false;
                }
                lparen = true;
            }
            else if(l == '>')
            {
                if(lparen != true)
                {
                    is_correct = false;
                }
                lparen = false;
            }
        }

        if(lparen == true)
        {
            is_correct = false;
        }
        
        return is_correct;
    }
}

class Edit_Website_Form extends JFrame
{
    JLabel lblMsg;
    JTextField txtDomainName;
    JPanel pnl;
    JButton btnSubmit;

    public Edit_Website_Form(CreatedWebsites created_db)
    {
        setTitle("Edit Website");
        setBounds(500, 200, 600, 150);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pnl = new JPanel();
        pnl.setLayout(new FlowLayout());
        lblMsg = new JLabel("provide a domain name: ");
        txtDomainName = new JTextField();
        txtDomainName.setPreferredSize(new Dimension(200, 50));
        btnSubmit = new JButton("submit");
        btnSubmit.addActionListener(e -> {
            String domain_name = txtDomainName.getText();
            Website website = created_db.get(domain_name);
            new Model_Website_Form(website, created_db);
            this.dispose();
        });

        pnl.add(lblMsg);
        pnl.add(txtDomainName);
        pnl.add(btnSubmit);

        add(pnl);

        setVisible(true);
    }
}

interface WindowCloseListener
{
    void windowClosed();
}

class SearchGUI extends JFrame implements MouseListener
{
    RankedWebsites ranked_db;
    CreatedWebsites created_db;
    Model_Website_Form form;
    JLabel lblSearchAddress;
    JLabel lblSearchPhrase;
    JPanel pnlMain;
    JPanel addressBar;
    JPanel phraseBar;
    JPanel modelWebsite;
    JButton btnSearchAddress;
    JButton btnSearchPhrase;
    JButton btnCreateWebsite ;
    JButton btnEditWebsite; 
    JTextField txtSearchAddress;
    JTextField txtSearchPhrase;
    ImageIcon icon;
    String phrase;
    String domain_name;
    WindowCloseListener closeListener;
    JPanel results;
    JLabel lblResult1;
    JLabel lblResult2;
    JLabel lblResult3;

    public SearchGUI(RankedWebsites ranked_db, CreatedWebsites created_db, WindowCloseListener closeListener)
    {
        this.created_db = created_db;
        this.ranked_db = ranked_db;
        this.closeListener = closeListener;

        setTitle("Search Engine");
        setBounds(500, 200, 1000, 1000);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        Font font = new Font(Font.DIALOG_INPUT, Font.PLAIN, 20);
        pnlMain = new JPanel();
        pnlMain.setLayout(new GridLayout(7, 1, 10, 10));

        File file;
        BufferedImage i;
        try
        {
            file = new File("loupe.png");
            i = ImageIO.read(file);
            Image iscaled = i.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(iscaled);
        }
        catch(IOException e) { System.err.printf("couldn't find loupe.png file\n"); }


        addressBar = new JPanel();
        addressBar.setLayout(new FlowLayout());

        lblSearchAddress = new JLabel("search address");
        addressBar.add(lblSearchAddress);

        txtSearchAddress = new JTextField();
        txtSearchAddress.setFont(font);
        txtSearchAddress.setPreferredSize(new Dimension(250, 50));
        addressBar.add(txtSearchAddress);

        btnSearchAddress = new JButton();
        btnSearchAddress.setBackground(Color.GRAY);
        btnSearchAddress.setIcon(icon);
        btnSearchAddress.addActionListener(e -> {
                            domain_name = txtSearchAddress.getText();
                            search_address(domain_name);
                        });
        addressBar.add(btnSearchAddress, BorderLayout.LINE_END);

        pnlMain.add(addressBar);        

        phraseBar = new JPanel();
        phraseBar.setLayout(new FlowLayout());

        lblSearchPhrase = new JLabel("search phrase");
        phraseBar.add(lblSearchPhrase);

        txtSearchPhrase = new JTextField();
        txtSearchPhrase.setFont(font);
        txtSearchPhrase.setPreferredSize(new Dimension(600, 50));

        phraseBar.add(txtSearchPhrase);

        btnSearchPhrase = new JButton();
        btnSearchPhrase.setBackground(Color.GRAY);
        btnSearchPhrase.setIcon(icon);
        btnSearchPhrase.addActionListener(e -> {
                            phrase = txtSearchPhrase.getText();
                            search_phrase(phrase);
                        });
        phraseBar.add(btnSearchPhrase, BorderLayout.LINE_END);

        pnlMain.add(phraseBar);

        btnCreateWebsite = new JButton();
        btnCreateWebsite.setBackground(Color.LIGHT_GRAY);
        try
        {
            file = new File("create.png");
            i = ImageIO.read(file);
            Image iscaled = i.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(iscaled);
        }
        catch(IOException e) { System.err.printf("couldn't find create.png"); }

        modelWebsite = new JPanel();
        modelWebsite.setLayout(new FlowLayout());


        btnCreateWebsite.setIcon(icon);
        btnCreateWebsite.setPreferredSize(new Dimension(50, 50));
        btnCreateWebsite.addActionListener(e -> {
            Website website = new Website("", "");
            new Model_Website_Form(website, created_db);
        });
        modelWebsite.add(btnCreateWebsite);

        JButton btnEditWebsite = new JButton();
        btnEditWebsite.setBackground(Color.LIGHT_GRAY);
        try
        {
            file = new File("edit.png");
            i = ImageIO.read(file);
            Image iscaled = i.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(iscaled);
        }
        catch(IOException e) { System.err.printf("couldn't find edit.png"); }

        btnEditWebsite.setIcon(icon);
        btnEditWebsite.setPreferredSize(new Dimension(50, 50));
        btnEditWebsite.addActionListener(e -> {
            new Edit_Website_Form(created_db);
        });
        modelWebsite.add(btnEditWebsite);

        pnlMain.add(modelWebsite);
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosed(WindowEvent e)
            {
                if (closeListener != null)
                {
                    closeListener.windowClosed();
                }
            }
        });

        results = new JPanel();
        pnlMain.add(results);
        add(pnlMain);

        setVisible(true);
    }
    private void search_address(String domain_name)
    {
        Website website = created_db.get(domain_name);
        if(website != null)
        {
            System.out.println("debug2");
            website.display(created_db);
        }
        else
        {
            System.err.println("no such website error" + domain_name);
        }
    }
    private void search_phrase(String phrase)
    {
        results.removeAll();

        ArrayList < Website > all_results = new ArrayList < Website >(ranked_db.websites.values());
        Collections.sort(all_results);

        int N = all_results.size();

        System.out.println(N);

        lblResult1 = new JLabel(all_results.get(N - 1).domain);
        lblResult1.addMouseListener(this);         
        lblResult2 = new JLabel(all_results.get(N - 2).domain);
        lblResult2.addMouseListener(this);         
        lblResult3 = new JLabel(all_results.get(N - 3).domain);
        lblResult3.addMouseListener(this);

        results.add(lblResult1);
        results.add(lblResult2);
        results.add(lblResult3);

        results.repaint();
        results.revalidate();   
    }
    public void mouseClicked(MouseEvent e)
    {
        Component clicked_component = e.getComponent();
        if(clicked_component instanceof JLabel)
        {
            JLabel clicked_label = (JLabel)clicked_component;
            String domain_name = clicked_label.getText();
            Website clicked_website = ranked_db.get(domain_name);
            clicked_website.display(created_db);
        }
    }
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

}
//<a href="https://www.flaticon.com/free-icons/data" title="data icons">Data icons created by Freepik - Flaticon</a>
//<a href="https://www.flaticon.com/free-icons/create" title="create icons">Create icons created by HideMaru - Flaticon</a>

class SearchEngine implements WindowCloseListener
{
    SearchGUI gui;
    Crawler crawler;
    RankedWebsites ranked_db;
    CreatedWebsites created_db;
    File rankedFile;
    File createdFile;

    public SearchEngine()
    {
        try
        {
            rankedFile = new File("ranked_db.ser");
            createdFile = new File("created_db.ser");

            if (rankedFile.length() > 0 && createdFile.length() > 0)
            {
                try (FileInputStream fileInRanked = new FileInputStream(rankedFile);
                FileInputStream fileInCreated = new FileInputStream(createdFile);
                ObjectInputStream objectInRanked = new ObjectInputStream(fileInRanked);
                ObjectInputStream objectInCreated = new ObjectInputStream(fileInCreated))
                {
                    ranked_db = (RankedWebsites) objectInRanked.readObject();
                    created_db = (CreatedWebsites) objectInCreated.readObject();
                }
            }
            else
            {
                ranked_db = new RankedWebsites();
                created_db = new CreatedWebsites();
                Website A = new Website("A", "unknown");
                String contentA = "<B> <C> to są linki mozna w nie klikac";
                A.parse(contentA);
                Website B = new Website("B", "unknown");
                String contentB = "<C> to jest link mozna go kliknac";
                B.parse(contentB);
                Website C = new Website("C", "unknown");
                String contentC = "<A> <B> to są linki mozna w nie klikac";
                C.parse(contentC);
                
                ranked_db.add("A", A);
                ranked_db.add("B", B);
                ranked_db.add("C", C);

                created_db.add("A", A);
                created_db.add("B", B);
                created_db.add("C", C);

                System.out.println(created_db.size());
                System.out.println(ranked_db.size());
            }

            gui = new SearchGUI(ranked_db, created_db, this);
            crawler = new Crawler(ranked_db, created_db);
            crawler.run();
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    public void windowClosed()
    {
        try (FileOutputStream fileOutRanked = new FileOutputStream(createdFile);
        FileOutputStream fileOutCreated = new FileOutputStream(createdFile);
        ObjectOutputStream objectOutRanked = new ObjectOutputStream(fileOutRanked);
        ObjectOutputStream objectOutCreated = new ObjectOutputStream(fileOutCreated))
        {
            objectOutRanked.writeObject(ranked_db);
            objectOutCreated.writeObject(created_db);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

public class Program
{
    public static void main(String args[])
    {
        new SearchEngine();
    }
}