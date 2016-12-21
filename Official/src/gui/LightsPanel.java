/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;
import client.*;
import java.util.List;
import java.util.Vector;
import client.util.*;
import client.exception.*;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;


/**
 *
 * @author federico
 */
public class LightsPanel extends javax.swing.JPanel implements PanelInterface {

    private DistUser f_user;
    private LightsListModel f_lightsModel;
    
    
    /**
     * Creates new form LightsPanel
     */
    public LightsPanel(DistUser user) {
        initComponents();
        
        f_user = user;
        f_lightsModel = new LightsListModel();
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
        lstLights = new javax.swing.JList<>();
        btnLightsOn = new javax.swing.JButton();
        btnLightsOff = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(600, 400));

        lstLights.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        lstLights.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstLightsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(lstLights);

        btnLightsOn.setText("Turn lights on");
        btnLightsOn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnLightsOnMouseClicked(evt);
            }
        });

        btnLightsOff.setText("Turn lights off");
        btnLightsOff.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnLightsOffMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnLightsOn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnLightsOff, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(112, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(btnLightsOn)
                        .addGap(18, 18, 18)
                        .addComponent(btnLightsOff)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void lstLightsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstLightsMouseClicked
        if (evt.getClickCount() == 2) {
            // get dialog to update the light
            int lstIndex = lstLights.getSelectedIndex();
            int newState = Integer.parseInt( (String) JOptionPane.showInputDialog(this, 
                "New light state: ", "Set light state", 
                JOptionPane.PLAIN_MESSAGE, 
                null, 
                null, 
                ""));
            
            LightState state = (LightState) (f_lightsModel.getElementAt(lstIndex));
            int lightID = state.ID;
            
            try {
                f_user.setLightState(newState, lightID);
            } catch (MultipleInteractionException ex) {
                JOptionPane.showMessageDialog(this,
                    "You are currently connected to a fridge, close the connection first.",
                    "Error: fridge connection",
                    JOptionPane.ERROR_MESSAGE);
            } catch (AbsentException ex) {
                JOptionPane.showMessageDialog(this,
                    "You should be present in the house before you try to close the fridge.",
                    "Error: not present",
                    JOptionPane.ERROR_MESSAGE);
            } catch (TakeoverException ex) {
                JOptionPane.showMessageDialog(this,
                    "You are currently acting as the backup controller of the system, due to a failure of the main controller. During this time, you cannot perform any actions.",
                    "Error: controller takeover",
                    JOptionPane.ERROR_MESSAGE);
            }
            
            this.getLights();
        }
    }//GEN-LAST:event_lstLightsMouseClicked

    private void btnLightsOnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLightsOnMouseClicked
        this.setAllLights(100);
        this.getLights();
    }//GEN-LAST:event_btnLightsOnMouseClicked

    private void btnLightsOffMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLightsOffMouseClicked
        this.setAllLights(0);
        this.getLights();
    }//GEN-LAST:event_btnLightsOffMouseClicked

    public void setAllLights(int state) {
        List<LightState> lightstates = f_lightsModel.getStates();
        
        for (LightState lightstate : lightstates) {
            try {
                f_user.setLightState(state, lightstate.ID);
            } catch (MultipleInteractionException ex) {
                JOptionPane.showMessageDialog(this,
                    "You are currently connected to a fridge, close the connection first.",
                    "Error: fridge connection",
                    JOptionPane.ERROR_MESSAGE);
            } catch (AbsentException ex) {
                JOptionPane.showMessageDialog(this,
                    "You should be present in the house before you try to close the fridge.",
                    "Error: not present",
                    JOptionPane.ERROR_MESSAGE);
            } catch (TakeoverException ex) {
                JOptionPane.showMessageDialog(this,
                    "You are currently acting as the backup controller of the system, due to a failure of the main controller. During this time, you cannot perform any actions.",
                    "Error: controller takeover",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public void getLights() {
        List<LightState> lightstates = null;
        
        try {
            lightstates = f_user.getLightStates();
        } catch (MultipleInteractionException ex) {
            JOptionPane.showMessageDialog(this,
                "You are currently connected to a fridge, close the connection first.",
                "Error: fridge connection",
                JOptionPane.ERROR_MESSAGE);
        } catch (AbsentException ex) {
            JOptionPane.showMessageDialog(this,
                "You should be present in the house before you try to close the fridge.",
                "Error: not present",
                JOptionPane.ERROR_MESSAGE);
        } catch (TakeoverException ex) {
            JOptionPane.showMessageDialog(this,
                "You are currently acting as the backup controller of the system, due to a failure of the main controller. During this time, you cannot perform any actions.",
                "Error: controller takeover",
                JOptionPane.ERROR_MESSAGE);
        }
        f_lightsModel = new LightsListModel();
        for (LightState state : lightstates) {
            f_lightsModel.addItem(state);
        }
        lstLights.setModel(f_lightsModel);
    }
    
    @Override
    public void update() {
        this.getLights();
    }
    
    private class LightsListModel implements ListModel {

        private List<LightState> f_lightStates;
        
        public LightsListModel() {
            f_lightStates = new Vector<LightState>();
        }
        
        @Override
        public int getSize() {
            return f_lightStates.size();
        }

        @Override
        public Object getElementAt(int index) {
            return f_lightStates.get(index);
        }

        public void addItem(LightState lightstate) {
            f_lightStates.add(lightstate);
        }
        
        public List<LightState> getStates() {
            return f_lightStates;
        }
        
        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
        }
        
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLightsOff;
    private javax.swing.JButton btnLightsOn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<String> lstLights;
    // End of variables declaration//GEN-END:variables
}
