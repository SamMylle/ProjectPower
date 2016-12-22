/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Class used to display dialogs according to exceptions thrown
 * @author federico
 */
public class DialogExceptions {
    static public void notifyTakeover(JPanel panel) {
        JOptionPane.showMessageDialog(panel,
            "You are currently acting as the backup controller of the system, due to a failure of the main controller. During this time, you cannot perform any actions.",
            "Error: controller takeover",
            JOptionPane.ERROR_MESSAGE);
    }
    
    static public void notifyNoFridgeConnection(JPanel panel) {
        JOptionPane.showMessageDialog(panel,
            "Something went wrong during the connection setup with the fridge, please try again.",
            "Error: fridge connection setup",
            JOptionPane.ERROR_MESSAGE);
    }
    
    static public void notifyAbsent(JPanel panel, String reason) {
        JOptionPane.showMessageDialog(panel,
            "You should be present in the house before " + reason + ".",
            "Error: not present",
            JOptionPane.ERROR_MESSAGE);
    }
    
    static public void notifyMultipleInteraction(JPanel panel) {
        JOptionPane.showMessageDialog(panel,
            "You are currently connected to a fridge, close the connection first.",
            "Error: fridge connection",
            JOptionPane.ERROR_MESSAGE);
    }
    
    static public void notifyFridgeOccupied(JPanel panel) {
        JOptionPane.showMessageDialog(panel,
            "The fridge you are trying to communicate with is already occupied by another user.",
            "Error: fridge occupied",
            JOptionPane.ERROR_MESSAGE);
    }
}
