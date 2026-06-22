package com.taskmanager.view;

import com.taskmanager.util.AnimationUtil;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Toast notification hiện ở góc phải màn hình
 * Tự biến mất sau 3 giây
 */
public class ToastNotification {

    private final String message;
    private final String type; // success | error | warning | info

    public ToastNotification(String message, String type) {
        this.message = message;
        this.type = type;
    }

    public void show(Stage owner) {
        Stage toast = new Stage();
        toast.initOwner(owner);
        toast.initStyle(StageStyle.TRANSPARENT);
        toast.setAlwaysOnTop(true);
        toast.initModality(Modality.NONE);

        // Nội dung toast
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.getStyleClass().addAll("toast", "toast-" + type);
        box.setMaxWidth(380);

        // Icon theo type
        FontAwesomeSolid iconType = switch (type) {
            case "success" -> FontAwesomeSolid.CHECK_CIRCLE;
            case "error"   -> FontAwesomeSolid.TIMES_CIRCLE;
            case "warning" -> FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            default        -> FontAwesomeSolid.INFO_CIRCLE;
        };

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(16);
        icon.getStyleClass().add("toast-icon");

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("toast-message");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(320);

        box.getChildren().addAll(icon, msgLabel);

        StackPane root = new StackPane(box);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        toast.setScene(scene);

        // Vị trí: góc dưới phải màn hình
        toast.show();
        positionToast(toast, owner);

        // Fade in
        box.setOpacity(0);
        box.setTranslateX(30);

        Timeline showAnim = new Timeline(
            new KeyFrame(Duration.millis(250),
                new KeyValue(box.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(box.translateXProperty(), 0, Interpolator.EASE_OUT))
        );

        // Tự đóng sau 3 giây
        Timeline hideAnim = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(box.opacityProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(box.translateXProperty(), 30, Interpolator.EASE_IN))
        );
        hideAnim.setDelay(Duration.seconds(2.8));
        hideAnim.setOnFinished(e -> toast.close());

        showAnim.setOnFinished(e -> hideAnim.play());
        showAnim.play();

        // Click để đóng sớm
        box.setOnMouseClicked(e -> {
            showAnim.stop();
            hideAnim.stop();
            toast.close();
        });
    }

    private void positionToast(Stage toast, Stage owner) {
        double ownerX = owner.getX();
        double ownerY = owner.getY();
        double ownerWidth = owner.getWidth();
        double ownerHeight = owner.getHeight();

        toast.setX(ownerX + ownerWidth - toast.getWidth() - 24);
        toast.setY(ownerY + ownerHeight - toast.getHeight() - 24);
    }
}
