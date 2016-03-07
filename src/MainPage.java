import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.*;
import javax.swing.border.*;

import java.util.*;

public class MainPage {
    
	private ArrayList<DateEvent> events = new ArrayList<DateEvent>();
    private String username; 
    private JFrame mainFrame;
    private String server;
    private int port;
    private ObjectInputStream sInput; //read from socket
    private ObjectOutputStream sOutput; //write to socket
    private Socket socket;
    private String page;
    //private volatile int updated;
    private volatile boolean lock;
    
    public MainPage(){
    	mainFrame = new JFrame("Event Planner");
    	mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	server = "localhost";
    	port = 8080;
    	//updated = 0;
    	lock = false;
    }
    
    static protected String[] getMonthStrings() {
        String[] months = new java.text.DateFormatSymbols().getMonths();
        int lastIndex = months.length - 1;
 
        if (months[lastIndex] == null
           || months[lastIndex].length() <= 0) { //last item empty
            String[] monthStrings = new String[lastIndex];
            System.arraycopy(months, 0,
                             monthStrings, 0, lastIndex);
            return monthStrings;
        } else { //last item not empty
            return months;
        }
    }    
    
    private String setMemberText(DateEvent event) {
    	String result = "<html>";
    	for(Map.Entry<String, Integer> entry : event.getMembers().entrySet()) { 		    			
    		if(entry.getValue() == 3)
    			result += ("&nbsp;&nbsp;" + entry.getKey() + " is going.<br>");
    		else if(entry.getValue() == 2)
    			result += ("&nbsp;&nbsp;" + entry.getKey() + " might be going.<br>");
    		else if(entry.getValue() == 1)
    			result += ("&nbsp;&nbsp;" + entry.getKey() + " is not going.<br>");
    		else
    			result += ("&nbsp;&nbsp;" + entry.getKey() + " is invited.<br>");
    	}
    	return result + "</html>";
    }
    
