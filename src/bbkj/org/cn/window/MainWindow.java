package bbkj.org.cn.window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Button;

public class MainWindow extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8499961301917624467L;
	
	private MainPanel mainPanel;

	public MainWindow() {
		setTitle("WEB测试工具");
		setResizable(false);
		setSize(800, 600);
		getContentPane().setLayout(null);
		JButton button = new JButton("新建业务");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ServicePanel panel = new ServicePanel();
				mainPanel.add(panel);
				mainPanel.repaint();
			}
		});
		button.setBounds(20, 10, 93, 23);
		getContentPane().add(button);
		
		JButton button_1 = new JButton("查看业务");
		button_1.setBounds(136, 10, 93, 23);
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		getContentPane().add(button_1);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(20, 69, 750, 421);
		getContentPane().add(scrollPane);
		
		mainPanel = new MainPanel();
		mainPanel.setSize(scrollPane.getSize());
		scrollPane.setViewportView(mainPanel);
		
	}
}