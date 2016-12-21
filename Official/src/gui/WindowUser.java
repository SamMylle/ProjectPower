/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import client.DistUser;
import client.DistTemperatureSensor;
import controller.DistController;
import avro.ProjectPower.*;
import java.util.List;
import client.exception.*;
import javax.swing.table.*;
import javax.swing.JTable;
import gui.ClientsPanel;
import avro.ProjectPower.ControllerComm;
import client.DistLight;
import java.awt.Component;
import client.DistSmartFridge;
import java.awt.Color;
import java.awt.Font;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.avro.AvroRemoteException;

/**
 *
 * @author federico
 */
public class WindowUser extends javax.swing.JFrame {

    private DistUser f_user;
    private DistController f_controller; /// TODO remove this, here for debugging
    private DistTemperatureSensor f_sensor; /// TODO remove this, here for debugging
    private DistSmartFridge f_fridge1; /// TODO remove this, here for debugging
    private DistSmartFridge f_fridge2; /// TODO remove this, here for debugging
    private DistLight f_light1; /// TODO remove this, here for debugging
    private List<DistLight> f_lights;
    
    /**
     * Creates new form MainWindow
     */
    public WindowUser() {
        initComponents();
        String localIP = "192.168.1.4";
        f_controller = new DistController(5000, 10, localIP);
        try {
            f_user = new DistUser("", localIP, localIP, 5000);
        } catch (IOControllerException e) {
            JOptionPane.showMessageDialog(this,
                "Could not connect to the controller on startup... aborting.",
                "Error: could not connect",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        f_sensor = new DistTemperatureSensor(20, 20, localIP, localIP, 5000);
        f_fridge1 = new DistSmartFridge(localIP, localIP, 5000);
        f_fridge2 = new DistSmartFridge(localIP, localIP, 5000);
        
        f_lights = new Vector<DistLight>();
        for (int i = 0; i < 100; i++) {
            f_lights.add(new DistLight(localIP, localIP));
            f_lights.get(i).connectToServer(5000, localIP);
        }
        
        f_fridge1.addItem("butter");
        f_fridge2.addItem("cheese");
        f_fridge2.addItem("milk");
       
        jtpPanelSwitch.addTab("Clients", new ClientsPanel(f_user) );
        jtpPanelSwitch.addTab("Temperature", new TemperaturePanel(f_user));
        jtpPanelSwitch.addTab("Fridge", new FridgePanel(f_user));
        jtpPanelSwitch.addTab("Lights", new LightsPanel(f_user));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jtpPanelSwitch = new javax.swing.JTabbedPane();
        pnlUserStatus = new javax.swing.JPanel();
        lblStatusText = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(800, 500));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jtpPanelSwitch.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jtpPanelSwitchStateChanged(evt);
            }
        });

        lblStatusText.setText("Status:");
        lblStatusText.setToolTipText("");

        javax.swing.GroupLayout pnlUserStatusLayout = new javax.swing.GroupLayout(pnlUserStatus);
        pnlUserStatus.setLayout(pnlUserStatusLayout);
        pnlUserStatusLayout.setHorizontalGroup(
            pnlUserStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUserStatusLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblStatusText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblStatus)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlUserStatusLayout.setVerticalGroup(
            pnlUserStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUserStatusLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlUserStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStatusText)
                    .addComponent(lblStatus))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblStatusText.getAccessibleContext().setAccessibleName("lblStatusText");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jtpPanelSwitch)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 716, Short.MAX_VALUE)
                .addComponent(pnlUserStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlUserStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtpPanelSwitch, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jtpPanelSwitchStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jtpPanelSwitchStateChanged
        // TODO add your handling code here:

        Component comp = jtpPanelSwitch.getSelectedComponent();
        PanelInterface panelinterface = (PanelInterface) comp;
        panelinterface.update();
        this.updateStatus(); // TODO maybe remove this here
    }//GEN-LAST:event_jtpPanelSwitchStateChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        f_user.disconnect();
    }//GEN-LAST:event_formWindowClosing

    
    private void updateStatus() {
        UserStatus status = null;
        
        // should not throw the exception since this is not a remote call
        try {
            status = f_user.getStatus();
        } catch (AvroRemoteException e) { }
        
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
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WindowUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WindowUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WindowUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WindowUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WindowUser().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jtpPanelSwitch;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblStatusText;
    private javax.swing.JPanel pnlUserStatus;
    // End of variables declaration//GEN-END:variables
}
