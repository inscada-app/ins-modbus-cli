package com.inscada.modbus.client.controllers;

import com.inscada.modbus.client.model.*;
import com.inscada.modbus.client.services.*;
import com.inscada.modbus.client.utils.Constraints;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger("Main Controller");
    private final ConnectionService connectionService;
    private final DataBlockService dataBlockService;
    private final ConfigService configService;
    private final Console console;
    private final SetValueController setValueController;
    private final SimpleBooleanProperty isStarted = new SimpleBooleanProperty();
    private final SimpleBooleanProperty isConnected = new SimpleBooleanProperty();
    private final StringConverter<Number> numberStringConverter = new StringConverter<>() {
        @Override
        public String toString(Number number) {
            return number == null ? "" : number.toString();
        }

        @Override
        public Number fromString(String s) {
            return s == null || s.isBlank() ? null : Integer.parseInt(s); //TODO maybe parse number not int?
        }
    };
    private final SimpleObjectProperty<ConsoleLogLevel> logLevelFunc = new SimpleObjectProperty<>();
    @FXML
    Button btnStart;
    @FXML
    Button btnStop;
    @FXML
    Button btnDataBlockAdd;
    @FXML
    Button btnDataBlockDelete;
    @FXML
    Button btnDataBlockUpdate;
    @FXML
    Button btnDataBlockClear;
    @FXML
    Button btnTagAdd;
    @FXML
    Button btnTagDelete;
    @FXML
    Button btnTagUpdate;
    @FXML
    Button btnTagClear;
    @FXML
    Button btnExportLog;
    @FXML
    TableView<DataBlock> tblViewDataBlock;
    @FXML
    TableView<Tag> tblViewTags;
    @FXML
    TableColumn<DataBlock, String> tblColumnDBName;
    @FXML
    TableColumn<DataBlock, Integer> tblColumnDBAmount;
    @FXML
    TableColumn<DataBlock, String> tblColumnDBType;
    @FXML
    TableColumn<DataBlock, Integer> tblColumnDBStartAddress;
    @FXML
    TableColumn<Tag, String> tblColumnTagName;
    @FXML
    TableColumn<Tag, Integer> tblColumnTagAddress;
    @FXML
    TableColumn<Tag, Object> tblColumnTagType;
    @FXML
    TableColumn<Tag, Boolean> tblColumnTagWordSwap;
    @FXML
    TableColumn<Tag, Boolean> tblColumnTagByteSwap;
    @FXML
    TableColumn<Tag, Object> tblColumnTagValue;
    @FXML
    TableColumn<Tag, LocalDateTime> tblColumnTagDate;
    @FXML
    TableColumn<Tag, String> tblColumnTagDBName;
    @FXML
    TextField txtFieldDBName;
    @FXML
    TextField txtFieldDBAmount;
    @FXML
    TextField txtFieldDBStartAddress;
    @FXML
    TextField txtFieldTimeout;
    @FXML
    TextField txtFieldIpAddr;
    @FXML
    TextField txtFieldUnitAdd;
    @FXML
    TextField txtFieldPort;
    @FXML
    TextField txtFieldScanTime;
    @FXML
    TextField txtFieldDBSearch;
    @FXML
    TextField txtFieldTagName;
    @FXML
    TextField txtFieldTagAddress;
    @FXML
    TextField txtFieldTagSearch;
    @FXML
    TextField txtFieldLogSearch;
    @FXML
    ComboBox<ConnectionType> comboBoxConnType;
    @FXML
    ComboBox<DataBlockType> comboBoxDBType;
    @FXML
    ComboBox<TagType> comboBoxTagType;
    @FXML
    Text textConnStatus;
    @FXML
    Circle iconConnStatus;
    @FXML
    CheckBox checkBoxByteSwap;
    @FXML
    CheckBox checkBoxWordSwap;
    @FXML
    MenuItem menuItemSave;
    @FXML
    MenuItem menuItemLoad;
    @FXML
    MenuItem menuItemExit;
    @FXML
    MenuItem menuItemDocumentation;
    @FXML
    MenuItem menuItemModbusManual;
    @FXML
    MenuItem menuItemAbout;
    @FXML
    MenuItem menuItemReset;
    @FXML
    MenuItem menuItemClearLogs;
    @FXML
    MenuItem menuItemFunctionInputs;
    @FXML
    MenuItem menuItemFunctionRegisters;
    @FXML
    MenuItem menuItemFunctionMaskWrite;
    @FXML
    MenuItem menuItemFunctionReadWrite;
    @FXML
    ListView<String> listViewLog;
    @FXML
    ComboBox<ConsoleLogLevel> comboBoxLogLevel;
    @FXML
    Label labelErrorCount;
    @FXML
    Label labelSuccessCount;
    private DataBlockScanner dataBlockScanner;
    private Stage stage;
    private HostServices hostServices;

    public MainController() {
        this.connectionService = new ConnectionService();
        this.dataBlockService = new DataBlockService();
        this.configService = new ConfigService(connectionService, dataBlockService);
        this.console = new Console();
        this.setValueController = new SetValueController();
    }

    @Override
    public void initialize(URL location, ResourceBundle resourceBundle) {
        initIcons();
        initConstraints();
        initBindings();
        initValues();
        initLogs();
        initDataBlockTable();
        initTagTable();
    }

    public void getHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void initIcons() {
        ImageView saveIcon = new ImageView();
        saveIcon.setImage(new Image(getClass().getResourceAsStream("/icons/saveIcon.png")));
        saveIcon.setFitHeight(20);
        saveIcon.setFitWidth(20);
        menuItemSave.setGraphic(saveIcon);
        ImageView loadIcon = new ImageView();
        loadIcon.setImage(new Image(getClass().getResourceAsStream("/icons/loadIcon.png")));
        loadIcon.setFitHeight(20);
        loadIcon.setFitWidth(20);
        menuItemLoad.setGraphic(loadIcon);
        ImageView exitIcon = new ImageView();
        exitIcon.setImage(new Image(getClass().getResourceAsStream("/icons/exitIcon.png")));
        exitIcon.setFitHeight(19);
        exitIcon.setFitWidth(19);
        menuItemExit.setGraphic(exitIcon);
        ImageView modbusDocIcon = new ImageView();
        modbusDocIcon.setImage(new Image(getClass().getResourceAsStream("/icons/docIcon.png")));
        modbusDocIcon.setFitHeight(20);
        modbusDocIcon.setFitWidth(20);
        menuItemModbusManual.setGraphic(modbusDocIcon);
        ImageView infoIcon = new ImageView();
        infoIcon.setImage(new Image(getClass().getResourceAsStream("/icons/infoIcon.png")));
        infoIcon.setFitHeight(20);
        infoIcon.setFitWidth(20);
        menuItemAbout.setGraphic(infoIcon);
        ImageView docIcon = new ImageView();
        docIcon.setImage(new Image(getClass().getResourceAsStream("/icons/docIcon.png")));
        docIcon.setFitHeight(20);
        docIcon.setFitWidth(20);
        menuItemDocumentation.setGraphic(docIcon);
        ImageView resetIcon = new ImageView();
        resetIcon.setImage(new Image(getClass().getResourceAsStream("/icons/restartIcon.png")));
        resetIcon.setFitHeight(20);
        resetIcon.setFitWidth(20);
        menuItemReset.setGraphic(resetIcon);
        ImageView clearIcon = new ImageView();
        clearIcon.setImage(new Image(getClass().getResourceAsStream("/icons/clearIcon.png")));
        clearIcon.setFitHeight(20);
        clearIcon.setFitWidth(20);
        menuItemClearLogs.setGraphic(clearIcon);
        ImageView oneIcon = new ImageView();
        oneIcon.setImage(new Image(getClass().getResourceAsStream("/icons/commandIcon.png")));
        oneIcon.setFitHeight(20);
        oneIcon.setFitWidth(20);
        menuItemFunctionInputs.setGraphic(oneIcon);
        ImageView twoIcon = new ImageView();
        twoIcon.setImage(new Image(getClass().getResourceAsStream("/icons/commandIcon.png")));
        twoIcon.setFitHeight(20);
        twoIcon.setFitWidth(20);
        menuItemFunctionRegisters.setGraphic(twoIcon);
        ImageView threeIcon = new ImageView();
        threeIcon.setImage(new Image(getClass().getResourceAsStream("/icons/commandIcon.png")));
        threeIcon.setFitHeight(20);
        threeIcon.setFitWidth(20);
        menuItemFunctionMaskWrite.setGraphic(threeIcon);
        ImageView fourIcon = new ImageView();
        fourIcon.setImage(new Image(getClass().getResourceAsStream("/icons/commandIcon.png")));
        fourIcon.setFitHeight(20);
        fourIcon.setFitWidth(20);
        menuItemFunctionReadWrite.setGraphic(fourIcon);
    }

    public void shutdown() {
        stop();
        console.close();
    }

    private void initConstraints() {
        Constraints.setIPTextField(txtFieldIpAddr);
        Constraints.setPortTextField(txtFieldPort);
        Constraints.setIntTextField(txtFieldUnitAdd);
        Constraints.setIntTextField(txtFieldTimeout);
        Constraints.setIntTextField(txtFieldScanTime);
        Constraints.setIntTextField(txtFieldDBStartAddress);
        Constraints.setIntTextField(txtFieldDBAmount);
        Constraints.setIntTextField(txtFieldTagAddress);
    }

    private void initBindings() {
        connectionService.setConnectionStatusListener(isConnected::set);
        connectionService.connectionProperty().addListener((observableValue, oldConn, newConn) -> setConnectionFields(newConn));

        btnStart.disableProperty().bind(isStarted);
        btnStop.disableProperty().bind(Bindings.not(isStarted));
        menuItemLoad.disableProperty().bind(isStarted);

        textConnStatus.textProperty().bind(Bindings.when(isConnected).then("Connected").otherwise("Not Connected"));
        iconConnStatus.fillProperty().bind(Bindings.when(isConnected).then(Color.GREEN).otherwise(Color.RED));

        labelSuccessCount.textProperty().bind(Statistics.successCountProperty().asString());
        labelErrorCount.textProperty().bind(Statistics.errorCountProperty().asString());

        comboBoxLogLevel.valueProperty().bindBidirectional(logLevelFunc);
        logLevelFunc.addListener((observableValue, consoleLogLevel, newLogLevel) -> console.setLogLevel(newLogLevel));
    }

    private void initValues() {
        comboBoxConnType.setItems(FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(ConnectionType.values())));

        comboBoxDBType.setItems(FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(DataBlockType.values())));

        comboBoxTagType.setItems(FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(TagType.values())));

        ObservableList<ConsoleLogLevel> logLevelObservableList = FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(ConsoleLogLevel.values()));
        comboBoxLogLevel.setItems(logLevelObservableList);
        logLevelFunc.setValue(logLevelObservableList.get(0));

        setConnectionFields(connectionService.getConnection());

    }

    private void initLogs() {
        txtFieldLogSearch.textProperty().addListener((observableValue, oldValue, newValue) ->
                console.setFilter(newValue));

        console.setChangeListener(() -> {
            if (txtFieldLogSearch.getText() == null || txtFieldLogSearch.getText().isBlank()) {
                int size = listViewLog.getItems().size();
                if (size > 0) {
                    listViewLog.scrollTo(size - 1);
                }
            }
        });

        listViewLog.setItems(console.getLogs());
        listViewLog.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewLog.setCellFactory(listView -> new ListCell<>() {
            final Pattern pattern = Pattern.compile("(ERROR|WARN|INFO)");

            @Override
            protected void updateItem(String log, boolean empty) {
                super.updateItem(log, empty);
                setText(log);
                if (log != null && !log.isBlank()) {
                    Matcher matcher = pattern.matcher(log);
                    if (matcher.find()) {
                        String level = matcher.group();
                        if (level.equalsIgnoreCase("WARN")) {
                            setTextFill(Color.DARKORANGE);
                        } else if (level.equalsIgnoreCase("ERROR")) {
                            setTextFill(Color.RED);
                        } else if (level.equalsIgnoreCase("INFO")) {
                            setTextFill(Color.BLUE);
                        }
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        listViewMouseClickAction();
    }

    private void listViewMouseClickAction() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent clipboardContent = new ClipboardContent();

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(actionEvent -> {
            List<String> selectedLogs = listViewLog.getSelectionModel().getSelectedItems();
            if (selectedLogs == null) {
                return;
            }
            clipboardContent.putString(selectedLogs.stream().collect(Collectors.joining()));
            clipboard.setContent(clipboardContent);
        });

        MenuItem clear = new MenuItem("Clear");
        clear.setOnAction(actionEvent -> {
            console.clear();
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(copy);
        contextMenu.getItems().add(clear);
        listViewLog.setContextMenu(contextMenu);
    }

    private void initDataBlockTable() {
        tblColumnDBName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tblColumnDBType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tblColumnDBStartAddress.setCellValueFactory(new PropertyValueFactory<>("startAddress"));
        tblColumnDBAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        txtFieldDBSearch.textProperty()
                .addListener((observableValue, oldValue, newValue) -> dataBlockService.changeDataBlockFilter(newValue));

        tblViewDataBlock.getSelectionModel().selectedItemProperty().addListener((observableValue, oldDataBlock, newDataBlock) ->
        {
            if (newDataBlock == null) {
                clearDataBlockFields();
                tblViewTags.setItems(dataBlockService.findTags());
                return;
            }
            txtFieldDBName.setText(newDataBlock.getName());
            txtFieldDBStartAddress.setText(Integer.toString(newDataBlock.getStartAddress()));
            comboBoxDBType.setValue(newDataBlock.getType());
            txtFieldDBAmount.setText(Integer.toString(newDataBlock.getAmount()));
            tblViewTags.setItems(dataBlockService.findTags(newDataBlock.getName()));

            log.info("Data Block: " + newDataBlock.getName() + " selected");
        });

        dataBlockService.comparatorDataBlockProperty().bind(tblViewDataBlock.comparatorProperty());
        tblColumnDBName.setSortType(TableColumn.SortType.ASCENDING);
        tblViewDataBlock.getSortOrder().add(tblColumnDBName);
        tblViewDataBlock.sort();
        tblViewDataBlock.setItems(dataBlockService.findDataBlocks());
    }

    private void initTagTable() {
        tblViewTags.setEditable(true);
        tblColumnTagDBName.setCellValueFactory(t -> t.getValue().dataBlockNameProperty());
        tblColumnTagName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tblColumnTagType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tblColumnTagAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        tblColumnTagByteSwap.setCellValueFactory(new PropertyValueFactory<>("byteSwap"));
        tblColumnTagWordSwap.setCellValueFactory(new PropertyValueFactory<>("wordSwap"));
        tblColumnTagValue.setCellValueFactory(cd ->
                Bindings.createObjectBinding(() -> cd.getValue().valueProperty().getValue().getVal(), cd.getValue().valueProperty()));
        tblColumnTagDate.setCellValueFactory(cd ->
                Bindings.createObjectBinding(() -> cd.getValue().valueProperty().getValue().getDate(), cd.getValue().valueProperty()));
        tblColumnTagDate.setCellFactory(cd -> {
            TableCell<Tag, LocalDateTime> cell = new TableCell<>() {
                private DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

                @Override
                protected void updateItem(LocalDateTime localDateTime, boolean b) {
                    super.updateItem(localDateTime, b);
                    if (b) {
                        setText(null);
                    } else {
                        this.setText(format.format(localDateTime));
                    }
                }
            };
            return cell;
        });

        txtFieldTagSearch.textProperty()
                .addListener((observableValue, oldValue, newValue) -> dataBlockService.changeTagFilter(newValue));

        tblViewTags.getSelectionModel().selectedItemProperty().addListener((observableValue, oldTag, newTag) -> {
            if (newTag == null) {
                clearTagFields();
                return;
            }
            txtFieldTagName.setText(newTag.getName());
            txtFieldTagAddress.setText(Integer.toString(newTag.getAddress()));
            comboBoxTagType.setValue(newTag.getType());
            checkBoxByteSwap.setSelected(newTag.isByteSwap());
            checkBoxWordSwap.setSelected(newTag.isWordSwap());

            log.info("Tag: " + newTag.getName() + " selected");
        });

        dataBlockService.comparatorTagProperty().bind(tblViewTags.comparatorProperty());
        tblColumnTagName.setSortType(TableColumn.SortType.ASCENDING);
        tblViewTags.getSortOrder().add(tblColumnTagName);
        tblViewTags.sort();

        tblViewTags.setRowFactory(tableView -> {
            TableRow<Tag> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton().equals(MouseButton.PRIMARY)) {
                    if (btnStart.isDisabled()) {
                        Tag tag = row.getItem();
                        if (tag != null) {
                            setValueController.showModal(tag);
                            String newValue = setValueController.getNewValue();
                            if (newValue != null && !newValue.isBlank()) {
                                dataBlockScanner.setValue(tag, newValue);
                            }
                        }
                    }
                }
            });
            return row;
        });

        tblViewTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        addChartMenu();
    }

    private void addChartMenu() {
        MenuItem chart = new MenuItem("Chart");
        chart.setOnAction(actionEvent -> {
            ObservableList<Tag> selectedItems = tblViewTags.getSelectionModel().getSelectedItems();
            if (selectedItems != null) {
                ChartController chartController = new ChartController(selectedItems);
                chartController.showStage();
            }
        });
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(chart);
        tblViewTags.setContextMenu(contextMenu);
    }

    private void addDataBlock() {
        DataBlock dataBlock = extractDataBlock();
        Result<DataBlock> result = dataBlockService.addDataBlock(dataBlock);
        if (result.hasError()) {
            alertErrors("Data Block Add Error", result.getErrors(), true);
        } else {
            log.info("Data Block: New data block added");
            tblViewDataBlock.getSelectionModel().select(result.getVal());
        }
    }

    private void updateDataBlock() {
        DataBlock selectedDataBlock = tblViewDataBlock.getSelectionModel().getSelectedItem();
        if (selectedDataBlock != null) {
            DataBlock dataBlock = extractDataBlock();
            Result<DataBlock> result = dataBlockService.updateDataBlock(selectedDataBlock.getName(), dataBlock);
            if (result.hasError()) {
                alertErrors("Data Block Update Error", result.getErrors(), true);
            } else {
                log.info("Data Block: Data block updated");
                tblViewDataBlock.getSelectionModel().select(result.getVal());
            }
        } else {
            alertWarning("No Selection", "Data Block: No selection found", true);
        }
    }

    private void deleteDataBlock() {
        DataBlock selectedDataBlock = tblViewDataBlock.getSelectionModel().getSelectedItem();
        if (selectedDataBlock != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Are you sure to delete selected Data Block ?");
            ButtonType result = alert.showAndWait().orElse(null);
            if (result == ButtonType.OK) {
                Result<DataBlock> dataBlockResult = dataBlockService.deleteDataBlock(selectedDataBlock.getName());
                if (dataBlockResult.hasError()) {
                    alertErrors("Data Block Delete Error", dataBlockResult.getErrors(), false);
                } else {
                    log.info("Data Block: Selected data block deleted");
                }
            }
        } else {
            alertWarning("No Selection", "Data Block: No selection found", true);
        }
    }

    private void clearDataBlockFields() {
        txtFieldDBName.clear();
        txtFieldDBAmount.clear();
        txtFieldDBStartAddress.clear();
        comboBoxDBType.setValue(null);
        log.info("Data Block: Fields cleared");
    }

    private DataBlock extractDataBlock() {
        Number startAddress = numberStringConverter.fromString(txtFieldDBStartAddress.getText());
        Number amount = numberStringConverter.fromString(txtFieldDBAmount.getText());
        DataBlockType type = comboBoxDBType.getValue();
        String name = txtFieldDBName.getText();
        return new DataBlock(name, type, (Integer) startAddress, (Integer) amount);
    }

    private void addTag() {
        DataBlock dataBlock = tblViewDataBlock.getSelectionModel().getSelectedItem();
        if (dataBlock != null) {
            Tag tag = extractTag();
            Result<Tag> tagResult = dataBlockService.addTag(tag);
            if (tagResult.hasError()) {
                alertErrors("Tag Add Error", tagResult.getErrors(), true);
            } else {
                log.info("Tag: New Tag added");
                tblViewTags.getSelectionModel().select(tagResult.getVal());
            }
        } else {
            alertWarning("No Selection", "Data Block: No selection found", true);
        }
    }

    private void updateTag() {
        DataBlock dataBlock = tblViewDataBlock.getSelectionModel().getSelectedItem();
        Tag tag = tblViewTags.getSelectionModel().getSelectedItem();
        if (dataBlock != null && tag != null) {
            Result<Tag> tagResult = dataBlockService.updateTag(tag.getDataBlockName(), tag.getName(), extractTag());
            if (tagResult.hasError()) {
                alertErrors("Tag Update Error", tagResult.getErrors(), true);
            } else {
                log.info("Tag: Selected tag updated");
                tblViewTags.getSelectionModel().select(tagResult.getVal());
            }
        }
        if (tag == null) {
            alertWarning("No selection", "Tag: No selection found", true);
        }
        if (dataBlock == null) {
            alertWarning("No selection", "Data Block: No selection found", true);
        }
    }

    private void deleteTag() {
        Tag tag = tblViewTags.getSelectionModel().getSelectedItem();
        if (tag != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Are you sure to delete selected Tag ?");
            ButtonType result = alert.showAndWait().orElse(null);
            if (result == ButtonType.OK) {
                Result<Tag> tagResult = dataBlockService.deleteTag(tag.getDataBlockName(), tag.getName());
                if (tagResult.hasError()) {
                    alertErrors("Tag Delete Error", tagResult.getErrors(), false);
                } else {
                    log.info("Tag: Selected tag deleted");
                }
            }
        }
    }

    private void clearTagFields() {
        txtFieldTagAddress.clear();
        txtFieldTagName.clear();
        checkBoxByteSwap.setSelected(false);
        checkBoxWordSwap.setSelected(false);
        log.info("Tag: Fields cleared");
    }

    private Tag extractTag() {
        DataBlock dataBlock = tblViewDataBlock.getSelectionModel().getSelectedItem();
        String tagName = txtFieldTagName.getText();
        Number tagAddress = numberStringConverter.fromString(txtFieldTagAddress.getText());
        TagType type = comboBoxTagType.getValue();
        boolean wordSwap = checkBoxWordSwap.isSelected();
        boolean byteSwap = checkBoxByteSwap.isSelected();
        return new Tag(dataBlock.getName(), tagName, (Integer) tagAddress, type, byteSwap, wordSwap);
    }

    private void start() {
        log.info("Starting....");
        Statistics.clear();
        connectionService.setConnection(extractConnection());
        Result<Void> connectionResult = connectionService.connect();
        if (connectionResult.hasError()) {
            alertErrors("Connection Error", connectionResult.getErrors(), false);
            return;
        }
        Connection connection = connectionService.getConnection();
        InsModbusMaster modbusMaster = connectionService.getModbusMaster();
        dataBlockScanner = new DataBlockScanner(dataBlockService, modbusMaster, connection);
        dataBlockScanner.start();
        isStarted.set(true);
    }

    private void stop() {
        log.info("Stopping...");
        connectionService.disconnect();
        if (dataBlockScanner != null) {
            dataBlockScanner.stop();
        }
        isStarted.set(false);
    }

    private Connection extractConnection() {
        String ipAddr = txtFieldIpAddr.getText();
        Number port = numberStringConverter.fromString(txtFieldPort.getText());
        Number unitId = numberStringConverter.fromString(txtFieldUnitAdd.getText());
        Number timeout = numberStringConverter.fromString(txtFieldTimeout.getText());
        Number scanTime = numberStringConverter.fromString(txtFieldScanTime.getText());
        ConnectionType connectionType = comboBoxConnType.getValue();
        return new Connection(ipAddr, (Integer) port, (Integer) unitId,
                (Integer) timeout, (Integer) scanTime, connectionType);
    }

    private void setConnectionFields(Connection newConn) {
        txtFieldIpAddr.setText(newConn.getIpAddress());
        txtFieldPort.setText(numberStringConverter.toString(newConn.getPort()));
        txtFieldUnitAdd.setText(numberStringConverter.toString(newConn.getUnitId()));
        txtFieldTimeout.setText(numberStringConverter.toString(newConn.getTimeout()));
        txtFieldScanTime.setText(numberStringConverter.toString(newConn.getScanTime()));
        comboBoxConnType.setValue(newConn.getConnectionType());
    }

    private void save() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Config");
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Json File", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*"));
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        File saveFile = fileChooser.showSaveDialog(menuItemSave.getParentPopup().getScene().getWindow());
        if (saveFile == null) {
            return;
        }
        Result<Void> result = configService.save(saveFile);
        if (result.hasError()) {
            alertErrors("Save Config", result.getErrors(), false);
        }
    }

    private void load() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Config");
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Json File", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*"));
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        File loadFile = fileChooser.showOpenDialog(menuItemLoad.getParentPopup().getScene().getWindow());
        if (loadFile == null) {
            return;
        }
        Result<Void> result = configService.load(loadFile);
        if (result.hasError()) {
            alertErrors("Load Config", result.getErrors(), false);
        }
    }

    private void exportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Text File", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*"));
        File exportFile = fileChooser.showSaveDialog(btnExportLog.getParent().getScene().getWindow());
        if (exportFile == null) {
            return;
        }
        try {
            Files.write(exportFile.toPath(), listViewLog.getItems(), new StandardOpenOption[]{StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE});
        } catch (IOException e) {
            alertWarning("File Overwrite Error", "You cant overwrite any Text File for saving Log", false);
        }
    }

    private void alertErrors(String title, List<String> errors, boolean isWarning) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String error : errors) {
            stringBuilder.append(error).append(System.lineSeparator());
        }
        Alert alert = new Alert(isWarning ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(stringBuilder.toString());
        alert.show();
    }

    private void alertWarning(String title, String content, boolean isWarning) {
        Alert alert = new Alert(isWarning ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
        log.error(content);
    }

    private void coilDiscreteInputFunction() {
        CoilDiscreteInputController coilDiscreteInputController = new CoilDiscreteInputController(connectionService);
        coilDiscreteInputController.showStage();
    }

    private void holdingInputRegistersFunction() {
        HoldingInputRegistersController holdingInputRegistersController = new HoldingInputRegistersController(connectionService);
        holdingInputRegistersController.showStage();
    }

    private void maskWriteRegisterFunction() {
        MaskWriteRegisterController maskWriteRegisterController = new MaskWriteRegisterController(connectionService);
        maskWriteRegisterController.showStage();
    }

    private void readWriteRegistersFunction() {
        ReadWriteRegistersController readWriteRegistersController = new ReadWriteRegistersController(connectionService);
        readWriteRegistersController.showStage();
    }

    @FXML
    private void onSaveAction() {
        save();
    }

    @FXML
    private void onLoadAction() {
        load();
    }

    @FXML
    private void onExitAction() {
        stage.close();
    }

    @FXML
    public void onStartButtonClick() {
        start();
    }

    @FXML
    public void onStopButtonClick() {
        stop();
    }

    @FXML
    private void onDataBlockAddButtonClick() {
        addDataBlock();
    }

    @FXML
    private void onDataBlockUpdateButtonClick() {
        updateDataBlock();
    }

    @FXML
    private void onDataBlockDeleteButtonClick() {
        deleteDataBlock();
    }

    @FXML
    private void onDataBlockClearButtonClick() {
        clearDataBlockFields();
    }

    @FXML
    private void onTagAddButtonClick() {
        addTag();
    }

    @FXML
    private void onTagUpdateButtonClick() {
        updateTag();
    }

    @FXML
    private void onTagDeleteButtonClick() {
        deleteTag();
    }

    @FXML
    private void onTagClearButtonClick() {
        clearTagFields();
    }

    @FXML
    private void onExportButtonClick() {
        exportLogs();
    }

    @FXML
    private void onMenuItemFunctionInputsClick() {
        coilDiscreteInputFunction();
    }

    @FXML
    private void onMenuItemFunctionRegistersClick() {
        holdingInputRegistersFunction();
    }

    @FXML
    private void onMenuItemFunctionMaskWriteClick() {
        maskWriteRegisterFunction();
    }

    @FXML
    private void onMenuItemFunctionReadWriteClick() {
        readWriteRegistersFunction();
    }

    @FXML
    private void onMenuItemAboutCLick() {
        AboutController aboutController = new AboutController(hostServices);
        aboutController.showStage();
    }

    @FXML
    private void onMenuItemModbusManualClick() {
        hostServices.showDocument("https://modbus.org/tech.php");
    }
    @FXML
    private void onMenuItemDocumentationClick(){
        hostServices.showDocument("https://inscada.gitbook.io/ins/inscada-version-2022/yardimci-araclar/modbus-tcp-udp-client");
    }

    @FXML
    private void onResetCountersClick() {
        Statistics.clear();
    }

    @FXML
    private void onClearLogsClick() {
        console.clear();
    }
}
