package bbkj.org.cn.window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class MainWindow extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8499961301917624467L;

	public MainWindow() {
		setResizable(false);
		setSize(800, 600);
		getContentPane().setLayout(null);
		
		JButton button = new JButton("新建业务");
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
		
		JPanel panel = new JPanel();
		scrollPane.setViewportView(panel);
		panel.setLayout(null);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(10, 32, 728, 44);
		panel.add(panel_1);
		panel_1.setLayout(null);
		
		JLabel label = new JLabel("1");
		label.setBounds(10, 14, 54, 15);
		panel_1.add(label);
		
		JLabel label_1 = new JLabel("新增用户");
		label_1.setBounds(56, 14, 54, 15);
		panel_1.add(label_1);
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setBounds(127, 14, 328, 14);
		panel_1.add(progressBar);
		
		JButton button_2 = new JButton("配置参数");
		button_2.setBounds(510, 10, 93, 23);
		panel_1.add(button_2);
		
		JButton button_3 = new JButton("执行");
		button_3.setBounds(625, 10, 93, 23);
		panel_1.add(button_3);
		
	}
}