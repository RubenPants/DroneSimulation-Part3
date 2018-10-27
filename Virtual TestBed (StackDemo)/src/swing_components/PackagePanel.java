package swing_components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import control.WorldManager;
import entities.Package;

public class PackagePanel extends JPanel{
	
	private JTable packageTable;
	private PackageTableModel packagesModel;
	private JPopupMenu popupCubeMenu;
	
	private WorldManager manager;
	
	
	public PackagePanel() {

		packagesModel = new PackageTableModel();
		packageTable = new JTable(packagesModel);
		packageTable.getTableHeader().setReorderingAllowed(false);
		
		packageTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int row = packageTable.rowAtPoint(e.getPoint());
				packageTable.getSelectionModel().setSelectionInterval(row, row);

				if(e.getButton() == MouseEvent.BUTTON3) {
					popupCubeMenu.show((Component) e.getSource(),e.getX(),e.getY());
				}
			}
		});

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				packageTable.getSelectionModel().clearSelection();
			}
		});
		JScrollPane scrollPane2 = new JScrollPane(packageTable);
		
		scrollPane2.setPreferredSize(new Dimension(420,250));

		JLabel errorLabel = new JLabel();
		errorLabel.setPreferredSize(new Dimension(280, 30));
		errorLabel.setMinimumSize(new Dimension(280, 30));
		
		PackageAdderPanel packAdder = new PackageAdderPanel();
		// TODO: Add package
		packAdder.addPackageListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					manager.addNewPackage(packAdder.getPackage());
					errorLabel.setText("");
				} catch (IllegalArgumentException e1) {
					errorLabel.setText(e1.getMessage());
				}
				packagesModel.fireTableDataChanged();
			}
		});
		
		packAdder.testStackListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					for (int airport = 1; airport <= 8; airport++) {
						for (int gate = 0; gate <= 1; gate++) {
							manager.addNewPackage(new Package(airport, gate, 0, 0));
						}
					}
					errorLabel.setText("");
				} catch (IllegalArgumentException e1) {
					errorLabel.setText(e1.getMessage());
				}
				packagesModel.fireTableDataChanged();
			}
		});
		
		setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		
		/////////////////////////////////////////
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weighty = 1;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		add(new JLabel("Packages"), gc);

		//////////////////////////////////////////
		gc.gridy++;
		gc.anchor = GridBagConstraints.CENTER;
		add(scrollPane2, gc);		
		
		//////////////////////////////////////////
		gc.gridy++;
		gc.anchor = GridBagConstraints.CENTER;
		add(packAdder, gc);
		
		//////////////////////////////////////////
		gc.gridy++;
		gc.anchor = GridBagConstraints.CENTER;
		add(errorLabel, gc);
	}

	
	
	public void setWorldManager(WorldManager manager) {
		this.manager = manager;
		packagesModel.setWorldManager(manager);
	}
	
	public void refreshTables() {
		packagesModel.fireTableDataChanged();
	}
}