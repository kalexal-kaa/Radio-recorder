/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package radioplayer;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.*;
/**
 *
 * @author alex
 */
public class PlayerController implements Initializable {
    
    public ListView stationsListView;
    public Button playButton;
    public Button stopButton;
    public MenuItem recordItem;
    public MenuItem stopRecordItem;
    public Label nameStation;
    public Player player;
    public Task taskPlayer,taskRecord;
    public Toast toast;
    
    private OutputStream output;
    private URL radioUrl;
    private String parentPath;
    private String path;
    private final String separator;
    
    public PlayerController() {
        this.separator = System.getProperty("file.separator");
        this.toast = new Toast();
    }

    public void playAction() {
        String l= (String) stationsListView.getSelectionModel().getSelectedItem();
        if(l==null){
            toast.setMessage("Выберите станцию");
            return;
        }
        
        String urlString = reader(path+separator+l);

        taskPlayer = new Task() {
            @Override
            public Void call() {
                try {
                    radioUrl = new URL(urlString);
                    InputStream in = radioUrl.openStream();
                    InputStream is = new BufferedInputStream(in);
                    player = new Player(is);
                    player.play();
                } catch (FileNotFoundException e) {
                    e.getMessage();
                } catch (IOException | JavaLayerException e) {
                    e.getMessage();
                }
                return null;
            }
        };
        new Thread(taskPlayer).start();
        playButton.setDisable(true);
        stopButton.setDisable(false);
        recordItem.setDisable(false);
        stopRecordItem.setDisable(true);
        nameStation.setText(l);
    }

