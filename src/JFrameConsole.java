
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
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
	
	private Image icon = new ImageIcon(getClass().getClassLoader().getResource("res/icon.png")).getImage();
	
	private JTextArea console;

	private JProgressBar progress;

	public JFrameConsole() {
		this("compteur W/L");

	}

	public JFrameConsole(String title) {
		super(title);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.initialise();
		this.setLocationRelativeTo(null);
		this.setLocation(100, 100);
		this.setIconImage(icon);
		this.pack();
		this.setVisible(true);

	}

	private void initialise() {
		this.initConsole();
		this.initSetOffsetsBut();
	}

	private void initConsole() {
		this.console = new JTextArea(25, 80);
		this.console.getDocument().addDocumentListener(new DeleteLinesList());
		this.console.setLineWrap(true);
		this.console.setEditable(false);
		this.console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		this.console.setBackground(new Color(50, 50, 50));
		this.console.setForeground(new Color(255, 255, 255));
		this.console.setMargin(new Insets(0, 5, 0, 5));

		this.add(new JScrollPane(this.console, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
	}

	private void initSetOffsetsBut() {
		JButton offsetsBut = new JButton("Changer les offsets");
		offsetsBut.addActionListener(new OffsetsButList());
		this.add(offsetsBut, BorderLayout.NORTH);
	}

	public void consolePrint(String text) {
		this.console.append(text);
		this.console.setCaretPosition(this.console.getDocument().getLength());
	}

	public int[] askOffsets(int[] currentOffset) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		JLabel message = new JLabel("Mise à jour des offsets");
		message.setAlignmentX(CENTER_ALIGNMENT);
		message.setBorder(new EmptyBorder(0, 0, 5, 0));
		mainPanel.add(message);

		JTextField[] offsetInput = new JTextField[2];
		for (int i = 0; i < 2; i++) {
			JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 2));
			JLabel label = new JLabel();
			if (i == 0) {
				label.setText("Wins :");
			} else {
				label.setText("Losses :");
			}

			label.setPreferredSize(new Dimension(50, label.getPreferredSize().height));
			inputPanel.add(label);

			offsetInput[i] = new JTextField("" + currentOffset[i], 5);
			inputPanel.add(offsetInput[i]);

			inputPanel.setAlignmentX(CENTER_ALIGNMENT);

			mainPanel.add(inputPanel);
		}

		JOptionPane.showMessageDialog(this, mainPanel, "", JOptionPane.PLAIN_MESSAGE);

		int[] newOffset = { Integer.parseInt(offsetInput[0].getText()), Integer.parseInt(offsetInput[1].getText()) };
		return newOffset;
	}

	public void newProgressBar(int min, int max) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					if (JFrameConsole.this.progress != null) {
						JFrameConsole.this.removeProgressBar();
					}
					JFrameConsole.this.progress = new JProgressBar(min, max);
					Dimension dim = JFrameConsole.this.getPreferredSize();
					JFrameConsole.this.add(JFrameConsole.this.progress, BorderLayout.SOUTH);
					JFrameConsole.this.progress.setStringPainted(true);
					JFrameConsole.this.setPreferredSize(dim);
					JFrameConsole.this.pack();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setProgressBar(int value) {
		int bar = this.progress.getValue();
		int aumentation = value - bar;

		try {
			if (aumentation < 100) {
				for (int i = 1; i <= aumentation; i++) {
					int j = i;

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							JFrameConsole.this.progress.setValue(bar + j);
							JFrameConsole.this.progress.repaint();
							// JFrameConsole.this.progress.setString(""+(JFrameConsole.this.progress.getValue()/1000f));
						}
					});

					TimeUnit.MILLISECONDS.sleep(2);
				}
			} else {
				for (int i = 1; i <= 100; i++) {
					int j = i;

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							JFrameConsole.this.progress.setValue(bar + (aumentation * j) / 100);
							JFrameConsole.this.progress.repaint();
						}
					});

					TimeUnit.MILLISECONDS.sleep(2);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void removeProgressBar() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrameConsole.this.remove(JFrameConsole.this.progress);
				JFrameConsole.this.progress = null;

				JFrameConsole.this.pack();
			}
		});
	}

	class DeleteLinesList implements DocumentListener {

		@Override
		public void changedUpdate(DocumentEvent e) {
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					Document document = e.getDocument();
					Element root = document.getDefaultRootElement();
					while (root.getElementCount() > 1000) {
						Element line = root.getElement(0);
						int end = line.getEndOffset();

						try {
							document.remove(0, end);
						} catch (BadLocationException ble) {
							System.out.println(ble);
						}
					}

				}
			});

		}

		@Override
		public void removeUpdate(DocumentEvent e) {
		}

	}

	class OffsetsButList implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			CompteurWL.setOffset(JFrameConsole.this.askOffsets(CompteurWL.getOffsets()));
			
		}

	}

}
