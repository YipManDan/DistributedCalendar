import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

public class InviteMembersFrame {
	 JFrame frmMain;
	 ArrayList<String> userArrayList;
	 DefaultListModel<String> listModel;
	 public JList<String> userList;
	 JButton okButton;
	 JButton cancelButton;
	String user;
	
	public InviteMembersFrame(String creator) {
		frmMain = new JFrame();
		frmMain.setSize(500,400);
		frmMain.setTitle("Invite members. To select multiple users hold down ctrl.");
		userArrayList = new ArrayList<>();
		user = creator;
		
        //Create a new list model allowing for a dynamically modified list
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        //Allows user to select multiple users from the list
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listModel.addElement("Waiting for the user list from the server...");
        
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(userList));
        
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
		buttonsPanel.setOpaque(true);
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);		
        frmMain.add(centerPanel, BorderLayout.CENTER);
        frmMain.add(buttonsPanel, BorderLayout.SOUTH); 
        frmMain.setVisible(true);
	}
	
    //Update list of users
    public void updateList(ArrayList<String> users) {
        System.out.println("Updating the list...");
        listModel.removeAllElements();
        userArrayList = users;
        int index = userArrayList.indexOf(user);
        if(index >= 0) {
        	userArrayList.remove(index);
        }
        for(int i=0; i<userArrayList.size(); i++){
		    System.out.println("Adding to GUI List: " + userArrayList.get(i));
		    listModel.addElement(userArrayList.get(i));
        }
        userList.repaint();
        
        if(listModel.size() == 0) {
        	JOptionPane.showMessageDialog(null,"There are no other members registered on the server.");
	    	frmMain.dispatchEvent(new WindowEvent(frmMain, WindowEvent.WINDOW_CLOSING));
        }
    }
    
    //Sends back all the users who are selected
    public ArrayList<String> getSelectedUsers() {
    	ArrayList<String> resultList = new ArrayList<>();
   		int[] selected = userList.getSelectedIndices();
           for(int i=0; i < selected.length; i++)
        	   resultList.add(userArrayList.get(selected[i]));    	
        return resultList;
    }
}
