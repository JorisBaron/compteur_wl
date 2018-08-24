
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class JFrameWL extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8693432627618838147L;
	private JFrameConsole parentConsole;
	private JTextField text;
	
	public JFrameWL(JFrameConsole parent) {
		super("W/L");
		this.parentConsole=parent;
		
		this.setBounds(150, 150, 600, 300);
		this.getContentPane().setBackground(new Color(0, 255, 0));
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.addWindowListener(new WinCloseList());
		
		this.initText();
		
		this.setVisible(true);
	}
	
	private void initText() {
		this.text = new JTextField();
		this.text.setOpaque(false);
		this.text.setEditable(false);
		this.text.setHorizontalAlignment(JTextField.CENTER);
		this.text.setAlignmentY(CENTER_ALIGNMENT);
		this.text.setFont(this.text.getFont().deriveFont(75f));
		this.text.setBorder(null);
		ChangeTextSizeList changeSizeList = new ChangeTextSizeList();
		this.text.addMouseWheelListener(changeSizeList);
		this.text.addMouseListener(changeSizeList);
		this.text.addKeyListener(changeSizeList);
		this.add(this.text);
		this.refreshWL();
	}
	
	public void refreshWL() {
		try (BufferedReader reader = Files.newBufferedReader(CompteurWL.WL_PATH, CompteurWL.FILE_CHARSET)) {
			String strWL = reader.readLine();
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					JFrameWL.this.text.setText(strWL);
				}
			});
		} catch (IOException  e) {
			e.printStackTrace();
		}
	}
	
	class WinCloseList implements WindowListener {

		@Override
		public void windowActivated(WindowEvent e) {}

		@Override
		public void windowClosed(WindowEvent e) {
			parentConsole.fenWLClosed();
		}

		@Override
		public void windowClosing(WindowEvent e) {}

		@Override
		public void windowDeactivated(WindowEvent e) {}

		@Override
		public void windowDeiconified(WindowEvent e) {}

		@Override
		public void windowIconified(WindowEvent e) {}

		@Override
		public void windowOpened(WindowEvent e) {}
		
	}
	
	class ChangeTextSizeList implements MouseWheelListener, MouseListener,KeyListener {

		private boolean ctrlPressed;
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			Font textFont = text.getFont();
			float fontSize = textFont.getSize2D();
			if(!(fontSize<=0 && e.getWheelRotation()>0)) {
				float newFontSize;
				if(this.ctrlPressed) {
					newFontSize=fontSize-e.getWheelRotation();
				} else {
					newFontSize=fontSize-e.getWheelRotation()*10;
				}
				text.setFont(textFont.deriveFont(newFontSize));
			}
			else {
				text.setFont(textFont.deriveFont(0f));
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			text.setFont(text.getFont().deriveFont(75f));
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_CONTROL) {
				this.ctrlPressed=true;
				System.out.println("ctrl");
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_CONTROL) {
				this.ctrlPressed=false;
				System.out.println("!ctrl");
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {}
		
	}
	
}
