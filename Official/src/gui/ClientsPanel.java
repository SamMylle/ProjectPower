/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import avro.ProjectPower.Client;
import client.exception.AbsentException;
import client.exception.MultipleInteractionException;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import client.DistUser;
import gui.PanelInterface;

/**
 *
 * @author federico
 */
public class ClientsPanel extends javax.swing.JPanel implements PanelInterface {

    private DistUser f_user;
    /**
     * Creates new form ClientsPanel
     */
    public ClientsPanel(DistUser user) {
        
        initComponents();
        
        f_user = user;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainWindow = new javax.swing.JScrollPane();
        tblClients = new javax.swing.JTable();
        btnGetClients = new javax.swing.JButton();

        mainWindow.setViewportView(tblClients);

        btnGetClients.setText("Get Clients");
        btnGetClients.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnGetClientsMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnGetClients)
                .addContainerGap(30, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(129, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnGetClients)
                    .addComponent(mainWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(76, 76, 76))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnGetClientsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnGetClientsMouseClicked
        this.updateClients();
    }//GEN-LAST:event_btnGetClientsMouseClicked
 
    public void updateClients() {       
        try {
            List<Client> clients = f_user.getAllClients();
            DefaultTableModel model = new DefaultTableModel();
            
            model.addColumn("Id");
            model.addColumn("Client type");
            
            for (Client client : clients) {
                model.addRow(new Object[]{(new Integer(client.ID)).toString(), (client.clientType).toString()});
                // System.out.println("trying to add a row with a client");
            }
            this.tblClients.setModel(model);
            
        } catch(MultipleInteractionException e) {
            System.out.println("error multiple interactions at user.");
        } catch(AbsentException e) {
            System.out.println("the user is not present in the system.");
        }
    }
    
    @Override
    public void update() {
        // TODO add method here for updating when this panel is being focused
        this.updateClients();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGetClients;
    private javax.swing.JScrollPane mainWindow;
    private javax.swing.JTable tblClients;
    // End of variables declaration//GEN-END:variables
}
