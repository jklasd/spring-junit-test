package bbkj.org.cn.window;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JButton;

public class ConfigDialog extends JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6000295159635355671L;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;

	public ConfigDialog() {
		setSize(453, 253);
		setTitle("请求配置");
		getContentPane().setLayout(null);
		
		JLabel lab = new JLabel("请求类型：");
		lab.setBounds(29, 20, 70, 15);
		getContentPane().add(lab);
		
		JLabel label_1 = new JLabel("请求参数：");
		label_1.setBounds(29, 100, 70, 15);
		getContentPane().add(label_1);
		
		JLabel label = new JLabel("校验参数：");
		label.setBounds(29, 139, 70, 15);
		getContentPane().add(label);
		
		JLabel label_2 = new JLabel("头消息：");
		label_2.setBounds(35, 56, 70, 15);
		getContentPane().add(label_2);
		
		String[] requestType = {"GET","POST","PUT","DELETE"};
		JComboBox comboBox = new JComboBox(requestType);
		comboBox.setBounds(126, 17, 71, 21);
		getContentPane().add(comboBox);
		
		textField = new JTextField();
		textField.setBounds(126, 53, 276, 21);
		getContentPane().add(textField);
		textField.setColumns(10);
		
		textField_1 = new JTextField();
		textField_1.setColumns(10);
		textField_1.setBounds(126, 97, 276, 21);
		getContentPane().add(textField_1);
		
		textField_2 = new JTextField();
		textField_2.setColumns(10);
		textField_2.setBounds(126, 136, 276, 21);
		getContentPane().add(textField_2);
		
		JButton button = new JButton("保存");
		button.setBounds(309, 175, 93, 23);
		getContentPane().add(button);
		}
}
