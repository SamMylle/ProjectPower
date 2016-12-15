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
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author federico
 */
public class FridgePanel extends javax.swing.JPanel implements PanelInterface{

    private DistUser f_user;
    private List<Client> f_fridges;
    
    /**
     * Creates new form FridgePanel
     * @param user
     *      The user on which the methods are being used
     */
    public FridgePanel(DistUser user) {
        initComponents();
        
        f_user = user;
        this.updateFridges();
        scpScrollPaneFridgeInventory.setVisible(false);
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

    private void cbbSelectFridgeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbbSelectFridgeItemStateChanged
        // TODO add your handling code here:
        if (this.cbbSelectFridge.getSelectedIndex() == -1) {
            return;
        }
        this.updateTableFridgeInventory(this.cbbSelectFridge.getSelectedIndex());
    }//GEN-LAST:event_cbbSelectFridgeItemStateChanged

    private void btnCommunicateMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCommunicateMouseClicked
        int fridgeVectorIndex = this.cbbSelectFridge.getSelectedIndex();
        try {
            f_user.communicateWithFridge(f_fridges.get(fridgeVectorIndex).getID());
            f_user.openFridge();
        } catch (AbsentException e) {
            // TODO do something here
        } catch (TakeoverException e) {
            // TODO do something here
        } catch (MultipleInteractionException e) {
            // TODO do something here
        } catch (NoFridgeConnectionException e) {
            // TODO do something here
        } catch (FridgeOccupiedException e) {
            // TODO do something here
        }
        
        DirectFridgeFrame frame = new DirectFridgeFrame(f_user);
        frame.setVisible(true);
        
    }//GEN-LAST:event_btnCommunicateMouseClicked

    private void updateFridges() {
        f_fridges = new Vector<Client>();
        try {
            List<Client> clients = f_user.getAllClients();
            for (Client client : clients) {
                if (client.getClientType() == ClientType.SmartFridge) {
                    f_fridges.add(client);
                }
            }
        } catch (AbsentException e) {
            // TODO do something here
        } catch (MultipleInteractionException e) {
            // TODO do something here
        } catch (TakeoverException e) {
            // TODO do something here
        }
        
        cbbSelectFridge.removeAllItems();
        if (f_fridges.isEmpty() == true) {
            scpScrollPaneFridgeInventory.setVisible(false);
        }
        
        for (Client fridge : f_fridges) {
            cbbSelectFridge.addItem("Fridge - ID: " + fridge.getID());
        }
    }
    
    private void updateTableFridgeInventory (int fridgeVectorIndex) {
        scpScrollPaneFridgeInventory.setVisible(true);
        List<String> items = null;
        try {
            items = f_user.getFridgeItems(f_fridges.get(fridgeVectorIndex).getID());
        } catch (AbsentException e) {
            // TODO do something here
        } catch (MultipleInteractionException e) {
            // TODO do something here
        } catch (TakeoverException e) {
            // TODO do something here
        }
        
        DefaultTableModel model = new DefaultTableModel();
            
        model.addColumn("Items");

        for (String item : items) {
            model.addRow(new Object[]{item});
            // System.out.println("trying to add a row with a client");
        }
        this.tblFridgeInventory.setModel(model);
    }
    
    @Override
    public void update() {
        // TODO add method here for updating when this panel is being focused
        this.updateFridges();
        if (f_fridges.isEmpty() == false) {
            this.updateTableFridgeInventory(0);
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
