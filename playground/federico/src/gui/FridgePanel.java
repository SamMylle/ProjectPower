/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import client.DistUser;
import gui.PanelInterface;
import avro.ProjectPower.Client;
import client.exception.*;
import java.util.List;
import java.util.Vector;
import avro.ProjectPower.ClientType;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author federico
 */
public class FridgePanel extends javax.swing.JPanel implements PanelInterface{

    private DistUser f_user;
    private List<Client> f_fridges;
    private DirectFridgeFrame f_directFrame;
    
    /**
     * Creates new form FridgePanel
     * @param user
     *      The user on which the methods are being used
     */
    public FridgePanel(DistUser user) {
        initComponents();
        
        f_user = user;
        this.update();
        scpScrollPaneFridgeInventory.setVisible(false);
        f_directFrame = null;
        
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Items");
        tblFridgeInventory.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblFridgeText = new javax.swing.JLabel();
        cbbSelectFridge = new javax.swing.JComboBox<>();
        scpScrollPaneFridgeInventory = new javax.swing.JScrollPane();
        tblFridgeInventory = new javax.swing.JTable();
        btnCommunicate = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(600, 400));

        lblFridgeText.setText("Choose a fridge:");

        cbbSelectFridge.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbbSelectFridge.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbbSelectFridgeItemStateChanged(evt);
            }
        });

        tblFridgeInventory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        scpScrollPaneFridgeInventory.setViewportView(tblFridgeInventory);

        btnCommunicate.setText("Communicate directly");
        btnCommunicate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnCommunicateMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(scpScrollPaneFridgeInventory, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(lblFridgeText)
                            .addGap(18, 18, 18)
                            .addComponent(cbbSelectFridge, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(btnCommunicate))
                .addContainerGap(287, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFridgeText)
                    .addComponent(cbbSelectFridge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(scpScrollPaneFridgeInventory, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(btnCommunicate)
                .addContainerGap(41, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    
    private void directWindowClosing(java.awt.event.WindowEvent evt) {
        if (cbbSelectFridge.getSelectedIndex() != -1) {
            try {
                this.updateTableFridgeInventory(cbbSelectFridge.getSelectedIndex());
            } catch (AbsentException ex) {
                DialogExceptions.notifyAbsent(this, "trying to update the fridge item list");
                return;
            } catch (MultipleInteractionException ex) {
                DialogExceptions.notifyMultipleInteraction(this);
                return;
            } catch (TakeoverException ex) {
                DialogExceptions.notifyTakeover(this);
                return;
            } catch (ElectionBusyException e) {
    			DialogExceptions.notifyElectionBusy(this);
    			return;
    		}
        }
        f_directFrame = null;
    }
    
    private void cbbSelectFridgeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbbSelectFridgeItemStateChanged
        try {
            if (this.cbbSelectFridge.getSelectedIndex() == -1) {
                return;
            }
            this.updateTableFridgeInventory(this.cbbSelectFridge.getSelectedIndex());
        } catch (AbsentException ex) {
            DialogExceptions.notifyAbsent(this, "getting the item list of a fridge");
            return;
        } catch (MultipleInteractionException ex) {
            DialogExceptions.notifyMultipleInteraction(this);
            return;
        } catch (TakeoverException ex) {
            DialogExceptions.notifyTakeover(this);
            return;
        } catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
    }//GEN-LAST:event_cbbSelectFridgeItemStateChanged

    private void btnCommunicateMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCommunicateMouseClicked
        int fridgeVectorIndex = this.cbbSelectFridge.getSelectedIndex();
        try {
            f_user.communicateWithFridge(f_fridges.get(fridgeVectorIndex).getID());
            f_user.openFridge();
        } catch (AbsentException e) {
            DialogExceptions.notifyAbsent(this, "trying to communicate with the fridge");
            return;
        } catch (TakeoverException e) {
            DialogExceptions.notifyTakeover(this);
            return;
        } catch (MultipleInteractionException e) {
            DialogExceptions.notifyMultipleInteraction(this);
            return;
        } catch (NoFridgeConnectionException e) {
            DialogExceptions.notifyNoFridgeConnection(this);
            return;
        } catch (FridgeOccupiedException e) {
            DialogExceptions.notifyFridgeOccupied(this);
            return;
        } catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
        
        f_directFrame = new DirectFridgeFrame(f_user);
        f_directFrame.setVisible(true);
        f_directFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                directWindowClosing(evt);
            }
        });
    }//GEN-LAST:event_btnCommunicateMouseClicked

    private void updateFridges() throws AbsentException, MultipleInteractionException, TakeoverException, ElectionBusyException {
        f_fridges = new Vector<Client>();
        List<Client> clients = f_user.getAllClients();
        for (Client client : clients) {
            if (client.getClientType() == ClientType.SmartFridge) {
                f_fridges.add(client);
            }
        }
        
        cbbSelectFridge.removeAllItems();
        if (f_fridges.isEmpty() == true) {
            scpScrollPaneFridgeInventory.setVisible(false);
        }
        
        for (Client fridge : f_fridges) {
            cbbSelectFridge.addItem("Fridge - ID: " + fridge.getID());
        }
    }
    
    private void updateTableFridgeInventory (int fridgeVectorIndex) throws AbsentException, MultipleInteractionException, TakeoverException, ElectionBusyException {
        scpScrollPaneFridgeInventory.setVisible(true);
        List<String> items = null;
        items = f_user.getFridgeItems(f_fridges.get(fridgeVectorIndex).getID());

        DefaultTableModel model = (DefaultTableModel) tblFridgeInventory.getModel();
        model.setRowCount(0);
        for (String item : items) {
            model.addRow(new Object[]{item});
        }
    }
    
    @Override
    public void update() {
        try {
            // TODO add method here for updating when this panel is being focused
            this.updateFridges();
            if (f_fridges.isEmpty() == false) {
                this.updateTableFridgeInventory(0);
            }
        } catch (AbsentException ex) {
            DialogExceptions.notifyAbsent(this, "trying to get all the fridges");
            return;
        } catch (MultipleInteractionException ex) {
            DialogExceptions.notifyMultipleInteraction(this);
            return;
        } catch (TakeoverException ex) {
            DialogExceptions.notifyTakeover(this);
            return;
        } catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCommunicate;
    private javax.swing.JComboBox<String> cbbSelectFridge;
    private javax.swing.JLabel lblFridgeText;
    private javax.swing.JScrollPane scpScrollPaneFridgeInventory;
    private javax.swing.JTable tblFridgeInventory;
    // End of variables declaration//GEN-END:variables
}
