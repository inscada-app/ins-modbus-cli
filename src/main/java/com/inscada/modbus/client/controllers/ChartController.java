package com.inscada.modbus.client.controllers;

import com.inscada.modbus.client.model.Tag;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.fx.interaction.TooltipHandlerFX;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartController {
    private static final NumberStringConverter CONVERTER = new NumberStringConverter(Locale.US);
    private final Stage chartStage;
    private final List<Tag> tagList;
    private DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final StandardXYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator() {
        @Override
        public String generateToolTip(XYDataset dataset, int series, int item) {
            Instant instant = Instant.ofEpochMilli(dataset.getX(series, item).longValue());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return "Date: " + DATE_FORMATTER.format(localDateTime)
                    + "\n"
                    + "Value: " + dataset.getYValue(series, item);
        }
    };

    public ChartController(List<Tag> tagList) {
        this.chartStage = new Stage();
        this.tagList = tagList;
        loadStage();
    }
    private void loadStage() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (Tag tag : tagList) {
            TimeSeries series = new TimeSeries(tag.getName());
            dataset.addSeries(series);
            tag.valueProperty().addListener((observableValue, oldVal, newVal) -> {
                Date time = Date.from(newVal.getDate().atZone(ZoneId.systemDefault()).toInstant());
                Number val;
                if (newVal.getVal() instanceof Boolean) {
                    val = (Boolean) newVal.getVal() ? 1 : 0;
                } else {
                    val = CONVERTER.fromString(newVal.getVal().toString());
                }
                Platform.runLater(() -> {
                    if (series.getItemCount() > 1000) {
                        series.delete(0, 0);
                    }
                    series.add(new TimeSeriesDataItem(new Millisecond(time), val));
                });
            });
        }

        DateAxis xAxis = new DateAxis();
        NumberAxis yAxis = new NumberAxis();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setDefaultToolTipGenerator(toolTipGenerator);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        JFreeChart chart = new JFreeChart("Tag Chart", plot);
        ChartViewer chartViewer = new ChartViewer(chart);
        addFasterTooltip(chartViewer);
        chartViewer.addChartMouseListener(new ChartMouseListenerFX() {
            @Override
            public void chartMouseClicked(ChartMouseEventFX eventFX) {
                ChartEntity ce = eventFX.getEntity();
                if (ce instanceof LegendItemEntity) {
                    LegendItemEntity legendItem = (LegendItemEntity) ce;
                    int index = dataset.getSeriesIndex(legendItem.getSeriesKey());
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        if (i != index) {
                            renderer.setSeriesVisible(i, false);
                        }
                    }
                } else {
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        renderer.setSeriesVisible(i, true);
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEventFX eventFX) {
                ChartEntity ce = eventFX.getEntity();
                if (ce instanceof XYItemEntity) {
                    XYItemEntity item = (XYItemEntity) ce;
                    item.setToolTipText(toolTipGenerator.generateToolTip(dataset, item.getSeriesIndex(), item.getItem()));
                }
            }
        });

        Scene scene = new Scene(chartViewer, 900, 500);
        chartStage.setScene(scene);
        chartStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
        chartStage.setResizable(true);
        chartStage.setTitle("Chart");
    }

    public void showStage() {
        chartStage.show();
    }

    private void addFasterTooltip(ChartViewer chartViewer) {
        if (chartViewer.getCanvas().getMouseHandler("tooltip") != null) {
            chartViewer.getCanvas().removeAuxiliaryMouseHandler(chartViewer.getCanvas().getMouseHandler("tooltip"));
        }
        chartViewer.getCanvas().addAuxiliaryMouseHandler(new TooltipHandlerFX("tooltip") {
            Tooltip tooltip;
            boolean isVisible = false;

            @Override
            public void handleMouseMoved(ChartCanvas canvas, MouseEvent e) {
                if (!canvas.isTooltipEnabled()) {
                    return;
                }
                String text = getTooltipText(canvas, e.getX(), e.getY());
                setTooltip(canvas, text, e.getScreenX(), e.getScreenY());
            }

            private String getTooltipText(ChartCanvas canvas, double x, double y) {
                ChartRenderingInfo info = canvas.getRenderingInfo();
                if (info == null) {
                    return null;
                }
                EntityCollection entities = info.getEntityCollection();
                if (entities == null) {
                    return null;
                }
                ChartEntity entity = entities.getEntity(x, y);
                if (entity == null) {
                    return null;
                }
                return entity.getToolTipText();
            }

            // This function is copied from Canvas.setTooltip and manipulated as needed.
            public void setTooltip(ChartCanvas canvas, String text, double x, double y) {
                if (text != null) {
                    if (this.tooltip == null) {
                        this.tooltip = new Tooltip(text);
                        Tooltip.install(canvas, this.tooltip);
                    } else {
                        this.tooltip.setText(text);
                        this.tooltip.setAnchorX(x);
                        this.tooltip.setAnchorY(y);
                    }
                    this.tooltip.show(canvas, x, y);
                    isVisible = true;
                } else {
                    if (isVisible) {
                        this.tooltip.hide();
                        isVisible = false;
                    }
                }
            }
        });
    }
}




