package bbkj.org.cn.window;

import javax.swing.JDialog;
import javax.swing.JLabel;

public class ConfigDialog extends JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6000295159635355671L;

	public ConfigDialog() {
		getContentPane().setLayout(null);
		
		JLabel lab = new JLabel("头消息");
		lab.setBounds(29, 74, 54, 15);
		getContentPane().add(lab);
		
		JLabel label_1 = new JLabel("请求参数");
		label_1.setBounds(29, 165, 54, 15);
		getContentPane().add(label_1);}
}
