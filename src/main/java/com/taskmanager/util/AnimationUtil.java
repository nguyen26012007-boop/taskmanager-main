package com.taskmanager.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Tiện ích tạo animation cho UI elements
 */
public class AnimationUtil {

    /**
     * Hiệu ứng fade-in khi load node
     */
    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.play();
    }

    /**
     * Hiệu ứng slide-in từ dưới lên
     */
    public static void slideInFromBottom(Node node, double durationMs) {
        node.setTranslateY(30);
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(30);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        new ParallelTransition(tt, ft).play();
    }

    /**
     * Hiệu ứng scale bounce khi nhấn button
     */
    public static void pulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.95);
        st.setToY(0.95);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    /**
     * Hiệu ứng glow khi hover vào card
     */
    public static void addHoverEffect(Node node, String glowColor) {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(glowColor, 0.3));
        shadow.setRadius(0);

        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();

            Timeline glow = new Timeline(
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(shadow.radiusProperty(), 15, Interpolator.EASE_OUT)));
            node.setEffect(shadow);
            glow.play();
        });

        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            Timeline unglow = new Timeline(
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(shadow.radiusProperty(), 0, Interpolator.EASE_IN)));
            unglow.setOnFinished(ev -> node.setEffect(null));
            unglow.play();
        });
    }

    /**
     * Animation cho số đếm (counter)
     */
    public static void animateNumber(javafx.scene.control.Label label, int from, int to, double durationMs) {
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(durationMs), e -> label.setText(String.valueOf(to)),
                new KeyValue(new javafx.beans.property.SimpleIntegerProperty(from) {
                    @Override
                    protected void invalidated() {
                        label.setText(String.valueOf(get()));
                    }
                }, to, Interpolator.EASE_BOTH));
        timeline.getKeyFrames().add(keyFrame);

        // Simplified implementation
        double step = durationMs / Math.abs(to - from + 1);
        Timeline tl = new Timeline();
        for (int i = 0; i <= Math.abs(to - from); i++) {
            final int val = from + (to >= from ? i : -i);
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * step),
                    e -> label.setText(String.valueOf(val))));
        }
        tl.play();
    }

    /**
     * Shake animation khi có lỗi
     */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /**
     * Hiệu ứng stagger cho danh sách items
     */
    public static void staggerFadeIn(java.util.List<Node> nodes, double delayBetween) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            node.setOpacity(0);
            PauseTransition pause = new PauseTransition(Duration.millis(i * delayBetween));
            pause.setOnFinished(e -> fadeIn(node, 300));
            pause.play();
        }
    }
}
