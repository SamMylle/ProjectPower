/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import client.DistUser;
import client.exception.*;
import gui.PanelInterface;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author federico
 */
public class TemperaturePanel extends javax.swing.JPanel implements PanelInterface {

    private DistUser f_user;
    private Timer f_timer;
    
    /**
     * Creates new form TemperaturePanel
     * 
     * @param user
     *      The user on which the methods are used.
     */
    public TemperaturePanel(DistUser user) {
        initComponents();
        
        f_user = user;
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Temperature");
        this.tblTemperatureHistory.setModel(model);
        f_timer = new Timer();
        f_timer.schedule(new UpdateTemperatureTask(), 0, 1000);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblCurrentTempText = new javax.swing.JLabel();
        btnGetCurrentTemp = new javax.swing.JButton();
        lblHistoryTempText = new javax.swing.JLabel();
        lblCurrentTemp = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblTemperatureHistory = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(600, 400));

        lblCurrentTempText.setText("Current temperature: ");

        btnGetCurrentTemp.setText("Update Temperatures");
        btnGetCurrentTemp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnGetCurrentTempMouseClicked(evt);
            }
        });

        lblHistoryTempText.setText("History of temperatures:");

        lblCurrentTemp.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N

        tblTemperatureHistory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(tblTemperatureHistory);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnGetCurrentTemp)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblHistoryTempText)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCurrentTempText)
                        .addGap(63, 63, 63)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblCurrentTemp))))
                .addGap(0, 72, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCurrentTempText)
                    .addComponent(lblCurrentTemp))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblHistoryTempText)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addComponent(btnGetCurrentTemp)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnGetCurrentTempMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnGetCurrentTempMouseClicked
        this.update();
    }//GEN-LAST:event_btnGetCurrentTempMouseClicked

    private void updateCurrentTemp() throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
        double currentTemp = 0;
        try {
            currentTemp = f_user.getCurrentTemperatureHouse();
        } catch (NoTemperatureMeasures e) {
            lblCurrentTemp.setText("No temperature measurements available yet.");
            return;
        }
        
        lblCurrentTemp.setText(new DecimalFormat(".##").format(currentTemp));
    }
    
    private void updateHistoryTemp() throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
        List<Double> temperatures = f_user.getTemperatureHistory();
        
        DefaultTableModel model = (DefaultTableModel) tblTemperatureHistory.getModel();
        model.setRowCount(0);
            
        for (Double temperature : temperatures) {
            model.addRow(new Object[]{(new Double(temperature))});
        }
    }
    
    
    @Override
    public void update() {
        try {
            this.updateCurrentTemp();
            this.updateHistoryTemp();
        } catch (MultipleInteractionException ex) {
            DialogExceptions.notifyMultipleInteraction(this);
            return;
        } catch (AbsentException ex) {
            DialogExceptions.notifyAbsent(this, "getting the temperature");
            return;
        } catch (TakeoverException ex) {
            DialogExceptions.notifyTakeover(this);
            return;
        } catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
    }
    
    private class UpdateTemperatureTask extends TimerTask {
        public UpdateTemperatureTask() { }
        
        @Override
        public void run() {
            try {
                TemperaturePanel.this.updateCurrentTemp();
                TemperaturePanel.this.updateHistoryTemp();
            } catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException ex) { }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGetCurrentTemp;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCurrentTemp;
    private javax.swing.JLabel lblCurrentTempText;
    private javax.swing.JLabel lblHistoryTempText;
    private javax.swing.JTable tblTemperatureHistory;
    // End of variables declaration//GEN-END:variables
}
