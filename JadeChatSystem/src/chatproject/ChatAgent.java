package chatproject;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*; // Adăugat pentru citire/scriere fișiere
import java.util.Date;

public class ChatAgent extends Agent {

    // GUI Variables
    private JFrame myFrame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JComboBox<String> agentList;
    private String myName;
    private final String LOG_FILE = "chat_log_" + getLocalName() + ".txt"; // Nume fișier specific agentului

    @Override
    protected void setup() {
        myName = getLocalName();
        System.out.println("Starting Agent: " + myName);

        // --- STEP A: REGISTER SERVICE IN DF ---
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("chat-agent-service");
        sd.setName("JADE-chat");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // --- STEP B: SETUP GUI AND LOAD HISTORY ---
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setupGui();
                // 🚀 NOU: Încarcă istoricul după ce GUI-ul este inițializat
                loadHistoryFromFile(); 
            }
        });

        // --- STEP C: BEHAVIOUR TO RECEIVE MESSAGES ---
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    if ("SHUTDOWN_CMD".equals(msg.getContent())) {
                        myAgent.doDelete();
                        return;
                    }
                    
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();
                    String logEntry = "[RECEIVED] " + sender + ": " + content + "\n";
                    
                    if (chatArea != null) chatArea.append(logEntry);
                    saveToFile(logEntry); 
                } else {
                    block();
                }
            }
        });

        // --- STEP D: BEHAVIOUR TO UPDATE USER LIST (SEARCH DF) ---
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                updateActiveAgents();
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // e.printStackTrace();
        }
        if (myFrame != null) {
            myFrame.dispose();
        }
        System.out.println("Agent " + myName + " is shutting down.");
    }

    // --- GUI SETUP (No changes needed here) ---
    private void setupGui() {
        myFrame = new JFrame("Chat - " + myName);
        myFrame.setSize(400, 350);
        myFrame.setLayout(new BorderLayout());
        myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        myFrame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Send to: "), BorderLayout.WEST);
        
        agentList = new JComboBox<String>();
        topPanel.add(agentList, BorderLayout.CENTER);
        
        JButton killButton = new JButton("SHUTDOWN ALL");
        killButton.setForeground(Color.RED);
        killButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendShutdownSignal();
            }
        });
        topPanel.add(killButton, BorderLayout.EAST);
        myFrame.add(topPanel, BorderLayout.NORTH);

        JPanel botPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendBtn = new JButton("Send");
        
        ActionListener sendAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        };
        sendBtn.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
        
        botPanel.add(inputField, BorderLayout.CENTER);
        botPanel.add(sendBtn, BorderLayout.EAST);
        myFrame.add(botPanel, BorderLayout.SOUTH);

        myFrame.setVisible(true);
    }

    // --- LOGIC METHODS ---

    private void updateActiveAgents() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("chat-agent-service");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (agentList == null) return;
            
            // 🐛 FIX: Salvăm selecția curentă înainte de a șterge lista
            Object currentSelection = agentList.getSelectedItem();
            agentList.removeAllItems();
            
            for (int i = 0; i < result.length; ++i) {
                String name = result[i].getName().getLocalName();
                if (!name.equals(myName)) {
                    agentList.addItem(name);
                }
            }
            
            // 🐛 FIX: Restaurăm selecția dacă agentul este încă activ
            if (currentSelection != null) {
                agentList.setSelectedItem(currentSelection);
            }
            
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String content = inputField.getText();
        String recipient = (String) agentList.getSelectedItem();
        
        if (content == null || content.trim().isEmpty() || recipient == null) return;

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(recipient, AID.ISLOCALNAME));
        msg.setContent(content);
        send(msg);

        String log = "[SENT] " + "Me -> " + recipient + ": " + content + "\n";
        chatArea.append(log);
        saveToFile(log);
        
        inputField.setText("");
    }
    
    private void sendShutdownSignal() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("chat-agent-service");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            ACLMessage killMsg = new ACLMessage(ACLMessage.INFORM);
            killMsg.setContent("SHUTDOWN_CMD");
            
            for (DFAgentDescription agent : result) {
                killMsg.addReceiver(agent.getName());
            }
            send(killMsg);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // --- PERSISTENCE METHODS ---
    
    // 💾 Persistă datele
    private void saveToFile(String text) {
        try {
            // Folosim numele agentului în numele fișierului pentru a avea log-uri separate
            FileWriter fw = new FileWriter(LOG_FILE, true);
            PrintWriter pw = new PrintWriter(fw);
            pw.print(new Date().toString() + " | " + text);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // 💾 Încarcă datele (Persistența între sesiuni)
    private void loadHistoryFromFile() {
        File file = new File(LOG_FILE);
        if (!file.exists()) {
            chatArea.append("--- New Session: No previous history found ---\n");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            chatArea.append("--- Previous Session History Loaded ---\n");
            while ((line = br.readLine()) != null) {
                // Adăugăm direct linia citită în zona de chat
                // Sărim peste timestamp-ul JADE log-ul este deja inclus în linia salvată
                String historyLine = line.substring(line.indexOf("|") + 2) + "\n";
                chatArea.append(historyLine);
            }
            chatArea.append("------------------------------------------\n");
            
        } catch (IOException e) {
            chatArea.append("ERROR loading history: " + e.getMessage() + "\n");
        }
    }
}