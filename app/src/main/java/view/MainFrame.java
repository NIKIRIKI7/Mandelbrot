package view;

import render.FractalRenderer;
import services.FileService;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The main application window for the Mandelbrot Set Explorer.
 */
public class MainFrame extends JFrame {

    private final FractalViewModel viewModel;
    private final FractalRenderer renderer;
    private final FileService fileService;
    private final FractalPanel fractalPanel;

    /**
     * Constructs the main application frame.
     */
    public MainFrame() {
        renderer = new FractalRenderer();
        viewModel = new FractalViewModel(renderer);
        fileService = new FileService();

        setTitle("Mandelbrot Set Explorer");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        fractalPanel = new FractalPanel(viewModel, renderer);
        MenuBar menuBar = new MenuBar(viewModel, fileService, fractalPanel, this);

        setJMenuBar(menuBar);
        add(fractalPanel, BorderLayout.CENTER);
        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
    }

    /**
     * Handles window closing with a confirmation dialog.
     */
    private void handleWindowClose() {
        int confirmation = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to exit?", "Confirm Exit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Shutting down renderer...");
            renderer.shutdown();
            System.out.println("Exiting application.");
            dispose();
            System.exit(0);
        }
    }

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments (unused).
     */
    public static void main(String[] args) {
        // Set the system look and feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look and feel.");
        }

        // Launch the application on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}