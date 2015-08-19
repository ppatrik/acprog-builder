package net.acprog.builder;

import java.awt.EventQueue;
import javax.swing.UIManager;
import net.acprog.builder.gui.MainFrame;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {
	// Start gui
	EventQueue.invokeLater(new Runnable() {
	    public void run() {
		try {
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		    MainFrame frame = new MainFrame();
		    frame.setVisible(true);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	});
    }
}