    public void loginPage() {
    	page = "login";
 	    mainFrame.getContentPane().removeAll();
 	    mainFrame.getContentPane().revalidate();
 	    mainFrame.getContentPane().repaint();  
    	mainFrame.setSize(300,100);
    	JPanel panel = new JPanel();
    	JLabel userLabel = new JLabel("Enter username: ");
    	JTextField userText = new JTextField(10);
    	panel.add(userLabel);
    	panel.add(userText);
    	JButton loginButton = new JButton("Login");
    	panel.add(loginButton);
       	mainFrame.add(panel, BorderLayout.CENTER);

 	    mainFrame.setVisible(true); 
 	    
 	    loginButton.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent e) {
	    		   username = userText.getText().trim();
	    		   if(username.length() == 0) {
        			   JOptionPane.showMessageDialog(mainFrame, "Username must be at least 1 character.");
	    		   } else {
	    	            if(start()) { 	         			   	    	            
	    	            	mainFrame.setSize(500,500);
	    	            	setupMainPage();
	    	            }
	    		   }
	    	   } 
 	    }); 	    
    }
    
    private boolean start() {
        try {
            socket = new Socket(server, port);
        } 
        // if it failed not much I can so
        catch(Exception ec) {
        	JOptionPane.showMessageDialog(mainFrame,"Error connectiong to server:" + ec);
            return false;
        }
        
        /* Creating both Data Stream */
        try {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
        	JOptionPane.showMessageDialog(mainFrame,"Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server 
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
        	JOptionPane.showMessageDialog(mainFrame,"Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        
        // success we inform the caller that it worked
        System.out.println("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
    	return true;
    }
    
    private void disconnect() {
        try { 
        	if(sInput != null) sInput.close();
        } catch(Exception e) {} // not much else I can do

        try {
            if(sOutput != null) sOutput.close();
        } catch(Exception e) {} // not much else I can do

        try{
            if(socket != null) socket.close();
        } catch(Exception e) {} // not much else I can do
    }
    
    private synchronized void eventPanel(int index, JPanel container) {
    	DateEvent event = events.get(index);
    	JPanel eventContainer = new JPanel();
    	//JLabel userLabel = new JLabel();
    	
    	//eventContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
    	LineBorder eventBorderLine = new LineBorder(Color.BLACK);
    	Border margin = new EmptyBorder(10,10,10,10);    	
    	eventContainer.setBorder(new CompoundBorder(margin,eventBorderLine));
    	eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.Y_AXIS));
    	eventContainer.setMinimumSize (new Dimension (750, 0));
    	eventContainer.setMaximumSize (new Dimension (750, 500));
    	eventContainer.setOpaque(true);
    	
    	//For Debugging purposes only
    	//JLabel test = new JLabel("  ID: " + event.getID());
    	//eventContainer.add(test);
    	
    	JLabel title = new JLabel("  Event: " + event.getTitle());
    	JLabel startDate = new JLabel("  Start Time: " + event.getStart().toString());
    	JLabel endDate = new JLabel("  End Time: " + event.getEnd().toString());
    	JLabel starter = new JLabel("  Created by: " + event.getCreator());
    	JLabel description = new JLabel("  Description: " + event.getDescription());
    	JLabel membersLabel = new JLabel("  Members:\n");
    	eventContainer.add(title);
       	eventContainer.add(starter);
    	eventContainer.add(startDate);
    	eventContainer.add(endDate);
    	eventContainer.add(description);
    	eventContainer.add(membersLabel);
    	JLabel memberText = new JLabel(setMemberText(event));
    	eventContainer.add(memberText);

		JPanel attendingButtons = new JPanel();
		attendingButtons.setAlignmentX( Component.LEFT_ALIGNMENT );
		attendingButtons.setOpaque(true);
		JButton goingButton = new JButton("I am attending");
		JButton maybeGoingButton = new JButton("I might be attending");
		JButton notGoingButton = new JButton("I am not attending");
		attendingButtons.add(goingButton);
		attendingButtons.add(maybeGoingButton);
		attendingButtons.add(notGoingButton);
	
		goingButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   	event.setMemberFlag(username, 3);
    		   	//memberText.setText(setMemberText(event));
	   	        try {
		            sOutput.writeObject(event);
		    		setupMainPage();  
		        } catch(IOException ioe) {
		        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
		        }  	
    	   } 
		});

		maybeGoingButton.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent e) {
	    		   	event.setMemberFlag(username, 2);
	    		   	//memberText.setText(setMemberText(event));
		   	        try {
			            sOutput.writeObject(event);
			    		setupMainPage();  
			        } catch(IOException ioe) {
			        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
			        } 
	    	   } 
		});
		
		notGoingButton.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent e) {
	    		   	event.setMemberFlag(username, 1);
	    		   	//memberText.setText(setMemberText(event));
		   	        try {
			            sOutput.writeObject(event);
			    		setupMainPage();  
			        } catch(IOException ioe) {
			        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
			        } 
	    	   } 
		});
		
		eventContainer.add(attendingButtons);
    	container.add(eventContainer);
    }
    
    private synchronized void setupMainPage(){
       lock = true;
       page = "mainPage";
	   mainFrame.getContentPane().removeAll();
	   mainFrame.getContentPane().revalidate();
	   mainFrame.getContentPane().repaint();    	
    	
	   JPanel container = new JPanel();
	   container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
	   //container.add(Box.createVerticalStrut(10));
	   for(int i = 0; i < events.size(); i++) {
		   eventPanel(i, container);
		   //container.add(Box.createVerticalStrut(10));
	   }

	   JScrollPane scrPane = new JScrollPane(container);
	   mainFrame.add(scrPane);
	   JButton newButton = new JButton("Add New Event"); 
	   
	   newButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   createNewEvent();    		  
    	   }
	   });
	   
	   mainFrame.add(newButton, BorderLayout.PAGE_END); 
	   /*if(updated > 0) {
		   updated--;
		   setupMainPage();
	   }*/
	   lock = false;
	   mainFrame.setVisible(true);
    }
    
    @SuppressWarnings("deprecation")
	private synchronized void createNewEvent() { 
       page = "newEvent";
	   mainFrame.getContentPane().removeAll();
	   mainFrame.getContentPane().revalidate();
	   mainFrame.getContentPane().repaint();
	   mainFrame.setSize(500, 500);
	   
	   Date startDate = new Date();
	   Date endDate = new Date();
	   ArrayList<String> newMembers = new ArrayList<>();
	   newMembers.add(username);
	   
	   //String[] months = getMonthStrings();

	   //mainFrame.setLayout(new BoxLayout(mainFrame));
	   
	   JPanel container = new JPanel(new GridLayout(7,2));
	   JLabel titleLabel = new JLabel("  Create New Event");
	   titleLabel.setHorizontalAlignment(JLabel.CENTER);
	   JLabel eventNameLabel = new JLabel("  Event name: ");
	   JTextField eventName = new JTextField();
	   JLabel descriptionLabel = new JLabel("  Description: ");
	   JTextField description = new JTextField();
	   
	   JPanel startDatePanel = new JPanel();
	   JLabel startDateLabel = new JLabel("Start Date: ");
	   JButton startDateButton = new JButton("Select Start Date");
	   startDatePanel.add(startDateLabel);
	   startDatePanel.add(startDateButton);
	   JLabel startDateText = new JLabel(CalendarFrame.months[startDate.getMonth()] + " " + startDate.getDate() + ", " + (startDate.getYear() + 1900));
	   
	   JPanel endDatePanel = new JPanel();
	   JLabel endDateLabel = new JLabel("End Date: ");
	   JButton endDateButton = new JButton("Select End Date");
	   endDatePanel.add(endDateLabel);
	   endDatePanel.add(endDateButton);
	   JLabel endDateText = new JLabel(CalendarFrame.months[endDate.getMonth()] + " " + endDate.getDate() + ", " + (endDate.getYear() + 1900));
	   
	   JLabel startTimeLabel = new JLabel("  Start Time: ");
	   JSpinner timeSpinner = new JSpinner( new SpinnerDateModel() );
	   JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
	   timeSpinner.setEditor(timeEditor);
	   timeSpinner.setValue(startDate); // starts at the current time
	   
	   JLabel endTimeLabel = new JLabel("  End Time: ");
	   JSpinner timeSpinner2 = new JSpinner( new SpinnerDateModel() );
	   JSpinner.DateEditor timeEditor2 = new JSpinner.DateEditor(timeSpinner2, "HH:mm");
	   timeSpinner2.setEditor(timeEditor2);
	   timeSpinner2.setValue(endDate); // starts at the current time

	   JPanel bottomPanel = new JPanel(new FlowLayout());
	   JButton createEventButton = new JButton("Create Event");
	   JButton backButton = new JButton("Back");
	   bottomPanel.add(createEventButton);
	   bottomPanel.add(backButton);
	   
	   JLabel membersLabel = new JLabel("  Members: ");
	   JScrollPane membersScroll = new JScrollPane(membersLabel);
	   membersScroll.setBorder(new EmptyBorder(0,0,0,0));
	   
	   JPanel membersPanel = new JPanel();
	   JButton addMembersButton = new JButton("Invite member");
	   JTextField addMembersText = new JTextField(20);
	   
	   for(int i = 0; i < newMembers.size(); i++) {
		   if(i > 0)
			   membersLabel.setText(membersLabel.getText() + ", " + newMembers.get(i));
		   else
			   membersLabel.setText(membersLabel.getText() + newMembers.get(i));
	   }
	   membersPanel.add(addMembersText);
	   membersPanel.add(addMembersButton);	   
	   
	   mainFrame.add(titleLabel,BorderLayout.NORTH);
	   container.add(eventNameLabel);
	   container.add(eventName);
	   container.add(descriptionLabel);
	   container.add(description);
	   container.add(startDatePanel);
	   container.add(startDateText);
	   container.add(endDatePanel);
	   container.add(endDateText);
	   container.add(startTimeLabel);
	   container.add(timeSpinner);
	   container.add(endTimeLabel);
	   container.add(timeSpinner2);	   
	   container.add(membersScroll);	  
	   container.add(membersPanel);	  
	   mainFrame.add(container);
	   mainFrame.add(bottomPanel,BorderLayout.SOUTH);
	   
	   addMembersButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   String newMemberToAdd = addMembersText.getText();
    		   boolean valid = true;
    		   for(String existingMembers: newMembers) {
    			   if(newMemberToAdd.equals(existingMembers)) {
    				   valid = false;
        			   JOptionPane.showMessageDialog(container, "Cannot add member. He or she is already invited to the event");
    			   }
    		   }
    		   
    		   if(newMemberToAdd.equals(null) || newMemberToAdd.equals("")) {
    			   valid = false;
    			   JOptionPane.showMessageDialog(container, "Please type a member name into the add member textfield.");
    		   }
    		   
    		   if(valid) {
    			   newMembers.add(newMemberToAdd);
    			   membersLabel.setText("  Members: ");
    			   for(int i = 0; i < newMembers.size(); i++) {
    				   if(i > 0)
    					   membersLabel.setText(membersLabel.getText() + ", " + newMembers.get(i));
    				   else
    					   membersLabel.setText(membersLabel.getText() + newMembers.get(i));
    			   }
    		   }
    	   }
	   });
	   
	   startDateButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   new CalendarFrame();
    		   CalendarFrame.tblCalendar.addMouseListener(new java.awt.event.MouseAdapter() {
    			    @SuppressWarnings("deprecation")
					@Override
    			    public void mouseClicked(java.awt.event.MouseEvent evt) {
    			        int row = CalendarFrame.tblCalendar.rowAtPoint(evt.getPoint());
    			        int col = CalendarFrame.tblCalendar.columnAtPoint(evt.getPoint());
    			        if (row >= 0 && col >= 0) {    			        	
    			        	//JOptionPane.showMessageDialog(null, Calendar.months[Calendar.currentMonth] + " " + (Integer) Calendar.mtblCalendar.getValueAt(row, col) + ", " + Calendar.currentYear);
    			        	if(CalendarFrame.mtblCalendar.getValueAt(row, col) != null) {
    			        		startDateText.setText(CalendarFrame.months[CalendarFrame.currentMonth] + " " + (Integer) CalendarFrame.mtblCalendar.getValueAt(row, col) + ", " + CalendarFrame.currentYear);
    			        		CalendarFrame.frmMain.dispatchEvent(new WindowEvent(CalendarFrame.frmMain, WindowEvent.WINDOW_CLOSING));
    			        		startDate.setMonth(CalendarFrame.currentMonth);
    			        		startDate.setYear(CalendarFrame.currentYear);
    			        		startDate.setDate((Integer) CalendarFrame.mtblCalendar.getValueAt(row, col));
    			        	}
    			        }
    			    }    			    
    			});
    	   }
	   });

	   endDateButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   new CalendarFrame();
    		   CalendarFrame.tblCalendar.addMouseListener(new java.awt.event.MouseAdapter() {
    			    @SuppressWarnings("deprecation")
					@Override
    			    public void mouseClicked(java.awt.event.MouseEvent evt) {
    			        int row = CalendarFrame.tblCalendar.rowAtPoint(evt.getPoint());
    			        int col = CalendarFrame.tblCalendar.columnAtPoint(evt.getPoint());
    			        if (row >= 0 && col >= 0) {    			        	
    			        	//JOptionPane.showMessageDialog(null, Calendar.months[Calendar.currentMonth] + " " + (Integer) Calendar.mtblCalendar.getValueAt(row, col) + ", " + Calendar.currentYear);
    			        	if(CalendarFrame.mtblCalendar.getValueAt(row, col) != null) {
    			        		endDateText.setText(CalendarFrame.months[CalendarFrame.currentMonth] + " " + (Integer) CalendarFrame.mtblCalendar.getValueAt(row, col) + ", " + CalendarFrame.currentYear);
        			        	CalendarFrame.frmMain.dispatchEvent(new WindowEvent(CalendarFrame.frmMain, WindowEvent.WINDOW_CLOSING));
    			        		endDate.setMonth(CalendarFrame.currentMonth);
    			        		endDate.setYear(CalendarFrame.currentYear);
    			        		endDate.setDate((Integer) CalendarFrame.mtblCalendar.getValueAt(row, col));
    			        	}
    			        }
    			    }
    			});
    	   }
	   });
	   
	   backButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   setupMainPage();    		  
    	   }
	   });
	   
	   createEventButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   String newEventName = eventName.getText();
    		   String newEventDescription = description.getText();
    		   
    		   String newStartTime = ((JSpinner.DefaultEditor)timeSpinner.getEditor()).getTextField().getText();
    		   int startTimeColon = newStartTime.indexOf(":");
    		   startDate.setHours(Integer.parseInt(newStartTime.substring(0, startTimeColon)));
    		   startDate.setMinutes(Integer.parseInt(newStartTime.substring(startTimeColon+1)));    		   
    		   startDate.setSeconds(0);
    		   
    		   String newEndTime = ((JSpinner.DefaultEditor)timeSpinner2.getEditor()).getTextField().getText();
    		   int endTimeColon = newEndTime.indexOf(":");
    		   endDate.setHours(Integer.parseInt(newEndTime.substring(0, endTimeColon)));
    		   endDate.setMinutes(Integer.parseInt(newEndTime.substring(endTimeColon+1)));    		   
    		   endDate.setSeconds(0);
    		   System.out.println(newStartTime);
    		   System.out.println(newEndTime);
    		   DateEvent newEvent = new DateEvent(startDate,endDate,newEventDescription,newEventName,username);
    		   for(String membersToAdd : newMembers) {
    			   if(membersToAdd.equals(username))
    	    		   newEvent.setMemberFlag(username, 3);
    			   else
    				   newEvent.setMemberFlag(membersToAdd, 0);   
    		   }
    		   //events.add(newEvent);
    	        try {
    	            sOutput.writeObject(newEvent);
    	    		setupMainPage();  
    	        } catch(IOException ioe) {
    	        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
    	        }  		  
    	   }
	   });
    }    
    
    public static void main(String[] args){
    	MainPage mainPage = new MainPage();
    	mainPage.loginPage();  
    }
    
    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {
        //ArrayList<UserId> users = new ArrayList<>();
        @Override
        public void run() {
            while(true) {
                try {                	
                	//Object received = sInput.readObject();
                	DateEvent incomingEvent = (DateEvent) sInput.readObject();
                	//if(received instanceof DateEvent) {
                		//DateEvent incomingEvent = (DateEvent) received;
                		boolean existingEvent = false;
                		for(int i = 0; i < events.size(); i++) {
                			if(incomingEvent.getID().equals(events.get(i).getID())) {
                				existingEvent = true;
                				events.set(i, incomingEvent);
                				break;
                			}
                		}
                		
                		if(!existingEvent)
                			events.add(incomingEvent);
                		
                		if(page.equals("mainPage")) {
	                		JOptionPane.showMessageDialog(mainFrame, "There are " + events.size() + " events for you.");
	                		//updated++;
	                		while(lock) {} //wait while lock is in place.
	                		setupMainPage();
                		}
                	//} else {
                		
                	//}
                }
                catch(IOException e) {
                	 JOptionPane.showMessageDialog(mainFrame, "Server has close the connection: " + e);
                	 loginPage();
                	 break;
                } catch(ClassNotFoundException e) {
                }
            }
        }
    }
}