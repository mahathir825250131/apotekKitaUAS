package main.java.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import main.java.model.Pembayaran;

/** Jendela untuk menampilkan (dan mencetak) struk pembayaran. */
public final class StrukWindow {
    private StrukWindow() { }

    public static void show(Pembayaran pembayaran, Window owner) {
        TextArea struk = new TextArea(pembayaran.getStruk());
        struk.setEditable(false);
        struk.getStyleClass().add("struk");
        VBox.setVgrow(struk, javafx.scene.layout.Priority.ALWAYS);

        Stage stage = new Stage();
        Button cetak = new Button("Print");
        cetak.getStyleClass().add("custom-button");
        cetak.setOnAction(e -> print(struk, stage));
        Button tutup = new Button("Tutup");
        tutup.getStyleClass().add("button-secondary");
        tutup.setOnAction(e -> stage.close());

        HBox tombol = new HBox(10, cetak, tutup);
        tombol.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(10, struk, tombol);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 430, 640);
        scene.getStylesheets().add(StrukWindow.class.getResource("/main/resources/css/style.css").toExternalForm());
        stage.setTitle("Struk - BILL# " + pembayaran.getBillNumber());
        stage.setScene(scene);
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        }
        stage.show();
    }

    private static void print(TextArea struk, Stage owner) {
        // createPrinterJob mengembalikan null jika tidak ada printer
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(owner)) {
            if (job.printPage(struk)) {
                job.endJob();
            }
        }
    }
}
