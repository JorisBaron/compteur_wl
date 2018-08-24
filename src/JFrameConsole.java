
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

public class JFrameConsole extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2537388080608607710L;

	private JTextArea console;
	
	private JFrameWL fenWL;
	
	public JFrameConsole() {
		this("compteur W/L");
		
		
	}

	public JFrameConsole(String title) {
		super(title);
		
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.initialise();
		this.setLocationRelativeTo(null);
		this.setLocation(100, 100);
		this.pack();
		this.setVisible(true);
		
	}

	private void initialise() {
		this.initConsole();
		this.initWLButt();
		
	}
	
	private void initConsole() {
		this.console = new JTextArea(25,80);
		this.console.getDocument().addDocumentListener(new DeleteLinesList());
		this.console.setLineWrap(true);
		this.console.setEditable(false);
		this.console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		this.console.setBackground(new Color(50, 50, 50));
		this.console.setForeground(new Color(255, 255, 255));
		this.console.setMargin(new Insets(0, 5, 0, 5));
		
		this.add(new JScrollPane(this.console,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
	}
	
	private void initWLButt() {
		JButton wlBut = new JButton("Affichage W/L");
		wlBut.addActionListener(new WlButList());
		this.add(wlBut,BorderLayout.NORTH);
	}

	public void consolePrint(String text) {
		this.console.append(text);
		this.console.setCaretPosition(this.console.getDocument().getLength());
	}
	
	public void fenWLClosed () {
		this.fenWL=null;
	}
	
	public boolean isFenWLOpened() {
		return this.fenWL!=null;
	}
	
	public void refreshWL() {
		this.fenWL.refreshWL();
	}
	
	public void waitForOffsets() {
		JOptionPane.showMessageDialog(this,
			    "Mettez les offsets à jour avant de continuer",
			    "",
			    JOptionPane.PLAIN_MESSAGE);
	}
	
	class DeleteLinesList implements DocumentListener {

		@Override
		public void changedUpdate(DocumentEvent e) {}

		@Override
		public void insertUpdate(DocumentEvent e) {
			SwingUtilities.invokeLater( new Runnable()
			{
				public void run()
				{
					Document document = e.getDocument();
					Element root = document.getDefaultRootElement();
					while (root.getElementCount() > 100)
					{
						Element line = root.getElement(0);
						int end = line.getEndOffset();

						try
						{
							document.remove(0, end);
						}
						catch(BadLocationException ble)
						{
							System.out.println(ble);
						}
					}
					
				}
			});
			
		}

		@Override
		public void removeUpdate(DocumentEvent e) {}
		
	}
	
	class WlButList implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if(fenWL==null) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						fenWL = new JFrameWL(JFrameConsole.this);
					}
				});
				
			}
		}
		
	}
	
	
}
