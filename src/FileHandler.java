//
// Decompiled by Procyon v0.5.36
//


import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.Optional;

public class FileHandler {

    public FileHandler() {
    }

    public JFileChooser openPopup() {
        final JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File f) {
                if (f.isDirectory()) {
                    return true;
                }
                final Optional<String> extensionOp = this.getExtension(f.getName());
                if (extensionOp.isPresent()) {
                    final String extension = extensionOp.get();
                    return extension.equalsIgnoreCase("stp");
                }
                return false;
            }

            @Override
            public String getDescription() {
                return ".stp";
            }

            public Optional<String> getExtension(final String filename) {
                return Optional.ofNullable(filename).filter(f -> f.contains(".")).map(f -> f.substring(filename.lastIndexOf(".") + 1));
            }
        };
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select STEP File");
        fileChooser.showOpenDialog(null);
        return fileChooser;
    }

    public BufferedReader getFileReader(final JFileChooser fileChooser) throws FileNotFoundException {
        final File file = fileChooser.getSelectedFile();
        final FileReader fileReader = new FileReader(file);
        return new BufferedReader(fileReader);
    }

    public PrintWriter getFileWriter(String fileName) throws IOException {
        return new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
    }


    public JFileChooser openPopupDirectory() {
        final JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fileChooser.setCurrentDirectory(new java.io.File("."));
        fileChooser.setDialogTitle("Select output directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.showOpenDialog(null);
        return fileChooser;
    }
}
