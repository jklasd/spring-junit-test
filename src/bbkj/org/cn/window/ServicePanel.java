package bbkj.org.cn.window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServicePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServicePanel() {
		log.info("新增业务线");
		JPanel panel_1 = this;
		panel_1.setSize(728, 44);
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
		button_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ConfigDialog dialog = new ConfigDialog();
				dialog.setDefaultCloseOperation(ConfigDialog.HIDE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		button_2.setBounds(510, 10, 93, 23);
		panel_1.add(button_2);
		
		JButton button_3 = new JButton("执行");
		button_3.setBounds(625, 10, 93, 23);
		panel_1.add(button_3);
	}

}
