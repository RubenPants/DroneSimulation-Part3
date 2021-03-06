package swing_components;

import javax.swing.table.AbstractTableModel;

import control.WorldManager;
import entities.Package;

public class PackageTableModel extends AbstractTableModel{

	private WorldManager manager;

	private final String[] headers = {"Number", "From", "Gate", "To", "Gate", "Status"};
	
	public void setWorldManager(WorldManager manager) {
		this.manager = manager;
	}
	
	@Override
	public String getColumnName(int col) {
		return headers[col];
	}

	@Override
	public int getColumnCount() {
		return 6;
	}

	@Override
	public int getRowCount() {
		if(manager==null) return 0;
		return manager.getPackages().size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		Package pack = manager.getPackages().get(row);
		
		switch(col) {
		case 0:
			return row;
		case 1:
			return pack.getFromA();
		case 2:
			return pack.getFromG();
		case 3: 
			return pack.getToA();
		case 4:
			return pack.getToG();
		case 5:
			if(pack.isWaiting()) return "Waiting";
			if(pack.isDelivered()) return "Delivered";
			return "On the way";
		default:
			return null;
		}
	}
	
}