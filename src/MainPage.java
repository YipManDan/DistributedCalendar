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
    private String page; //the form currently displayed
    private InviteMembersFrame imf;
	private ArrayList<String> newMembers; //the list of invited members on an event

    //The constructor
    public MainPage(){
    	mainFrame = new JFrame();
    	mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	server = "localhost";
    	port = 9001;
    	imf = null;
    	newMembers = new ArrayList<>();
    }
    
    //Returns the name of each month
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
    
    //displays all the members invited to an event and their invite status
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
    
    //creates the login page
    public void loginPage() {
    	page = "login";
 	    mainFrame.getContentPane().removeAll();
 	    mainFrame.getContentPane().revalidate();
 	    mainFrame.getContentPane().repaint();  
    	mainFrame.setSize(300,100);
    	mainFrame.setTitle("Event Planner");
    	JPanel panel = new JPanel();
    	JLabel userLabel = new JLabel("Enter username: ");
    	JTextField userText = new JTextField(10);
    	panel.add(userLabel);
    	panel.add(userText);
    	JButton loginButton = new JButton("Login");
    	panel.add(loginButton);
       	mainFrame.add(panel, BorderLayout.CENTER);

 	    mainFrame.setVisible(true); 
 	    
 	    //action listener of the login button
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
    
    //creates a socket connection to the server
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
    
    //disconnects from the server
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
    
    //creates a panel displaying one event for the username
    private synchronized void eventPanel(int index, JPanel container) {
    	DateEvent event = events.get(index);
    	JPanel eventContainer = new JPanel();    	
    	
    	LineBorder eventBorderLine = new LineBorder(Color.BLACK);
    	Border margin = new EmptyBorder(10,10,10,10);    	
    	eventContainer.setBorder(new CompoundBorder(margin,eventBorderLine));
    	eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.Y_AXIS));
    	eventContainer.setMinimumSize (new Dimension (750, 0));
    	eventContainer.setMaximumSize (new Dimension (750, 500));
    	eventContainer.setOpaque(true);
    	
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
		
		//tells the server that the user is going to the event
		goingButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   	event.setMemberFlag(username, 3);
    		   	event.setLastSender(username);
	   	        try {
		            sOutput.writeObject(event);
		    		setupMainPage();  
		        } catch(IOException ioe) {
		        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
		        }  	
    	   } 
		});

		//tells the server that the user might go to the event
		maybeGoingButton.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent e) {
	    		   	event.setMemberFlag(username, 2);
	    		   	event.setLastSender(username);
		   	        try {
			            sOutput.writeObject(event);
			    		setupMainPage();  
			        } catch(IOException ioe) {
			        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
			        } 
	    	   } 
		});
		
		//tells the server that the user is not going to the event
		notGoingButton.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent e) {
	    		   	event.setMemberFlag(username, 1);
	    		   	event.setLastSender(username);
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
    
    //creates the form for the main page of the client
    private synchronized void setupMainPage(){
       page = "mainPage";
	   mainFrame.getContentPane().removeAll();
	   mainFrame.getContentPane().revalidate();
	   mainFrame.getContentPane().repaint();    	
    	
	   JLabel topLabel = new JLabel("Signed in as: " + username);
	   mainFrame.add(topLabel,BorderLayout.NORTH);
	   
	   JPanel container = new JPanel();
	   container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
	   for(int i = 0; i < events.size(); i++) {
		   eventPanel(i, container);
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
	   mainFrame.setVisible(true);
    }
    
    //Creates the form for the setting the fields of a new event
    @SuppressWarnings("deprecation")
	private synchronized void createNewEvent() { 
       page = "newEvent";
	   mainFrame.getContentPane().removeAll();
	   mainFrame.getContentPane().revalidate();
	   mainFrame.getContentPane().repaint();
	   mainFrame.setSize(500, 500);
	   
	   Date startDate = new Date();
	   Date endDate = new Date();
	   newMembers.add(username);	   
	   
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
	   
	   membersLabel.setText("  Members: " + username);	   
	   membersPanel.add(addMembersButton);	   
	   
	   //Adds the components to the form
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
	   
	   //creates the InviteMembersFrame frame 
	   addMembersButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   imf = new InviteMembersFrame(username);
    		   try {
		            sOutput.writeObject("MemberRequest");  
		       } catch(IOException ioe) {
		        	JOptionPane.showMessageDialog(mainFrame, "Exception requesting from server: " + ioe);
		       }  	    
    		   //adds the selected members to the event
    		   imf.okButton.addMouseListener(new java.awt.event.MouseAdapter() {
   			    	public void mouseClicked(java.awt.event.MouseEvent evt) {
   			    		System.out.println("Setting invited members.");
   			    		newMembers = imf.getSelectedUsers();
   			    		newMembers.add(username);
   			    		membersLabel.setText("  Members: ");
	   			  	    for(int i = 0; i < newMembers.size(); i++) {
	   					   if(i > 0)
	   						   membersLabel.setText(membersLabel.getText() + ", " + newMembers.get(i));
	   					   else
	   						   membersLabel.setText(membersLabel.getText() + newMembers.get(i));
	   				    }   			   
	   			  	   
	   			  	imf.frmMain.dispatchEvent(new WindowEvent(imf.frmMain, WindowEvent.WINDOW_CLOSING));
   			    	}    			    
    		   });
    		   
    		   //exits the InviteMembersFrame without adding the members
    		   imf.cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
      			    public void mouseClicked(java.awt.event.MouseEvent evt) {
      			    	System.out.println("Canceling invite member action.");
      			    	imf.frmMain.dispatchEvent(new WindowEvent(imf.frmMain, WindowEvent.WINDOW_CLOSING));
      			    }    			    
    		   });  		   
    	   }
	   });
	   
	   //creates the calendar frame for selecting the start date
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

	   //creates the calendar frame for selecting the end date
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
	   
	   //exits back to the main page
	   backButton.addActionListener(new ActionListener() {
    	   public void actionPerformed(ActionEvent e) {
    		   setupMainPage();    		  
    	   }
	   });
	   
	   //takes all the data and creates a new event
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
    		   
    		   if(newEventName.equals("") || newEventName.equals(null)) {
    			   JOptionPane.showMessageDialog(mainFrame, "Please enter an event name.");
    		   } else if(startDate.after(endDate)) {
    			   JOptionPane.showMessageDialog(mainFrame, "The event's start time must occur before the end time.");    			   
    		   } else {
	    		   DateEvent newEvent = new DateEvent(startDate,endDate,newEventDescription,newEventName,username,username);
	    		   for(String membersToAdd : newMembers) {
	    			   if(membersToAdd.equals(username))
	    	    		   newEvent.setMemberFlag(username, 3);
	    			   else
	    				   newEvent.setMemberFlag(membersToAdd, 0);   
	    		   }	    		    
	    	        try {
	    	            sOutput.writeObject(newEvent);
	    	            System.out.println("Creating new event.");
	    	            System.out.println(newEvent.title + "\n" + newEvent.getMembers().toString());
	    	    		setupMainPage();  
	    	        } catch(IOException ioe) {
	    	        	JOptionPane.showMessageDialog(mainFrame, "Exception writing to server: " + ioe);
	    	        }  		  
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
        @Override
        public void run() {
            while(true) {
                try {
                	//handles objects received from the server
                	Object received = sInput.readObject();
                	if(received instanceof DateEvent) {
                		DateEvent incomingEvent = (DateEvent) received;
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
                			if(!incomingEvent.getLastSender().equals(username))
                				JOptionPane.showMessageDialog(mainFrame, "Event updates from the server arrived at " + incomingEvent.getTimestamp());
	                		setupMainPage();
                		}
                	} else if(received instanceof String){
                		String errorMsg = (String) received;
                		JOptionPane.showMessageDialog(mainFrame, errorMsg);
                		disconnect();
                		loginPage();
                		break;
                	} else if(received instanceof ArrayList<?>) {
                		ArrayList<String> users = (ArrayList<String>) received;
                		if(!imf.equals(null)) {
                			imf.updateList(users);
                		}
                	}
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