    public void stopAction() throws IOException {
        if(!stopRecordItem.isDisable()){
           stopRecordAction();
        }
        playButton.setDisable(false);
        stopButton.setDisable(true);
        recordItem.setDisable(true);
        nameStation.setText("");
        player.close();
        taskPlayer.cancel();
    }
   public void recordAction(){
       File file = new File(parentPath+separator+"recordspath");
        if(!new File(reader(file.getAbsolutePath())).exists()){
            File f = new File(parentPath+separator+"Recordings");
            if(!f.exists()){
                f.mkdir();
            }
            if(!f.exists()){
                alertWindow("Укажите директорию для сохранения записей");
                return;
            }
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500,200);
        alert.setTitle("Сохранение Записей");
        alert.setHeaderText("");
        alert.setContentText("Путь по умолчанию для ваших записей:\n"+f.getAbsolutePath()+"\nИзменить?");
        
        ButtonType buttonTypeEdit = new ButtonType("Изменить", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeDefault = new ButtonType("По Умолчанию", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(buttonTypeEdit, buttonTypeDefault);
        
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if(resultAlert.get()==buttonTypeEdit){
            startDirectoryChooser(false);
        }else{
            writer(file.getAbsolutePath(), f.getAbsolutePath());
        }
       }
       taskRecord=new Task() {
            @Override
            public Void call() throws FileNotFoundException, IOException{
                    output = new FileOutputStream(reader(file.getAbsolutePath())+
                            separator+nameStation.getText()+"-"+new Date().toString().replace(":","-")+".mp3");
                    InputStream in = radioUrl.openStream();
                    InputStream is = new BufferedInputStream(in);
                    byte data[] = new byte[1024];
                    int count;
                    while ((count = is.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                output.flush();
                return null;
            }
        };
        new Thread(taskRecord).start();
        toast.setMessage("запись станции");
        recordItem.setDisable(true);
        stopRecordItem.setDisable(false);
   }
   public void stopRecordAction() throws IOException{
       output.close();
       taskRecord.cancel();
       toast.setMessage("остановка записи");
       recordItem.setDisable(false);
       stopRecordItem.setDisable(true);
   }
   public void directoryRecordAction(){
       File file = new File(parentPath+separator+"recordspath");
       if(new File(reader(file.getAbsolutePath())).exists()){
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500,200);
        alert.setTitle("Сохранение Записей");
        alert.setHeaderText("Уже существует путь сохранения записей");
        alert.setContentText("Хотите изменить путь <" + reader(file.getAbsolutePath()) + ">?");
        
        ButtonType buttonTypeEdit = new ButtonType("Изменить", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(buttonTypeEdit, buttonTypeCancel);
        
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if(resultAlert.get()==buttonTypeEdit){
            startDirectoryChooser(true);
        }
        }else{
            startDirectoryChooser(true);
        }
   }
   @SuppressWarnings("unchecked")
    public void addAction() {
        Dialog dialog = new Dialog<>();
        dialog.setTitle("Создание Станции");
        dialog.setHeaderText("Введите название и url радиостанции");

        ButtonType createButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType,cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField stationName = new TextField();
        TextField url = new TextField();

        grid.add(new Label("Название:"), 0, 0);
        grid.add(stationName, 1, 0);
        grid.add(new Label("Url:"), 0, 1);
        grid.add(url, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.get()==createButtonType){
            if (isEmpty(stationName.getText())){
                alertWindow("Станция не будет создана:\nне указано название станции");
                return;
            }
            if (isEmpty(url.getText())){
                alertWindow("Станция не будет создана:\nне указан url станции");
                return;
            }
            if (incorrectSymbols(stationName.getText())){
                alertWindow("Станция не будет создана:\nОбнаружены недопустимые символы в названии станции");
                return;
            }
            File file = new File(path+separator+stationName.getText().trim());
            if (file.exists()) {
                alertWindow("Станция не будет создана:\nСтанция с таким названием уже существует.");
                return;
            }
                writer(file.getAbsolutePath(), url.getText());
                showStationsList();
                stationsListView.getSelectionModel().select(file.getName());
        }
    }

    public void deleteAction() {
        String l= (String) stationsListView.getSelectionModel().getSelectedItem();
        if (l==null){
            toast.setMessage("Выберите станцию");
            return;
        }
        File file=new File(path+separator+l);
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтвердить");
        alert.setHeaderText("Удаление станции");
        alert.setContentText("Удалить станцию " + file.getName() + "?");
        
        ButtonType buttonTypeDelete = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(buttonTypeDelete, buttonTypeCancel);
        
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if (resultAlert.get()==buttonTypeDelete){
            File f=new File(path+separator+stationsListView.getSelectionModel().getSelectedItem());
            if(f.delete()){
                showStationsList();
            }else{
                toast.setMessage("Ошибка удаления");
            }
        }
    }

   @SuppressWarnings("unchecked")
    public void editAction() {
        String l= (String) stationsListView.getSelectionModel().getSelectedItem();
        if(l==null){
            toast.setMessage("Выберите станцию");
            return;
        }
        Dialog dialog = new Dialog<>();
        dialog.setTitle("Изменение Станции");
        dialog.setHeaderText("Измените название и(или) url радиостанции");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField stationName = new TextField();
        TextField url = new TextField();

        grid.add(new Label("Название:"), 0, 0);
        grid.add(stationName, 1, 0);
        grid.add(new Label("Url:"), 0, 1);
        grid.add(url, 1, 1);

        stationName.setText(l);
        url.setText(reader(path+separator+l));

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.get()==saveButtonType){
            if (isEmpty(stationName.toString())){
                alertWindow("Станция не будет изменена:\nне указано название станции");
                return;
            }
            if (isEmpty(url.toString())){
                alertWindow("Станция не будет изменена:\nне указан url станции");
                return;
            }
            if (incorrectSymbols(stationName.getText())){
                alertWindow("Станция не будет изменена:\nОбнаружены недопустимые символы в названии станции");
                return;
            }
            
            File selectFile = new File(path+separator+l);
            File editFile = new File(path+separator+stationName.getText().trim());

            if (!selectFile.getName().equals(editFile.getName())&&
                    !stationsListView.getItems().contains(editFile.getName())){
                if(!selectFile.renameTo(editFile)){
                    toast.setMessage("Ошибка переименования!");
                    return;
                }
                writer(editFile.getAbsolutePath(), url.getText());
            }else{
                if (!selectFile.getName().equals(editFile.getName())) {
                    alertWindow("Станция не будет изменена:\nСтанция с таким названием уже существует.");
                    return;
                }
                writer(editFile.getAbsolutePath(), url.getText());
            }
            showStationsList();
            stationsListView.getSelectionModel().select(editFile.getName());
        }
    }
    public void exitAction(){
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500,200);
        alert.setTitle("ВЫХОД");
        alert.setHeaderText("Выйти из программы?");
        alert.setContentText("Перед выходом из программы остановите воспроизведение и запись."
                + " Сверните окно, если прослушивание продолжится и окно программы мешает работе.");
        
        ButtonType buttonTypeExit = new ButtonType("Выйти", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeExit, buttonTypeCancel);
        
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if (resultAlert.get() == buttonTypeExit) {
            System.exit(0);
        }
    }
    public void appInfoAction(){
        alertWindow("Название: Радио\nВерсия: 1.0\nРазработчик: Developer Kalexal(KAA)");
    }
    private void startDirectoryChooser(boolean b){
        final DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Выбор директории для записей");
        dc.setInitialDirectory(null);
        File file = dc.showDialog(null);
        if(file!=null){
            writer(parentPath+separator+"recordspath", file.getAbsolutePath());
            if(b){
               toast.setMessage("выбрана директория <"+file.getName()+">");
            }
        }
    }
    private boolean incorrectSymbols(String str){
        return str.contains(":")||str.contains("*")||str.contains("/")||str.contains("|") ||str.contains("#")||str.contains(";");
    }
    private boolean isEmpty(String p){
        return p.trim().length()==0;
    }
    private void dirCreator(final String fPath) {
        final File file = new File(fPath);
        if (!file.exists()) {
            file.mkdir();
            if(file.exists()){
                alertWindow("Создан каталог <Stations>.\nВаши радиостанции будут здесь:\n"+fPath);
            }else{
                alertWindow("Ошибка!\nКаталог <Stations> не будет создан.\n" +
                        "Попробуйте создать указанный каталог вручную по следующему пути:\n"+fPath+"\nПрограмма будет закрыта.");
                System.exit(0);
            }
        }
    }
   @SuppressWarnings("unchecked")
    private void showStationsList(){
        File files=new File(path);
        File[] f=files.listFiles();
        assert f != null;
        String[] stations=new String[f.length];
        for(int i=0;i<stations.length;i++){
            stations[i]=f[i].getName();
        }
        stationsListView.setItems(FXCollections.observableArrayList(stations).sorted());
    }
    private void alertWindow(final String s) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(400, 140);
        alert.setTitle("Сообщение");
        alert.setHeaderText("");
        alert.setContentText(s);
        ButtonType buttonType = new ButtonType("Закрыть", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(buttonType);
        alert.showAndWait();
    }
    private boolean permissionRead(File file){
        if(!file.canRead()){
            file.setReadable(true);
            return !file.canRead();
        }
        return false;
    }
    private boolean permissionWrite(File file){
        if(!file.canWrite()){
            file.setWritable(true);
            return !file.canWrite();
        }
        return false;
    }
    private void writer(String pathFile,String text){
        try (final FileWriter fw = new FileWriter(pathFile)) {
            fw.write(text);
        } catch (IOException e) {
            e.getMessage();
        }
    }
    private String reader(final String s) {
        StringBuilder f=new StringBuilder();
        try {
            final File file = new File(s);
            final BufferedReader br;
            try (FileReader fr = new FileReader(file)) {
                br = new BufferedReader(fr);
                String str;
                while ((str = br.readLine()) != null) {
                    f.append(str);
                }
            }
            br.close();
        }
        catch (IOException e) {
            e.getMessage();
        }
        return f.toString();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        try {
            parentPath= URLDecoder.decode(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.getMessage();
        }
        path=parentPath+separator+"Stations";
        this.dirCreator(this.path);
        File f=new File(path);
        if(permissionRead(f)||permissionWrite(f)){
            if(permissionRead(f)&&permissionWrite(f)){
                alertWindow("Не удалось получить разрешение на чтение и запись файлов в каталог <Stations>.\nПопробуйте дать разрешение вручную.");
            }else if(permissionRead(f)){
                alertWindow("Не удалось получить разрешение на чтение файлов в каталоге <Stations>.\nПопробуйте дать разрешение вручную.");
            }else{
                alertWindow("Не удалось получить разрешение на запись файлов в каталог <Stations>.\nПопробуйте дать разрешение вручную.");
            }
            System.exit(0);
        }
        showStationsList();
        stopButton.setDisable(true);
        recordItem.setDisable(true);
        stopRecordItem.setDisable(true);
    }    
    
}
