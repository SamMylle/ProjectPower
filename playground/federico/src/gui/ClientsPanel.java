/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import avro.ProjectPower.Client;
import client.exception.AbsentException;
import client.exception.ElectionBusyException;
import client.exception.TakeoverException;
import client.exception.MultipleInteractionException;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import client.DistUser;
import gui.PanelInterface;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;

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
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 499, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGetClients))
                .addContainerGap(229, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainWindow, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(btnGetClients)
                .addContainerGap())
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
            	
            	if (client.getID().equals(new Integer(f_user.getID()))) {
            		model.addRow(new Object[]{(new Integer(client.getID())).toString() + " (yourself)", (client.getClientType()).toString()});
            		continue;
            	}
                model.addRow(new Object[]{(new Integer(client.getID())).toString(), (client.getClientType()).toString()});
            }
            this.tblClients.setModel(model);
            
        } catch(MultipleInteractionException e) {
            DialogExceptions.notifyMultipleInteraction(this);
            return;
        } catch(AbsentException e) {
            DialogExceptions.notifyAbsent(this, "trying to get all the clients");
            return;
        } catch(TakeoverException e) {
            DialogExceptions.notifyTakeover(this);
            return;
        } catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
    }
    
    @Override
    public void update() {
        this.updateClients();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGetClients;
    private javax.swing.JScrollPane mainWindow;
    private javax.swing.JTable tblClients;
    // End of variables declaration//GEN-END:variables
}
