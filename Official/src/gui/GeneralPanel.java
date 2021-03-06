/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;
import avro.ProjectPower.UserStatus;
import client.*;
import client.exception.*;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.avro.AvroRemoteException;


/**
 *
 * @author federico
 */
public class GeneralPanel extends javax.swing.JPanel implements PanelInterface {

    private DistUser f_user;
    private Timer f_timer;
    
    /**
     * Creates new form GeneralPanel
     */
    public GeneralPanel(DistUser user) {
        initComponents();
        
        f_user = user;
        this.updateStatus();
        txtaNotifications.append("Notifications:\n\n");
        f_timer = new Timer();
        f_timer.schedule(new UpdateNotifications(), 50, 1000);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtaNotifications = new javax.swing.JTextArea();
        lblStatusText = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        btnEnter = new javax.swing.JButton();
        btnLeave = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(750, 450));

        txtaNotifications.setEditable(false);
        txtaNotifications.setColumns(20);
        txtaNotifications.setRows(5);
        jScrollPane1.setViewportView(txtaNotifications);

        lblStatusText.setText("Status:");
        lblStatusText.setToolTipText("");

        btnEnter.setText("Enter house");
        btnEnter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnEnterMouseClicked(evt);
            }
        });

        btnLeave.setText("Leave house");
        btnLeave.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnLeaveMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblStatusText)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(lblStatus))
                    .addComponent(btnLeave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnEnter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 166, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 461, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnEnter)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnLeave)
                .addGap(57, 57, 57)
                .addComponent(lblStatusText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblStatus)
                .addGap(53, 53, 53))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnterMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEnterMouseClicked
        try {
			f_user.enterHouse();
		} catch (TakeoverException e) {
			DialogExceptions.notifyTakeover(this);
			return;
		} catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
        this.updateStatus();
    }//GEN-LAST:event_btnEnterMouseClicked

    private void btnLeaveMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLeaveMouseClicked
        try {
			f_user.leaveHouse();
		} catch (TakeoverException e) {
			DialogExceptions.notifyTakeover(this);
			return;
		} catch (ElectionBusyException e) {
			DialogExceptions.notifyElectionBusy(this);
			return;
		}
        this.updateStatus();
    }//GEN-LAST:event_btnLeaveMouseClicked


    private void updateStatus() {
        UserStatus status = null;
        
        // should not throw the exception since this is not a remote call
        try {
            status = f_user.getStatus();
        } catch (AvroRemoteException e) { return; }
        
        lblStatusText.setFont(new Font(lblStatusText.getName(), Font.PLAIN, 18));
        lblStatus.setFont(new Font(lblStatus.getName(), Font.PLAIN, 18));
        
        switch (status) {
            case present:
                lblStatus.setText("Present");
                lblStatus.setForeground(new Color(0,153,0));
                break;
            case absent:
                lblStatus.setText("Absent");
                lblStatus.setForeground(Color.red);
                break;
            default:
                break;
        }
    }
    
    @Override
    public void update() {
        this.updateStatus();
    }
    
    
    private class UpdateNotifications extends TimerTask {
        public UpdateNotifications() {}
        
        public void run() {
            List<String> notifications = f_user.getNotifications();
            if (notifications.isEmpty() == false) {
                for (String notification : notifications) {
                    txtaNotifications.append("\n[" + new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(Calendar.getInstance().getTime())  + "] " + notification);
                    f_user.removeFirstNotification();
                }
            }
        }
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEnter;
    private javax.swing.JButton btnLeave;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblStatusText;
    private javax.swing.JTextArea txtaNotifications;
    // End of variables declaration//GEN-END:variables
}
