package org.jabref.gui;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jabref.Globals;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.preferences.JabRefPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if canceled.
 */
public class EntryTypeDialog extends BaseDialog {

  private static final int COLUMN = 3;
  private static final int GAP = 5;
  private static final int BUTTON_WIDTH = 135;
  private static final int BUTTON_HEIGHT = 30;
  private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeDialog.class);
  private JabRefFrame jabRefFrame;
  private Stage stage;
  private DialogPane dialogPane;
  private Parent root;
  private EntryType type;

  private double height = 300;


  private FetcherTask fetcherTask = new FetcherTask();
  private Button cancelButton;
  private Button generateButton;
  private TextField idTextField;
  private ComboBox<String> typeComboBox;

  public EntryTypeDialog(JabRefFrame frame) {
    this.jabRefFrame = frame;

    stage = (Stage) this.getDialogPane().getScene().getWindow();
    stage.setOnCloseRequest(event -> this.close());

    this.setTitle("Select entry type");

    try {
      dialogPane = FXMLLoader.load(getClass().getResource("./EntryTypeDialog.fxml"));
      root = dialogPane;

      this.setDialogPane(dialogPane);
      this.setResizable(true);
      this.entryInit();
      this.cancelButtonInit();
      this.searchButtonInit();
      this.setResizable(false);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void entryInit() {
    VBox vBox = (VBox) root.lookup("#entryBox");
    if (jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().isBiblatexMode()) {
      vBox.getChildren().add(addEntryListToPane("biblatex", BiblatexEntryTypes.ALL));

      List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
      if (!customTypes.isEmpty()) {
        vBox.getChildren().add(addEntryListToPane("Custom", customTypes));
      }
    } else {
      vBox.getChildren().add(addEntryListToPane("BibTex", BibtexEntryTypes.ALL));
      vBox.getChildren().add(addEntryListToPane("IEEETran", IEEETranEntryTypes.ALL));

      List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX);
      if (!customTypes.isEmpty()) {
        vBox.getChildren().add(addEntryListToPane("Custom", customTypes));
      }
    }
    dialogPane.setPrefHeight(height);
    dialogPane.setPrefWidth(COLUMN * (GAP + BUTTON_WIDTH + GAP));
  }

  private BorderPane addEntryListToPane(String groupTitle,
      Collection<? extends EntryType> entries) {
    BorderPane borderPane = new BorderPane();
    Label title = new Label(groupTitle);
    borderPane.setPadding(new Insets(GAP, GAP, GAP, GAP));
    borderPane.setTop(title);

    int column = 0;
    int row = 0;

    GridPane gridPane = new GridPane();
    gridPane.setHgap(GAP);
    gridPane.setVgap(GAP);

    height += (GAP * 2 + BUTTON_HEIGHT);
    for (EntryType entry : entries) {
      TypeButton button = new TypeButton(entry.getName(), entry);

      button.setPrefWidth(BUTTON_WIDTH);
      button.setPrefHeight(BUTTON_HEIGHT);

      button.setOnAction(event -> {
        type = button.getType();
        cancelAction();
      });
      gridPane.add(button, column, row);

      column++;
      if (column == COLUMN) {
        column = 0;
        row++;
        height += (BUTTON_HEIGHT + GAP);
      }
    }

    borderPane.setCenter(gridPane);
    borderPane.setBottom(new Separator());
    return borderPane;
  }

  public EntryType getChoice() {
    return type;
  }

  private void searchButtonInit() {
    generateButton = (Button) root.lookup("#generateButton");
    generateButton.setOnAction(event -> new Thread(fetcherTask).start());

    idTextField = (TextField) root.lookup("#idTextField");

    typeComboBox = (ComboBox<String>) root.lookup("#typeComboBox");

    WebFetchers.getIdBasedFetchers(Globals.prefs.getImportFormatPreferences())
        .forEach(idBasedFetcher -> typeComboBox.getItems().add(idBasedFetcher.getName()));

    typeComboBox.setValue(Globals.prefs.get(JabRefPreferences.ID_ENTRY_GENERATOR));
    typeComboBox.setOnAction(event -> {
      idTextField.requestFocus();
      idTextField.selectAll();
    });

    idTextField.requestFocus();
  }

  private void cancelButtonInit() {
    cancelButton = (Button) root.lookup("#cancelButton");
    cancelButton.setCancelButton(true);
    cancelButton.setOnAction(event -> cancelAction());
  }

  private void cancelAction() {
    if (!fetcherTask.isCancelled()) {
      fetcherTask.cancel();
    }
    stage.close();
  }

  class FetcherTask extends Task<Optional<BibEntry>> {

    private boolean fetcherException = false;
    private String fetcherExceptionMessage = "";
    private IdBasedFetcher fetcher = null;
    private String searchID = "";


    @Override
    protected Optional<BibEntry> call() throws Exception {
      Optional<BibEntry> bibEntry = Optional.empty();
      Platform.runLater(() -> {
        generateButton.setText("Searching...");
        generateButton.setDisable(true);
      });

      Globals.prefs
          .put(JabRefPreferences.ID_ENTRY_GENERATOR, String.valueOf(typeComboBox.getValue()));
      fetcher = WebFetchers.getIdBasedFetchers(Globals.prefs.getImportFormatPreferences())
          .get(typeComboBox.getItems().indexOf(typeComboBox.getValue()));
      searchID = idTextField.getText();
      if (!searchID.isEmpty()) {
        try {
          bibEntry = fetcher.performSearchById(searchID);
        } catch (FetcherException e) {
          LOGGER.error(e.getMessage(), e);
          fetcherException = true;
          fetcherExceptionMessage = e.getMessage();
        }
      }
      return bibEntry;
    }

    @Override
    protected void succeeded() {
      try {
        Optional<BibEntry> result = get();
        if (result.isPresent()) {
          final BibEntry bibEntry = result.get();
          if ((DuplicateCheck
              .containsDuplicate(jabRefFrame.getCurrentBasePanel().getDatabase(), bibEntry,
                  jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().getMode())
              .isPresent())) {
            //If there are duplicates starts ImportInspectionDialog
            final BasePanel panel = jabRefFrame.getCurrentBasePanel();

            ImportInspectionDialog diag = new ImportInspectionDialog(jabRefFrame, panel,
                Localization
                    .lang("Import"), false);
            diag.addEntries(Collections.singletonList(bibEntry));
            diag.entryListComplete();
            diag.setVisible(true);
            diag.toFront();
          } else {
            // Regenerate CiteKey of imported BibEntry
            new BibtexKeyGenerator(jabRefFrame.getCurrentBasePanel().getBibDatabaseContext(),
                Globals.prefs.getBibtexKeyPatternPreferences()).generateAndSetKey(bibEntry);
            // Update Timestamps
            if (Globals.prefs.getTimestampPreferences().includeCreatedTimestamp()) {
              bibEntry.setField(Globals.prefs.getTimestampPreferences().getTimestampField(),
                  Globals.prefs.getTimestampPreferences().now());
            }
            jabRefFrame.getCurrentBasePanel().insertEntry(bibEntry);
          }

          close();
        } else if (searchID.trim().isEmpty()) {
          jabRefFrame.getDialogService()
              .showWarningDialogAndWait(Localization.lang("Empty search ID"),
                  Localization.lang("The given search ID was empty."));
        } else if (!fetcherException) {
          jabRefFrame.getDialogService().showErrorDialogAndWait(Localization.lang("No files found.",
              Localization
                  .lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher.getName(),
                      searchID) + "\n" + fetcherExceptionMessage));
        } else {
          jabRefFrame.getDialogService().showErrorDialogAndWait(Localization.lang("Error"),
              Localization.lang("Error while fetching from %0", fetcher.getName()) + "." + "\n"
                  + fetcherExceptionMessage);
        }
        fetcherTask = new FetcherTask();
//        Platform.runLater(() -> {
        idTextField.requestFocus();
        idTextField.selectAll();
        generateButton.setText("Generate");
        generateButton.setDisable(false);
//        });
      } catch (ExecutionException | InterruptedException e) {
        LOGGER.error(String
            .format("Exception during fetching when using fetcherTask '%s' with entry id '%s'.",
                searchID, fetcher.getName()), e);
      }
    }
  }

  class TypeButton extends Button implements Comparable<TypeButton> {

    private final EntryType type;

    TypeButton(String label, EntryType type) {
      super(label);
      this.type = type;
    }

    public EntryType getType() {
      return this.type;
    }

    @Override
    public int compareTo(TypeButton typeButton) {
      return type.getName().compareTo(typeButton.getType().getName());
    }
  }
}
