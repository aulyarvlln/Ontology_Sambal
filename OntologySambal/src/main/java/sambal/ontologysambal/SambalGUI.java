package sambal.ontologysambal;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

public class SambalGUI extends JFrame {

    private static final String NS =
            "http://www.semanticweb.org/aulia/ontologies/2026/8/untitled-ontology-4#";

    private Model model;

    private final JLabel lblStatus = new JLabel("Belum ada ontologi yang dimuat.");
    private final JComboBox<String> cbRelasi = new JComboBox<>(
            new String[]{"DisajikanUntuk", "DibuatDenganAlat"});
    private final JTextField txtKeyword = new JTextField(15);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Jenis Sambal", "Hasil Relasi"}, 0);

    public SambalGUI() {
        setTitle("Pencarian Ontologi Sambal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JButton btnMuat = new JButton("Muat Ontologi");
        JButton btnCari = new JButton("Cari");

        JPanel panelAtas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelAtas.add(btnMuat);
        panelAtas.add(new JLabel("Relasi:"));
        panelAtas.add(cbRelasi);
        panelAtas.add(new JLabel("Kata kunci:"));
        panelAtas.add(txtKeyword);
        panelAtas.add(btnCari);

        JTable tabel = new JTable(tableModel);

        add(panelAtas, BorderLayout.NORTH);
        add(new JScrollPane(tabel), BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);

        btnMuat.addActionListener(e -> muatOntologi());
        btnCari.addActionListener(e -> cariData());
    }

    private void muatOntologi() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fc.getSelectedFile();
        try {
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, file.toURI().toString());
            lblStatus.setText("Berhasil dimuat: " + file.getName()
                    + " (" + model.size() + " triple)");
        } catch (Exception ex) {
            model = null;
            JOptionPane.showMessageDialog(this, "Gagal memuat file:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cariData() {
        if (model == null) {
            JOptionPane.showMessageDialog(this, "Muat file ontologi dulu ya.");
            return;
        }

        String relasi = (String) cbRelasi.getSelectedItem();
        String kw = txtKeyword.getText().trim()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        String sparql
                = "PREFIX : <" + NS + ">\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT DISTINCT ?sambal ?sambalLabel ?objek ?objekLabel\n"
                + "WHERE {\n"
                + "  ?sambal :" + relasi + " ?objek .\n"
                + "  OPTIONAL { ?sambal rdfs:label ?sambalLabel . }\n"
                + "  OPTIONAL { ?objek rdfs:label ?objekLabel . }\n"
                + "  FILTER( regex(str(?objek), \"" + kw + "\", \"i\")"
                + " || regex(str(?objekLabel), \"" + kw + "\", \"i\") )\n"
                + "}";

        try (QueryExecution qe = QueryExecutionFactory.create(sparql, model)) {
            ResultSet hasil = qe.execSelect();
            tableModel.setRowCount(0);
            while (hasil.hasNext()) {
                QuerySolution s = hasil.next();
                tableModel.addRow(new Object[]{
                    ambilNama(s, "sambal", "sambalLabel"),
                    ambilNama(s, "objek", "objekLabel")
                });
            }
            lblStatus.setText("Ditemukan " + tableModel.getRowCount() + " hasil.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Query gagal:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String ambilNama(QuerySolution s, String varNode, String varLabel) {
        if (s.contains(varLabel)) {
            return s.getLiteral(varLabel).getString();
        }
        RDFNode node = s.get(varNode);
        if (node.isURIResource()) {
            return node.asResource().getLocalName();
        }
        return node.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SambalGUI().setVisible(true));
    }
}